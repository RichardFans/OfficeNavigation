package com.richard.officenavigation.fragment;

import jama.Matrix;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jkalman.JKalman;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.qozix.tileview.paths.DrawablePath;
import com.richard.officenavigation.CreateMapActivity;
import com.richard.officenavigation.ManageBeaconActivity;
import com.richard.officenavigation.ManageMapActivity;
import com.richard.officenavigation.OfficeNaviApplication;
import com.richard.officenavigation.OfficeNaviApplication.onRangeBeaconsInRegionListener;
import com.richard.officenavigation.R;
import com.richard.officenavigation.TrainNodesActivity;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.callout.FindPathCallout;
import com.richard.officenavigation.callout.FindPathCallout.onConfirmPathListener;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IBeacon;
import com.richard.officenavigation.dao.ICluster;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.dialog.DirectoryChooserDialog;
import com.richard.officenavigation.dialog.DirectoryChooserDialog.OnConfirmDirectoryChooseListener;
import com.richard.officenavigation.dialog.MapChooserDialog;
import com.richard.officenavigation.dialog.MapChooserDialog.onMapSelectedListener;
import com.richard.officenavigation.dialog.MapParamsSetterDialog;
import com.richard.officenavigation.dialog.MapParamsSetterDialog.onConfirmSettingListener;
import com.richard.officenavigation.findpath.Dijkstra;
import com.richard.officenavigation.view.CircleManager.DrawableCircle;
import com.richard.officenavigation.view.DespoticTileView;
import com.richard.officenavigation.view.MapTileView.onNodeClickListener;
import com.richard.utils.Maths;
import com.richard.utils.Misc;
import com.richard.utils.Views;

