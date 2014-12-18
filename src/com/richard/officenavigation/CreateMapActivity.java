package com.richard.officenavigation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.qozix.tileview.markers.MarkerEventListener;
import com.qozix.tileview.paths.DrawablePath;
import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.callout.AddNodeCallout;
import com.richard.officenavigation.callout.AddNodeCallout.onConfirmNodeAddListener;
import com.richard.officenavigation.callout.AddPathCallout;
import com.richard.officenavigation.callout.AddPathCallout.onConfirmPathAddListener;
import com.richard.officenavigation.callout.DelNodeCallout;
import com.richard.officenavigation.callout.DelNodeCallout.onConfirmNodeDelListener;
import com.richard.officenavigation.callout.DelPathCallout;
import com.richard.officenavigation.callout.DelPathCallout.onConfirmPathDelListener;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.IMapDao;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.INodeDao;
import com.richard.officenavigation.dao.IPath;
import com.richard.officenavigation.dao.IPathDao;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.dialog.InfoDialog;
import com.richard.officenavigation.view.MapTileView;
import com.richard.utils.Maths;

/**
 * 该代码需要进一步优化：
 * 1、代码结构需要优化-->修改地图和新建地图可以做成一个activity
 * 2、数据库访问需要优化-->修改地图和新建地图时，对节点和路径的添加和删除应该在最后提交时才写入数据库
 * 3、编辑地图的交互需要优化-->增加撤销和重做的功能
 * @author Administrator
 *
 */
