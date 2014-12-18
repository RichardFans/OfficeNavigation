package com.richard.officenavigation.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PathEffect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.qozix.tileview.paths.DrawablePath;
import com.richard.officenavigation.CreateMapActivity;
import com.richard.officenavigation.ManageBeaconActivity;
import com.richard.officenavigation.ManageMapActivity;
import com.richard.officenavigation.R;
import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.adapter.BaseMapAdapter;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.callout.FindPathCallout;
import com.richard.officenavigation.callout.FindPathCallout.onConfirmPathListener;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dialog.DirectoryChooserDialog;
import com.richard.officenavigation.dialog.DirectoryChooserDialog.OnConfirmDirectoryChooseListener;
import com.richard.officenavigation.dialog.MapChooserDialog;
import com.richard.officenavigation.dialog.MapChooserDialog.onMapSelectedListener;
import com.richard.officenavigation.dialog.MapParamsSetterDialog;
import com.richard.officenavigation.dialog.MapParamsSetterDialog.onConfirmSettingListener;
import com.richard.officenavigation.findpath.Dijkstra;
import com.richard.officenavigation.view.DespoticTileView;
import com.richard.officenavigation.view.MapTileView.onNodeClickListener;
import com.richard.utils.Views;

public class MapFragment extends TabPagerFragment implements
		OnConfirmDirectoryChooseListener, onConfirmSettingListener,
		onMapSelectedListener, onNodeClickListener, onConfirmPathListener {
	private static final int REQ_CREATE_MAP = 1;
	private static final int REQ_MANAGE_MAP = 2;
	private DespoticTileView mTileMap;
	private DirectoryChooserDialog mDlgChooseMapDir;
	private MapParamsSetterDialog mDlgMapParamsSetter;
	private MapChooserDialog mDlgChooseMap;

	private ImageView mIvFrom;
	private ImageView mIvTo;
	private FindPathCallout mCalloutFindPath;
	private Dijkstra mAlgorithmFindPath;
	private DrawablePath mDrawPath, mDrawPathArrow;
	private Paint mPaintPath, mPaintArrow;

	private Bundle mExtraData;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		d("oncreateView");
		mTileMap = (DespoticTileView) inflater.inflate(R.layout.fragment_map,
				container, false);
		setupMap();
		createDialog();
		mExtraData = new Bundle();
		setHasOptionsMenu(true);
		return mTileMap;
	}

	private void setupMap() {
		SharedPreferences sp = getActivity().getSharedPreferences(
				C.PREFERENCES_MANAGE, Context.MODE_PRIVATE);
		Long mapId = sp.getLong(C.map.KEY_CURRENT_MAPID, C.map.DEFAULT_MAP_ID);
		BaseMapAdapter adapter = new IMapAdapter(getActivity(), mapId);
		mTileMap.setAdapter(adapter);
		mTileMap.setupMapDefault(true);
		mTileMap.setOnNodeClickListener(this);

		mIvFrom = new ImageView(getActivity());
		mIvFrom.setImageResource(R.drawable.find_path_start);
		mIvTo = new ImageView(getActivity());
		mIvTo.setImageResource(R.drawable.find_path_end);

		mCalloutFindPath = new FindPathCallout(getActivity(), mTileMap);
		mCalloutFindPath.setOnConfirmPathListener(this);

		List<INode> nodes = ((IMapAdapter) adapter).getINodes();
		mAlgorithmFindPath = new Dijkstra(nodes);

		mPaintPath = new Paint();
		mPaintPath.setColor(0xCC880000);
		mPaintPath.setStyle(Style.STROKE);
		mPaintPath.setStrokeWidth(dp(5));
		mPaintPath.setAntiAlias(true);
		mPaintPath.setStrokeCap(Cap.ROUND);
		mPaintPath.setShadowLayer(dp(3), dp(3), dp(3), 0x88000000);
		PathEffect effect = new DashPathEffect(new float[] { dp(1), dp(2),
				dp(4), dp(8) }, 1);
		mPaintPath.setPathEffect(effect);

		mPaintArrow = new Paint();
		mPaintArrow.setColor(0xCC880000);
		mPaintArrow.setStyle(Style.STROKE);
		mPaintArrow.setStrokeWidth(dp(5));
		mPaintArrow.setStrokeCap(Cap.ROUND);
		mPaintArrow.setAntiAlias(true);
		mPaintArrow.setShadowLayer(dp(3), dp(3), dp(3), 0x88000000);
		mPaintArrow.setPathEffect(new CornerPathEffect(5));
	}

	private void createDialog() {
		// 创建选择地图目录对话框
		mDlgChooseMapDir = DirectoryChooserDialog.newInstance(getActivity(),
				C.APP_FOLDER + File.separator + C.map.DIR, getResources()
						.getString(R.string.title_selected_map_folder), this);
		// 创建地图参数设置对话框
		mDlgMapParamsSetter = MapParamsSetterDialog.newInstance(getActivity(),
				this);
		// 创建选择地图对话框
		mDlgChooseMap = MapChooserDialog.newInstance(getActivity(),
				getResources().getString(R.string.title_selected_map), this);
	}

	@Override
	public void onJumpTo(Bundle data) {
		m("jump to  map");
		int action = data.getInt(C.map.KEY_ACTION);
		switch (action) {
		case C.map.ACTION_NAVIGATION:
			naviByNodeName("办公室大门", data.getString(C.map.KEY_NAVI_ARG_NAME));
			break;
		}
	}

	private void naviByNodeName(String srcName, String dstName) {
		List<INode> nodes = ((IMapAdapter) mTileMap.getAdapter()).getINodes();
		INode from = null, to = null;
		for (INode node : nodes) {
			if (from == null && node.getName().equals(srcName)) {
				from = node;
				if (to != null)
					break;
			}
			if (to == null && node.getName().equals(dstName)) {
				to = node;
				if (from != null)
					break;
			}
		}
		if (from != null && to != null) {
			onConfirmFrom(null, from);
			onConfirmTo(null, from, to);
			mTileMap.frameTo(getX(to), getY(to));
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mTileMap.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
		mTileMap.resume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTileMap != null) {
			mTileMap.destroy();
			mTileMap = null;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add_map:
			mDlgChooseMapDir.show();
			break;
		case R.id.action_set_map:
			mDlgChooseMap.show();
			break;
		case R.id.action_set_beacon:
			handleSetBeacon();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void handleSetBeacon() {
		SharedPreferences sp = getActivity().getSharedPreferences(
				C.PREFERENCES_MANAGE, Context.MODE_PRIVATE);
		Long id = sp.getLong(C.map.KEY_CURRENT_MAPID, C.map.DEFAULT_MAP_ID);
		mExtraData.clear();
		mExtraData.putLong(C.map.EXTRA_SELECTED_MAP_ID, id);
		Intent intent = new Intent(getActivity(), ManageBeaconActivity.class);
		intent.putExtras(mExtraData);
		startActivity(intent);
	}

	@Override
	public void OnConfirmDirectoryChoose(@NonNull String path) {
		mExtraData.clear();
		mExtraData.putString(C.map.EXTRA_SELECTED_MAP_PATH, path);
		mDlgMapParamsSetter.show();
	}

	@Override
	public void onConfirmSetting(long width, long height, double scale) {
		mExtraData.putLong(C.map.EXTRA_MAP_PX_WIDTH, width);
		mExtraData.putLong(C.map.EXTRA_MAP_PX_HEIGHT, height);
		mExtraData.putDouble(C.map.EXTRA_MAP_MM_PX_SCALE, scale);
		Intent intent = new Intent(getActivity(), CreateMapActivity.class);
		intent.putExtras(mExtraData);
		startActivityForResult(intent, REQ_CREATE_MAP);
	}

	@Override
	public void onMapSelected(Long id) {
		mExtraData.clear();
		mExtraData.putLong(C.map.EXTRA_SELECTED_MAP_ID, id);
		Intent intent = new Intent(getActivity(), ManageMapActivity.class);
		intent.putExtras(mExtraData);
		startActivityForResult(intent, REQ_MANAGE_MAP);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_CREATE_MAP:
		case REQ_MANAGE_MAP:
			handleResultForCreateMap(resultCode, data);
			mDlgChooseMap.notifyMapsChanged();
			break;
		}

	}

	private void handleResultForCreateMap(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Long id = data.getLongExtra(C.map.KEY_CURRENT_MAPID,
					C.map.DEFAULT_MAP_ID);
			mTileMap.clearNodeViews();
			mTileMap.setAdapter(new IMapAdapter(getActivity(), id));
			mTileMap.setupMapDefault(true);
			List<INode> nodes = ((IMapAdapter) mTileMap.getAdapter())
					.getINodes();
			mAlgorithmFindPath.changeNodes(nodes);
		}
	}

	@Override
	public void onNodeClick(Object node, int x, int y) {
		INode inode = (INode) node;
		if (mCalloutFindPath.isPathFromStage()) {
			mCalloutFindPath.setFrom(inode);
			mTileMap.addCallout(mCalloutFindPath, x / mTileMap.getScale(), y
					/ mTileMap.getScale());
		} else {
			if (inode != mCalloutFindPath.getFrom()) {
				mCalloutFindPath.setTo(inode);
				mTileMap.addCallout(mCalloutFindPath, x / mTileMap.getScale(),
						y / mTileMap.getScale());
			}
		}
	}

	@Override
	public void onConfirmFrom(View callout, INode from) {
		mTileMap.removeMarker(mIvFrom);
		mTileMap.removeMarker(mIvTo);
		mTileMap.removePath(mDrawPath);
		mTileMap.removePath(mDrawPathArrow);
		mIvFrom.setTag(from);
		mTileMap.addMarker(mIvFrom, getX(from), getY(from), -0.5f, -1.0f);
	}

	@Override
	public void onCancelFrom(View callout, INode from) {
		mTileMap.removeMarker(mIvFrom);
	}

	@Override
	public void onConfirmTo(View callout, INode from, INode to) {
		// m("从：" + from.getName() + "\n" + "到：" + to.getName());
		mAlgorithmFindPath.setFrom(from);
		mAlgorithmFindPath.computePaths();
		List<INode> nodes = mAlgorithmFindPath.getShortestPathTo(to);
		if (nodes.size() > 1) {
			drawPath(nodes);
		} else {
			m("路径不可达");
		}
		mIvTo.setTag(to);
		mTileMap.addMarker(mIvTo, getX(to), getY(to), -0.5f, -1.0f);
	}

	private void drawPath(List<INode> nodes) {
		List<double[]> positions = new ArrayList<>();
		for (INode node : nodes) {
			positions.add(new double[] { getX(node), getY(node) });
		}
		mDrawPath = mTileMap.drawPath(positions, mPaintPath);

		drawArrow(positions.get(positions.size() - 2),
				positions.get(positions.size() - 1));

	}

	private void drawArrow(double[] lastButOne, double[] last) {
		double x0, y0, x1, y1;
		List<double[]> positions = new ArrayList<>();
		x1 = last[0];
		y1 = last[1];
		x0 = lastButOne[0];
		y0 = lastButOne[1];

		double stdArrowLen = dp(8);

		double deltaX = x1 - x0;
		double deltaY = y1 - y0;
		double stickLen = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
		double frac = stdArrowLen / stickLen;

		double point_x_1 = x0 + ((1 - frac) * deltaX + frac * deltaY);
		double point_y_1 = y0 + ((1 - frac) * deltaY - frac * deltaX);

		double point_x_2 = x1;
		double point_y_2 = y1;

		double point_x_3 = x0 + ((1 - frac) * deltaX - frac * deltaY);
		double point_y_3 = y0 + ((1 - frac) * deltaY + frac * deltaX);

		positions.add(new double[] { point_x_1, point_y_1 });
		positions.add(new double[] { point_x_2, point_y_2 });
		positions.add(new double[] { point_x_3, point_y_3 });

		mDrawPathArrow = mTileMap.drawPath(positions, mPaintArrow);
	}

	/**
	 * 获得node.x的实际像素值
	 */
	private double getX(INode node) {
		return node.getX() / mTileMap.getAdapter().getScale();
	}

	/**
	 * 获得node.y的实际像素值
	 */
	private double getY(INode node) {
		return node.getY() / mTileMap.getAdapter().getScale();
	}

	private int dp(int dp) {
		return Views.dip2px(getActivity(), dp);
	}

}