public class MapFragment extends TabPagerFragment implements
		OnConfirmDirectoryChooseListener, onConfirmSettingListener,
		onMapSelectedListener, onNodeClickListener, onConfirmPathListener,
		onRangeBeaconsInRegionListener, SensorEventListener {
	private static final int REQ_CREATE_MAP = 1;
	private static final int REQ_MANAGE_MAP = 2;

	protected static final int SMALL_SCALE_THRESHOLD = 2200;

	private DespoticTileView mTileMap;
	private DirectoryChooserDialog mDlgChooseMapDir;
	private MapParamsSetterDialog mDlgMapParamsSetter;
	private MapChooserDialog mDlgChooseMap;

	private ImageView mIvFrom;
	private ImageView mIvTo;
	private ImageView mIvSelf;
	private FindPathCallout mCalloutFindPath;
	private Dijkstra mAlgorithmFindPath;
	private DrawablePath mDrawPath, mDrawPathArrow;
	private Paint mPaintPath, mPaintArrow;

	private SparseArray<IBeacon> mMapBeacon;
	private SparseArray<DrawableCircle> mMapBeaconCircle;

	private Bundle mExtraData;

	private int mXVal;
	private SparseArray<DataForKalman> mBeaconsDataForKalman;
	private SparseIntArray mBeaconsDataCorrected;

	private List<ICluster> mClusterList;

	private Handler mHandler;
	private INode mLastNodeFind;
	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			Map<Integer, Integer> datas = Misc.findBiggestDatas(
					mBeaconsDataCorrected, C.map.DISTRIBUTION_K_VAL);
			Map<Integer, Integer> kDatas = datas;
			Map<Integer, Integer> qDatas = new HashMap<>();
			int i = 0;
			for (Entry<Integer, Integer> e : kDatas.entrySet()) {
				if (++i > C.map.CLUSTER_Q_VAL)
					break;
				qDatas.put(e.getKey(), e.getValue());
			}

			List<ICluster> matchedClusters = Misc.findMatchedClusters(qDatas,
					mClusterList);
			INode nodeFind = Misc.findNearestNode(getActivity(),
					matchedClusters, mCurOri, kDatas);
			if (nodeFind != null) {
				// 如果找到了合适的节点，还要和之前找到的节点比较，两个节点不能相差得太远，
				// 否则认为是小尺度误差，需要进一步进行修正
				if (mLastNodeFind != null) {
					double dD = Maths.distance(mLastNodeFind.getX(),
							mLastNodeFind.getY(), nodeFind.getX(),
							nodeFind.getY());
					if (dD > SMALL_SCALE_THRESHOLD) {
						// 只补偿最强的信号值，取最强信号值的+4%/-4%
						int key = kDatas.keySet().iterator().next();
						int value = kDatas.get(key);
						int dy = Math.round(value / 25);

						kDatas.put(key, value - dy);
						INode nodeFindMinus = Misc
								.findNearestNode(getActivity(),
										matchedClusters, mCurOri, kDatas);

						kDatas.put(key, value + dy);
						INode nodeFindPlus = Misc
								.findNearestNode(getActivity(),
										matchedClusters, mCurOri, kDatas);

						double mD = Maths.distance(mLastNodeFind.getX(),
								mLastNodeFind.getY(), nodeFindMinus.getX(),
								nodeFindMinus.getY());
						double pD = Maths.distance(mLastNodeFind.getX(),
								mLastNodeFind.getY(), nodeFindPlus.getX(),
								nodeFindPlus.getY());
						if (mD > pD) {
							if (pD < dD) {
								nodeFind = nodeFindPlus;
							}
						} else {
							if (mD < dD) {
								nodeFind = nodeFindMinus;
							}
						}
					}
				}
				if (mLastNodeFind != nodeFind) {
					mLastNodeFind = nodeFind;
					mTileMap.removeMarker(mIvSelf);
					mTileMap.addMarker(mIvSelf, getX(nodeFind), getY(nodeFind),
							-0.5f, -1.0f);
				}
				// d("===============>");
				// for (IRssi r : nodeFind.getRssis()) {
				// if (r.getOrientation() == mCurOri) {
				// double y = r.getValue();
				// double y0 = mBeaconsDataCorrected.get((int) r
				// .getBeaconId());
				// d("beacon(" + r.getBeaconId() + ", nodeId = "
				// + nodeFind.getId() + "): y = " + y + ", y0 = "
				// + y0);
				// }
				// }
			}

			mHandler.postDelayed(this, 2000);
		}
	};
	// 方向相关
	private SensorManager mSensorManager;
	private float[] aValues = new float[3];
	private float[] mValues = new float[3];
	private int rotation;

	private float currentDegree = 0f;
	private int mCurOri;

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
		IMapAdapter adapter = new IMapAdapter(getActivity(), mapId);
		mTileMap.setAdapter(adapter);
		mTileMap.setupMapDefault(true);
		mTileMap.setOnNodeClickListener(this);

		mIvFrom = new ImageView(getActivity());
		mIvFrom.setImageResource(R.drawable.find_path_start);
		mIvTo = new ImageView(getActivity());
		mIvTo.setImageResource(R.drawable.find_path_end);
		mIvSelf = new ImageView(getActivity());
		mIvSelf.setImageResource(R.drawable.map_node_icon);

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

		// 方向
		mSensorManager = (SensorManager) getActivity().getSystemService(
				Context.SENSOR_SERVICE);
		WindowManager wm = (WindowManager) getActivity().getSystemService(
				Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		rotation = display.getRotation();
		updateOrientation(new float[] { 0, 0, 0 });

		mMapBeacon = new SparseArray<>();
		List<IBeacon> beacons = adapter.getIBeacons();
		for (IBeacon beacon : beacons) {
			mMapBeacon.put(beacon.getMinor(), beacon);
		}
		mMapBeaconCircle = new SparseArray<>();
		mBeaconsDataForKalman = new SparseArray<>();
		mBeaconsDataCorrected = new SparseIntArray();
		mClusterList = loadAllClusters();
		mHandler = new Handler();
		mHandler.postDelayed(mRunnable, 5000);
	}

	private List<ICluster> loadAllClusters() {
		DaoSession session = SingletonDaoSession.getInstance(getActivity());
		return session.getIClusterDao().loadAll();
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
	public void onRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
		if (beacons.size() > 0) {
			mBeaconsDataCorrected.clear();
			for (Beacon beacon : beacons) {
				int minor = beacon.getId3().toInt();
				DataForKalman beaconDataForKalman = mBeaconsDataForKalman
						.get(minor);
				if (null == beaconDataForKalman) {
					beaconDataForKalman = new DataForKalman();
					mBeaconsDataForKalman.put(minor, beaconDataForKalman);
				}

				if (beacon.getRssi() < 0) {
					int corrected = ftKalman(beaconDataForKalman,
							beacon.getRssi());
					mBeaconsDataCorrected.put(minor, corrected);
				}
			}
			mXVal++;
		}
	}

	private int ftKalman(DataForKalman beaconDataForKalman, int observedValue) {
		// 插值
		int xLast = beaconDataForKalman.xVal;
		int yLast = beaconDataForKalman.yVal;
		int xVal = mXVal;
		int yVal = observedValue;
		ArrayList<Integer> datas = new ArrayList<>();
		// 插值
		if (xLast > 0) {
			if (xVal > xLast + 1) {
				int dy = (yVal - yLast) / (xVal - xLast);
				for (int j = 1; j < xVal - xLast; j++)
					datas.add(yLast + dy * j);
			}
		} else {
			for (int j = 0; j < xVal; j++)
				datas.add(yVal);
		}
		datas.add(yVal);
		// 滤波
		if (xLast == 0) {
			beaconDataForKalman.setInitState(datas.get(0));
			datas.remove(0);
		}
		for (int d : datas) {
			beaconDataForKalman.iterate(d);
		}
		// 更新
		beaconDataForKalman.xVal = mXVal;
		beaconDataForKalman.yVal = observedValue;
		return beaconDataForKalman.getCorrectedData();
	}

	@SuppressWarnings("unused")
	private void updateBeaconCircle(final Beacon beacon) {
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				double scale = mTileMap.getAdapter().getScale();
				IBeacon ibeacon = mMapBeacon.get(beacon.getId3().toInt());
				if (ibeacon != null) {
					DrawableCircle circle = mMapBeaconCircle.get(ibeacon
							.getMinor());
					mTileMap.removeCircle(circle);
					circle = mTileMap.drawCircle(ibeacon.getX() / scale,
							ibeacon.getY() / scale, beacon.getDistance() * 1000
									/ scale);
					mMapBeaconCircle.put(ibeacon.getMinor(), circle);
				}
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		// 信标
		((OfficeNaviApplication) this.getActivity().getApplication())
				.setOnRangeBeaconsInRegionListener(null);
		// 方向
		mSensorManager.unregisterListener(this);
		// 地图
		mTileMap.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
		// 信标
		((OfficeNaviApplication) this.getActivity().getApplication())
				.setOnRangeBeaconsInRegionListener(this);
		// 方向
		Sensor aSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor mSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, aSensor,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_GAME);
		// 地图
		mTileMap.resume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mTileMap != null) {
			mTileMap.destroy();
			mTileMap = null;
		}
		mHandler.removeCallbacks(mRunnable);
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
		case R.id.action_locate_training:
			handleLocateTraining();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mValues = event.values;
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			aValues = event.values;
		updateOrientation(calculateOrientation());
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	private float[] calculateOrientation() {
		float[] values = new float[3];
		float[] inR = new float[9];
		float[] outR = new float[9];
		// Determine the rotation matrix
		SensorManager.getRotationMatrix(inR, null, aValues, mValues);
		// Remap the coordinates based on the natural device orientation.
		int x_axis = SensorManager.AXIS_X;
		int y_axis = SensorManager.AXIS_Y;
		switch (rotation) {
		case (Surface.ROTATION_90):
			x_axis = SensorManager.AXIS_Y;
			y_axis = SensorManager.AXIS_MINUS_X;
			break;
		case (Surface.ROTATION_180):
			y_axis = SensorManager.AXIS_MINUS_Y;
			break;
		case (Surface.ROTATION_270):
			x_axis = SensorManager.AXIS_MINUS_Y;
			y_axis = SensorManager.AXIS_X;
			break;
		default:
			break;
		}
		SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);
		// Obtain the current, corrected orientation.
		SensorManager.getOrientation(outR, values);
		// Convert from Radians to Degrees.
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);
		return values;
	}

	private void updateOrientation(float[] values) {
		float degree = Math.round(values[0]);
		if (-degree != currentDegree) {
			degreeToOrientation(degree);
			currentDegree = -degree;
		}
	}

	private String degreeToOrientation(float degree) {
		int oriStringId = 0;
		if (degree >= -45 && degree < 45) {
			oriStringId = R.string.ori_north;
			mCurOri = C.map.ORIENT_NORTH;
		} else if (degree >= 45 && degree <= 135) {
			oriStringId = R.string.ori_east;
			mCurOri = C.map.ORIENT_EAST;
		} else if ((degree >= 135 && degree <= 180) || (degree) >= -180
				&& degree < -135) {
			oriStringId = R.string.ori_sorth;
			mCurOri = C.map.ORIENT_SORTH;
		} else if (degree >= -135 && degree < -45) {
			oriStringId = R.string.ori_west;
			mCurOri = C.map.ORIENT_WEST;
		}
		return getActivity().getString(oriStringId);
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

	private void handleLocateTraining() {
		SharedPreferences sp = getActivity().getSharedPreferences(
				C.PREFERENCES_MANAGE, Context.MODE_PRIVATE);
		Long id = sp.getLong(C.map.KEY_CURRENT_MAPID, C.map.DEFAULT_MAP_ID);
		mExtraData.clear();
		mExtraData.putLong(C.map.EXTRA_SELECTED_MAP_ID, id);
		Intent intent = new Intent(getActivity(), TrainNodesActivity.class);
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

	class DataForKalman {
		int xVal;
		int yVal;
		Matrix s; // state [y, dy]
		Matrix c; // corrected state [y, dy]
		Matrix m; // measurement [x]
		JKalman kalman;

		public DataForKalman() {
			try {
				kalman = new JKalman(2, 1);
				s = new Matrix(2, 1); // state [y, dy]
				c = new Matrix(2, 1); // corrected state [y, dy]
				m = new Matrix(1, 1); // measurement [x]
				double[][] tr = { { 1, 1 }, { 0, 1 } };
				kalman.setTransition_matrix(new Matrix(tr));
				kalman.setError_cov_post(kalman.getError_cov_post().identity());
			} catch (Exception e) {
				e.printStackTrace();
				e("DataForKalman");
			}
		}

		public void setInitState(int rssi) {
			// 设置初始状态
			s.set(0, 0, rssi);
			s.set(0, 0, 0);
		}

		public void iterate(int rssi) {
			s = kalman.Predict();
			m.set(0, 0, rssi);
			c = kalman.Correct(m);
		}

		public int getCorrectedData() {
			return (int) c.get(0, 0);
		}
	}
}
