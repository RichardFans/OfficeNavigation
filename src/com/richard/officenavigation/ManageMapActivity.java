package com.richard.officenavigation;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
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
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.INodeDao;
import com.richard.officenavigation.dao.IPath;
import com.richard.officenavigation.dao.IPathDao;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.dialog.InfoDialog;
import com.richard.officenavigation.view.MapTileView;
import com.richard.utils.Maths;

@SuppressLint("ClickableViewAccessibility")
public class ManageMapActivity extends BaseActivity implements
		onConfirmNodeAddListener, onConfirmNodeDelListener,
		onConfirmPathAddListener, onConfirmPathDelListener, OnTouchListener {
	private MapTileView mTileMap;
	private IMap mMap;

	private DaoSession mDaoSession;

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
		Long id = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
		mTileMap.setAdapter(new IMapAdapter(this, id));
		mTileMap.setupMapDefault(false);

		Paint paint = mTileMap.getPathPaint();
		paint.setShadowLayer(4, 2, 2, 0x66000000);
		paint.setPathEffect(new CornerPathEffect(5));

		mCalloutAddNode.setOnConfirmNodeAddListener(this);
		mCalloutDelNode.setOnConfirmNodeDelListener(this);
		mCalloutAddPath.setOnConfirmPathAddListener(this);
		mCalloutDelPath.setOnConfirmPathDelListener(this);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		mMap = ((IMapAdapter) mTileMap.getAdapter()).getIMap();
		mDrawPaths = new LongSparseArray<LongSparseArray>();
		List<INode> nodes = mMap.getNodes();
		INode from, to;
		LongSparseArray<DrawablePath> lsaFromTo, lsaToFrom;

		if (!nodes.isEmpty()) {
			for (INode node : nodes) {
				ImageView marker = new ImageView(this);
				marker.setImageResource(R.drawable.map_node_icon);
				marker.setTag(node);
				mTileMap.addMarker(marker, node.getX() / mMap.getScale(),
						node.getY() / mMap.getScale(), -0.5f, -1.0f);
				List<IPath> paths = node.getAdjacencies();
				from = node;
				for (IPath path : paths) {
					to = path.getTarget();
					// 画出并记录所画路径
					DrawablePath drawPath = null;
					boolean havePath = false;
					lsaFromTo = mDrawPaths.get(from.getId());
					if (lsaFromTo == null) {
						lsaFromTo = new LongSparseArray<DrawablePath>();
						mDrawPaths.put(from.getId(), lsaFromTo);
						drawPath = drawDrawablePath(from, to);
						lsaFromTo.put(to.getId(), drawPath);
					} else {
						if (lsaFromTo.get(to.getId()) == null) {
							drawPath = drawDrawablePath(from, to);
							lsaFromTo.put(to.getId(), drawPath);
						} else {
							havePath = true;
						}
					}
					if (!havePath) {
						lsaToFrom = mDrawPaths.get(to.getId());
						if (lsaToFrom == null) {
							lsaToFrom = new LongSparseArray<DrawablePath>();
							mDrawPaths.put(to.getId(), lsaToFrom);
						}
						lsaToFrom.put(from.getId(), drawPath);
					}
				}
			}
		}
		mTileMap.addMarkerEventListener(mDelNodeMarkerListener);
	}

	private DrawablePath drawDrawablePath(INode from, INode to) {
		List<double[]> positions;
		positions = new ArrayList<>();
		positions.add(new double[] { from.getX() / mMap.getScale(),
				from.getY() / mMap.getScale() });
		positions.add(new double[] { to.getX() / mMap.getScale(),
				to.getY() / mMap.getScale() });
		return mTileMap.drawPath(positions);
	}

	private void handleActionAddNode() {
		getActionBar().setTitle(R.string.action_add_node);
		mTileMap.setOnTouchListener(this);
	}

	private void handleActionDelNode() {
		getActionBar().setTitle(R.string.action_del_node);
		mTileMap.addMarkerEventListener(mDelNodeMarkerListener);
	}

	private void handleActionAddPath() {
		getActionBar().setTitle(R.string.action_add_path);
		mTileMap.addMarkerEventListener(mAddPathMarkerListener);
		mCalloutAddPath.setPathFromStage(true);
	}

	private void handleActionDelPath() {
		getActionBar().setTitle(R.string.action_del_path);
		mTileMap.addMarkerEventListener(mDelPathMarkerListener);
		mCalloutDelPath.setPathFromStage(true);
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
						mDaoSession.clear();
						mDaoSession = null;
						finish();
					}
				}).show();
	}

	private void removeAllMarkerListenersAndCallouts() {
		if (mPrevItem == null) {
			mTileMap.removeMarkerEventListener(mDelNodeMarkerListener);
			mTileMap.removeCallout(mCalloutDelNode);
			return;
		}
		switch (mPrevItem.getItemId()) {
		case R.id.action_add_node:
			mTileMap.setOnTouchListener(null);
			mTileMap.removeCallout(mCalloutAddNode);
			break;
		case R.id.action_del_node:
			mTileMap.removeMarkerEventListener(mDelNodeMarkerListener);
			mTileMap.removeCallout(mCalloutDelNode);
			break;
		case R.id.action_add_path:
			mTileMap.removeMarkerEventListener(mAddPathMarkerListener);
			mTileMap.removeCallout(mCalloutAddPath);
			mCalloutAddPath.setPathFromStage(true);
			break;
		case R.id.action_del_path:
			mTileMap.removeMarkerEventListener(mDelPathMarkerListener);
			mTileMap.removeCallout(mCalloutDelPath);
			mCalloutDelPath.setPathFromStage(true);
			break;
		}
	}

	private MenuItem mPrevItem;

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() != R.id.menu_mangage_map) {
			item.setChecked(true);
			removeAllMarkerListenersAndCallouts();
			mPrevItem = item;
			switch (item.getItemId()) {
			case R.id.action_add_node:
				handleActionAddNode();
				break;
			case R.id.action_del_node:
				handleActionDelNode();
				break;
			case R.id.action_add_path:
				handleActionAddPath();
				break;
			case R.id.action_del_path:
				handleActionDelPath();
				break;
			case R.id.action_done:
				handleActionDone();
				break;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.manage_map, menu);
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

		long id = nodeDao.insert(node);
		d("加入节点: " + name + "(" + id + "), x, y = " + x + ", " + y);		
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