@SuppressLint("ClickableViewAccessibility")
public class CreateMapActivity extends BaseActivity implements
		onConfirmNodeAddListener, onConfirmNodeDelListener,
		onConfirmPathAddListener, onConfirmPathDelListener, OnTouchListener {
	private MapTileView mTileMap;
	private IMap mMap;

	private DaoSession mDaoSession;

	private MenuItem mItemAddNote, mItemAddPath, mItemPrevStep, mItemNextStep,
			mItemDone;

	private AddNodeCallout mCalloutAddNode;
	private DelNodeCallout mCalloutDelNode;
	private AddPathCallout mCalloutAddPath;
	private DelPathCallout mCalloutDelPath;

	/**
	 * fromId toId
	 */
	@SuppressWarnings("rawtypes")
	private LongSparseArray<LongSparseArray> mDrawPaths;

	@Override
	protected void findViews() {
		mTileMap = new MapTileView(this);
		setContentView(mTileMap);
		mCalloutAddNode = new AddNodeCallout(this, mTileMap);
		mCalloutDelNode = new DelNodeCallout(this, mTileMap);
		mCalloutAddPath = new AddPathCallout(this, mTileMap);
		mCalloutDelPath = new DelPathCallout(this, mTileMap);
	}

	@Override
	protected void setupViews() {
		Bundle e = getIntent().getExtras();
		IMap map = new IMap();
		String path = e.getString(C.map.EXTRA_SELECTED_MAP_PATH);
		int lastSp = path.lastIndexOf(File.separator);
		String name = path.substring(lastSp + 1, path.length());
		map.setName(name);
		map.setSrc(path);
		map.setScale(e.getDouble(C.map.EXTRA_MAP_MM_PX_SCALE));
		map.setWidth(e.getLong(C.map.EXTRA_MAP_PX_WIDTH));
		map.setHeight(e.getLong(C.map.EXTRA_MAP_PX_HEIGHT));
		mTileMap.setAdapter(new IMapAdapter(this, map));
		mTileMap.setupMapDefault(false);

		Paint paint = mTileMap.getPathPaint();
		paint.setShadowLayer(4, 2, 2, 0x66000000);
		paint.setPathEffect(new CornerPathEffect(5));

		mCalloutAddNode.setOnConfirmNodeAddListener(this);
		mCalloutDelNode.setOnConfirmNodeDelListener(this);
		mCalloutAddPath.setOnConfirmPathAddListener(this);
		mCalloutDelPath.setOnConfirmPathDelListener(this);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		mMap = ((IMapAdapter) mTileMap.getAdapter()).getIMap();
		IMapDao mapDao = mDaoSession.getIMapDao();
		List<IMap> maps = mapDao.queryBuilder()
				.where(IMapDao.Properties.Src.eq(mMap.getSrc())).list();
		if (maps.isEmpty()) {
			mMap.setId(null);
			mapDao.insert(mMap);
		} else {
			mMap.setId(maps.get(0).getId());
			mapDao.update(mMap);
		}
		// 如果原地图有关联的路径，那么将它们删除
		IPathDao pathDao = mDaoSession.getIPathDao();
		List<IPath> paths = pathDao.queryBuilder()
				.where(IPathDao.Properties.MapId.eq(mMap.getId())).list();
		if (!paths.isEmpty()) {
			pathDao.queryBuilder()
					.where(IPathDao.Properties.MapId.eq(mMap.getId()))
					.buildDelete().executeDeleteWithoutDetachingEntities();
		}
		// 如果原地图有关联的节点，那么将它们删除
		INodeDao nodeDao = mDaoSession.getINodeDao();
		List<INode> nodes = nodeDao.queryBuilder()
				.where(INodeDao.Properties.MapId.eq(mMap.getId())).list();
		if (!nodes.isEmpty()) {
			nodeDao.queryBuilder()
					.where(INodeDao.Properties.MapId.eq(mMap.getId()))
					.buildDelete().executeDeleteWithoutDetachingEntities();
		}
		mDrawPaths = new LongSparseArray<LongSparseArray>();
	}

	private void handleActionAddNode() {
		MenuItem item = mItemAddNote;
		item.setChecked(!item.isChecked());

		Drawable icon = getResources().getDrawable(
				R.drawable.action_manage_node);
		int[] state = { item.isChecked() ? android.R.attr.state_checked
				: android.R.attr.state_empty };
		icon.setState(state);
		item.setIcon(icon.getCurrent());

		if (item.isChecked()) {
			getActionBar().setTitle(R.string.action_add_node);
			mTileMap.setOnTouchListener(this);
			mTileMap.removeMarkerEventListener(mDelNodeMarkerListener);
			mTileMap.removeCallout(mCalloutDelNode);
		} else {
			getActionBar().setTitle(R.string.action_del_node);
			mTileMap.setOnTouchListener(null);
			mTileMap.addMarkerEventListener(mDelNodeMarkerListener);
			mTileMap.removeCallout(mCalloutAddNode);
		}
	}

	private void handleActionAddPath() {
		MenuItem item = mItemAddPath;
		item.setChecked(!item.isChecked());

		Drawable icon = getResources().getDrawable(
				R.drawable.action_manage_path);
		int[] state = { item.isChecked() ? android.R.attr.state_checked
				: android.R.attr.state_empty };
		icon.setState(state);
		item.setIcon(icon.getCurrent());

		if (item.isChecked()) {
			mTileMap.removeMarkerEventListener(mDelPathMarkerListener);
			mTileMap.removeCallout(mCalloutDelPath);
			getActionBar().setTitle(R.string.action_add_path);
			mTileMap.addMarkerEventListener(mAddPathMarkerListener);
			mCalloutAddPath.setPathFromStage(true);
		} else {
			mTileMap.removeMarkerEventListener(mAddPathMarkerListener);
			mTileMap.removeCallout(mCalloutDelPath);
			getActionBar().setTitle(R.string.action_del_path);
			mTileMap.addMarkerEventListener(mDelPathMarkerListener);
			mCalloutDelPath.setPathFromStage(true);
		}
	}

	private void handleActionPrevStep() {
		if (mItemAddPath.isChecked()) {
			mTileMap.removeMarkerEventListener(mAddPathMarkerListener);
			mTileMap.removeCallout(mCalloutAddPath);
		} else {
			mTileMap.removeMarkerEventListener(mDelPathMarkerListener);
			mTileMap.removeCallout(mCalloutDelPath);
		}

		if (mItemAddNote.isChecked()) {
			getActionBar().setTitle(R.string.action_add_node);
			mTileMap.setOnTouchListener(this);
		} else {
			getActionBar().setTitle(R.string.action_del_node);
			mTileMap.addMarkerEventListener(mDelNodeMarkerListener);
		}

		mItemPrevStep.setVisible(false);
		mItemNextStep.setVisible(true);
		mItemDone.setVisible(false);
		mItemAddNote.setVisible(true);
		mItemAddPath.setVisible(false);
	}

	private void handleActionNextStep() {
		if (mItemAddNote.isChecked()) {
			mTileMap.removeCallout(mCalloutAddNode);
			mTileMap.setOnTouchListener(null);
		} else {
			mTileMap.removeCallout(mCalloutDelNode);
			mTileMap.removeMarkerEventListener(mDelNodeMarkerListener);
		}

		if (mItemAddPath.isChecked()) {
			getActionBar().setTitle(R.string.action_add_path);
			mTileMap.addMarkerEventListener(mAddPathMarkerListener);
			mCalloutAddPath.setPathFromStage(true);
		} else {
			getActionBar().setTitle(R.string.action_del_path);
			mTileMap.addMarkerEventListener(mDelPathMarkerListener);
			mCalloutDelPath.setPathFromStage(true);
		}

		mItemPrevStep.setVisible(true);
		mItemNextStep.setVisible(false);
		mItemDone.setVisible(true);
		mItemAddNote.setVisible(false);
		mItemAddPath.setVisible(true);
	}

	private void handleActionDone() {
		InfoDialog.newInstance(this, "选择地图", "是否将该地图作为当前地图？",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							SharedPreferences.Editor edit = getSharedPreferences(
									C.PREFERENCES_MANAGE, MODE_PRIVATE).edit();
							edit.putLong(C.map.KEY_CURRENT_MAPID, mMap.getId());
							edit.commit();
							Intent i = new Intent();
							i.putExtra(C.map.KEY_CURRENT_MAPID, mMap.getId());
							setResult(RESULT_OK, i);
						} else {
							setResult(RESULT_CANCELED);
						}
						dialog.dismiss();
						finish();
					}
				}).show();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add_node:
			handleActionAddNode();
			break;
		case R.id.action_add_path:
			handleActionAddPath();
			break;
		case R.id.action_prev_step:
			handleActionPrevStep();
			break;
		case R.id.action_next_step:
			handleActionNextStep();
			break;
		case R.id.action_done:
			handleActionDone();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.create_map, menu);

		mItemAddNote = menu.findItem(R.id.action_add_node);
		mItemAddPath = menu.findItem(R.id.action_add_path);
		mItemPrevStep = menu.findItem(R.id.action_prev_step);
		mItemNextStep = menu.findItem(R.id.action_next_step);
		mItemDone = menu.findItem(R.id.action_done);
		mItemPrevStep.setVisible(false);
		mItemAddPath.setVisible(false);
		mItemDone.setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mTileMap != null)
			mTileMap.clear();

	}

	@Override
	public void onResume() {
		super.onResume();
		if (mTileMap != null)
			mTileMap.resume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTileMap != null) {
			mTileMap.destroy();
			mTileMap = null;
		}
		if (mDaoSession != null) {
			mDaoSession.clear();
			mDaoSession = null;
		}
	}

	@Override
	public void onConfirmNodeAdd(View callout, String name, double x, double y, boolean visible) {
		if (name.equals("")) {
			m("节点名称不能为空！");
			return;
		}
		// 保存节点到数据库
		INodeDao nodeDao = mDaoSession.getINodeDao();
		INode node = new INode();
		node.setName(name);
		node.setX((long) (x * mMap.getScale()));
		node.setY((long) (y * mMap.getScale()));
		node.setVisible(visible);
		node.setMapId(mMap.getId());
		List<INode> nodes = nodeDao
				.queryBuilder()
				.where(INodeDao.Properties.MapId.eq(mMap.getId()),
						INodeDao.Properties.X.eq(node.getX()),
						INodeDao.Properties.Y.eq(node.getY())).list();
		if (!nodes.isEmpty()) {
			m("该节点已存在！");
			return;
		}

		d("加入节点: " + name + ", x, y = " + x + ", " + y);
		nodeDao.insert(node);

		// 添加一个mark
		ImageView marker = new ImageView(this);
		marker.setImageResource(R.drawable.map_node_icon);
		marker.setTag(node);
		mTileMap.addMarker(marker, x, y, -0.5f, -1.0f);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void onConfirmNodeDel(View callout, INode node) {
		// 删除所画路径
		Long to, from = node.getId();
		DrawablePath path;
		LongSparseArray<DrawablePath> lsaToFrom, lsaFromTo = mDrawPaths
				.get(from);
		if (lsaFromTo != null) {
			for (int i = 0; i < lsaFromTo.size(); i++) {
				to = lsaFromTo.keyAt(i);
				path = lsaFromTo.get(to);
				lsaToFrom = mDrawPaths.get(to);
				lsaToFrom.remove(from);
			}
			for (int i = 0; i < lsaFromTo.size(); i++) {
				to = lsaFromTo.keyAt(i);
				path = lsaFromTo.get(to);
				mTileMap.removePath(path);
			}
		}

		// 删除数据库中的相关路径
		IPathDao pathDao = mDaoSession.getIPathDao();
		pathDao.queryBuilder()
				.where(IPathDao.Properties.MapId.eq(mMap.getId()))
				.whereOr(IPathDao.Properties.From.eq(node.getId()),
						IPathDao.Properties.To.eq(node.getId())).buildDelete()
				.executeDeleteWithoutDetachingEntities();

		// 删除所画节点
		mTileMap.removeMarker((View) callout.getTag());

		// 删除数据库中的节点
		INodeDao nodeDao = mDaoSession.getINodeDao();
		nodeDao.delete(node);
	}

	private MarkerEventListener mDelNodeMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			double scale = mTileMap.getScale();
			mCalloutDelNode.setNode((INode) view.getTag());
			mCalloutDelNode.setTag(view);
			mTileMap.addCallout(mCalloutDelNode, x / scale, y / scale);
		}
	};

	@Override
	public void onConfirmPathAdd(View callout, INode from, INode to) {
		double scale = mMap.getScale();

		// 画出路径
		List<double[]> positions = new ArrayList<>();
		positions
				.add(new double[] { from.getX() / scale, from.getY() / scale });
		positions.add(new double[] { to.getX() / scale, to.getY() / scale });
		DrawablePath drawPath = mTileMap.drawPath(positions);

		// 记录所画路径
		@SuppressWarnings("unchecked")
		LongSparseArray<DrawablePath> lsaFromTo = mDrawPaths.get(from.getId());
		@SuppressWarnings("unchecked")
		LongSparseArray<DrawablePath> lsaToFrom = mDrawPaths.get(to.getId());
		if (lsaFromTo == null) {
			lsaFromTo = new LongSparseArray<DrawablePath>();
			mDrawPaths.put(from.getId(), lsaFromTo);
		}
		if (lsaToFrom == null) {
			lsaToFrom = new LongSparseArray<DrawablePath>();
			mDrawPaths.put(to.getId(), lsaToFrom);
		}
		lsaFromTo.put(to.getId(), drawPath);
		lsaToFrom.put(from.getId(), drawPath);

		// 插入from到to和to到from两条路径到数据库
		IPathDao pathDao = mDaoSession.getIPathDao();
		IPath path = new IPath();
		path.setFrom(from.getId());
		path.setTo(to.getId());
		path.setDistance((long) Maths.distance(from.getX(), from.getY(),
				to.getX(), to.getY()));
		path.setMapId(mMap.getId());
		pathDao.insert(path);

		path.setId(null);
		path.setFrom(to.getId());
		path.setTo(from.getId());
		pathDao.insert(path);
	}

	private MarkerEventListener mAddPathMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			double scale = mTileMap.getScale();
			if (mCalloutAddPath.isPathFromStage()) {
				mCalloutAddPath.setFrom((INode) view.getTag());
				mTileMap.addCallout(mCalloutAddPath, x / scale, y / scale);
			} else {
				boolean newPath = false;
				INode to = (INode) view.getTag();
				INode from = mCalloutAddPath.getFrom();

				if (to != from) {
					List<IPath> paths = mDaoSession
							.getIPathDao()
							.queryBuilder()
							.where(IPathDao.Properties.MapId.eq(mMap.getId()),
									IPathDao.Properties.From.eq(from.getId()),
									IPathDao.Properties.To.eq(to.getId()))
							.list();
					if (paths.isEmpty())
						newPath = true;
				}
				if (newPath) {
					mCalloutAddPath.setTo((INode) view.getTag());
					mTileMap.addCallout(mCalloutAddPath, x / scale, y / scale);
				}
			}
		}
	};

	@SuppressWarnings("unchecked")
	@Override
	public void onConfirmPathDel(View callout, INode from, INode to) {
		// 删除所画路径
		// Long to, from = from.getId();
		DrawablePath path;
		LongSparseArray<DrawablePath> lsaFromTo = mDrawPaths.get(from.getId());
		LongSparseArray<DrawablePath> lsaToFrom = mDrawPaths.get(to.getId());
		path = lsaFromTo.get(to.getId());
		lsaFromTo.remove(to.getId());
		mTileMap.removePath(path);
		lsaToFrom.remove(from.getId());

		// 删除数据库中的相关路径
		IPathDao pathDao = mDaoSession.getIPathDao();
		pathDao.queryBuilder()
				.where(IPathDao.Properties.MapId.eq(mMap.getId()))
				.whereOr(
						pathDao.queryBuilder().and(
								IPathDao.Properties.From.eq(from.getId()),
								IPathDao.Properties.To.eq(to.getId())),
						pathDao.queryBuilder().and(
								IPathDao.Properties.From.eq(to.getId()),
								IPathDao.Properties.To.eq(from.getId())))
				.buildDelete().executeDeleteWithoutDetachingEntities();
	}

	private MarkerEventListener mDelPathMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			double scale = mTileMap.getScale();
			if (mCalloutDelPath.IsPathFromStage()) {
				INode node = (INode) view.getTag();
				List<IPath> paths = mDaoSession
						.getIPathDao()
						.queryBuilder()
						.where(IPathDao.Properties.MapId.eq(mMap.getId()))
						.whereOr(IPathDao.Properties.From.eq(node.getId()),
								IPathDao.Properties.To.eq(node.getId())).list();
				if (!paths.isEmpty()) {
					mCalloutDelPath.setFrom(node);
					mTileMap.addCallout(mCalloutDelPath, x / scale, y / scale);
				}
			} else {
				boolean havePath = false;
				INode to = (INode) view.getTag();
				INode from = mCalloutDelPath.getFrom();
				if (to != from) {
					List<IPath> paths = mDaoSession
							.getIPathDao()
							.queryBuilder()
							.where(IPathDao.Properties.MapId.eq(mMap.getId()),
									IPathDao.Properties.From.eq(from.getId()),
									IPathDao.Properties.To.eq(to.getId()))
							.list();
					if (!paths.isEmpty())
						havePath = true;
				}
				if (havePath) {
					mCalloutDelPath.setTo((INode) view.getTag());
					mTileMap.addCallout(mCalloutDelPath, x / scale, y / scale);
				}
			}
		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			double scale = mTileMap.getScale();
			double x = (event.getX() + mTileMap.getScrollX()) / scale;
			double y = (event.getY() + mTileMap.getScrollY()) / scale;
			mCalloutAddNode.clearName();
			mTileMap.addCallout(mCalloutAddNode, x, y);
			mCalloutAddNode.setPos(x, y);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			v.performClick();
		}
		return false;
	}
}
