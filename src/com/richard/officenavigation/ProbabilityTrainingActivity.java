package com.richard.officenavigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.richard.officenavigation.OfficeNaviApplication.onRangeBeaconsInRegionListener;
import com.richard.officenavigation.adapter.ProbabilityResultsAdapter;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.ICluster;
import com.richard.officenavigation.dao.IClusterItem;
import com.richard.officenavigation.dao.IClusterItemDao;
import com.richard.officenavigation.dao.INodeCluster;
import com.richard.officenavigation.dao.INodeClusterDao;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.IRssiDao;
import com.richard.officenavigation.dao.IRssiRaw;
import com.richard.officenavigation.dao.IRssiRawDao;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.dialog.CompassDialog;
import com.richard.officenavigation.dialog.CompassDialog.onConfirmOrientationListener;
import com.richard.officenavigation.dialog.InfoDialog;
import com.richard.utils.Misc;

public class ProbabilityTrainingActivity extends BaseActivity implements
		OnClickListener, onConfirmOrientationListener,
		onRangeBeaconsInRegionListener, OnLongClickListener {
	private TextView[] mTvsOrient = new TextView[4];
	private TextView mTvCountDown, mTvTitleCountDown;
	private EditText mEditTrainTime;
	private Button mBtnTrain;
	private ExpandableListView mElvResults;
	private ProbabilityResultsAdapter mElAdapter;
	private CompassDialog mDlgCompass;

	private int mTrainingStage;
	private TimerCountDown mTimerCountDown;
	/**
	 * 指定方向上扫描到的所有信标的数据集合，每个信标的数据由rssi-count映射构成
	 */
	private SparseArray<SparseIntArray> mRssiBeacons;

	private DaoSession mDaoSession;
	private final int[] mOrients = { R.string.ori_east, R.string.ori_sorth,
			R.string.ori_west, R.string.ori_north };

	private List<ICluster> mClusterList;
	private List<INodeCluster> mNodeClusterList;

	@Override
	protected void findViews() {
		setContentView(R.layout.activity_train_node);
		mTvsOrient[0] = (TextView) findViewById(R.id.tv_ori_east);
		mTvsOrient[1] = (TextView) findViewById(R.id.tv_ori_sorth);
		mTvsOrient[2] = (TextView) findViewById(R.id.tv_ori_west);
		mTvsOrient[3] = (TextView) findViewById(R.id.tv_ori_north);
		mTvCountDown = (TextView) findViewById(R.id.tv_countdown_time);
		mTvTitleCountDown = (TextView) findViewById(R.id.tv_title_countdown);
		mEditTrainTime = (EditText) findViewById(R.id.edit_train_time);
		mBtnTrain = (Button) findViewById(R.id.btn_training);
		mElvResults = (ExpandableListView) findViewById(R.id.elv_results);
		mDlgCompass = CompassDialog.newInstance(this, "", this);
		mRssiBeacons = new SparseArray<>();
	}

	@Override
	protected void setupViews() {
		mEditTrainTime.setText("" + C.map.DEFAULT_TRAINING_TIME);
		mElAdapter = new ProbabilityResultsAdapter(this);
		mElvResults.setAdapter(mElAdapter);
		mBtnTrain.setOnClickListener(this);
		// for test
		// mBtnTrain.setOnLongClickListener(this);
		// !for test
		for (TextView tv : mTvsOrient) {
			tv.setOnClickListener(this);
		}
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		mClusterList = mDaoSession.getIClusterDao().loadAll();
		mNodeClusterList = new ArrayList<>();
		mTrainingStage = 0;
	}

	/**
	 * for test
	 * 
	 * @param data_nr
	 * @param data_min
	 * @param data_max
	 * @return
	 */
	public SparseIntArray getDatas(int data_nr, float data_min, float data_max) {
		SparseIntArray datasRaw = new SparseIntArray();
		for (int j = 0; j < data_nr; j++) {
			int data = (int) Math.round((data_max - data_min) * Math.random()
					+ data_min);
			datasRaw.put(j, data);
		}
		return datasRaw;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_training) {
			handleTrainingStart();
			return;
		}
		switch (v.getId()) {
		case R.id.tv_ori_east:
			mTrainingStage = 0;
			break;
		case R.id.tv_ori_sorth:
			mTrainingStage = 1;
			break;
		case R.id.tv_ori_west:
			mTrainingStage = 2;
			break;
		case R.id.tv_ori_north:
			mTrainingStage = 3;
			break;
		}
		updateTvsOrient();
	}

	/**
	 * 根据方向改变相关的TextView
	 */
	private void updateTvsOrient() {
		for (TextView tv : mTvsOrient) {
			tv.setTextColor(getResources().getColor(R.color.black_text));
		}
		if (mTrainingStage < 4)
			mTvsOrient[mTrainingStage].setTextColor(getResources().getColor(
					R.color.dark_red_text));
	}

	private void handleTrainingStart() {
		if (mTrainingStage < 4) {
			String time = mEditTrainTime.getText().toString();
			if (time.equals("")) {
				m("请输入训练时间！");
			} else {
				int sec = Integer.parseInt(time);
				if (sec > 1) {
					mDlgCompass.setTitle("确认朝向"
							+ getString(mOrients[mTrainingStage]) + "方");
					mDlgCompass.show();
				} else {
					m("训练时间应不少于10秒！");
				}
			}
		} else {
			handleActionDone();
		}
	}

	private void handleActionDone() {
		InfoDialog.newInstance(this, "训练完成", "是否保存训练结果？",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Bundle e = getIntent().getExtras();
						Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
						Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
						if (which == DialogInterface.BUTTON_POSITIVE) {
							saveTrainingResult(nodeId, mapId);
						}
						dialog.dismiss();
						mDaoSession.clear();
						mDaoSession = null;
						setResult(RESULT_OK, getIntent());
						finish();
					}
				}).show();
	}

	private void saveTrainingResult(Long nodeId, Long mapId) {
		IRssiDao rssiDao = mDaoSession.getIRssiDao();
		rssiDao.queryBuilder()
				.where(IRssiDao.Properties.MapId.eq(mapId),
						IRssiDao.Properties.NodeId.eq(nodeId)).buildDelete()
				.executeDeleteWithoutDetachingEntities();
		// 保存新的数据
		ArrayList<IRssi> datas = mElAdapter.getAllDatasForPersistence(nodeId,
				mapId);
		if (!datas.isEmpty()) {
			rssiDao.insertInTx(datas);
		}
		INodeClusterDao nodeClusterDao = mDaoSession.getINodeClusterDao();
		nodeClusterDao.queryBuilder()
				.where(INodeClusterDao.Properties.NodeId.eq(nodeId))
				.buildDelete().executeDeleteWithoutDetachingEntities();
		if (!mNodeClusterList.isEmpty()) {
			nodeClusterDao.insertInTx(mNodeClusterList);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mTimerCountDown != null) {
			mTimerCountDown.cancel();
			((OfficeNaviApplication) getApplication())
					.setOnRangeBeaconsInRegionListener(null);
		}
	}

	@Override
	public void onConfirmOrientation(int orientation) {
		if (orientation != mTrainingStage) {
			m("请先调整好训练方向！");
		} else {
			// 开始训练
			mBtnTrain.setVisibility(View.INVISIBLE);
			mTvCountDown.setVisibility(View.VISIBLE);
			mTvTitleCountDown.setVisibility(View.VISIBLE);

			int sec = Integer.parseInt(mEditTrainTime.getText().toString());
			mTimerCountDown = new TimerCountDown(sec * 1000, 1000);
			mTimerCountDown.start();
			if (mTrainingStage < 4) {
				((OfficeNaviApplication) getApplication())
						.setOnRangeBeaconsInRegionListener(this);
				mRssiBeacons.clear();
			}
		}
	}

	@Override
	public void onRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
		if (beacons.size() > 0) {
			for (Beacon beacon : beacons) {
				int minor = beacon.getId3().toInt();
				int rssi = beacon.getRssi();
				SparseIntArray rssiBeacon = mRssiBeacons.get(minor);
				if (rssiBeacon == null) {
					rssiBeacon = new SparseIntArray();
					mRssiBeacons.put(minor, rssiBeacon);
				}
				rssiBeacon.put(rssi, rssiBeacon.get(rssi) + 1);
			}
		}
	};

	class TimerCountDown extends CountDownTimer {

		public TimerCountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onTick(long millisUntilFinished) {
			int sec = (int) (millisUntilFinished / 1000);
			mTvCountDown.setText("" + sec);
		}

		@Override
		public void onFinish() {
			if (mTrainingStage == 0) {
				mBtnTrain.setText(getString(R.string.action_continue));
			} else if (mTrainingStage == 3) {
				mBtnTrain.setText(getString(R.string.action_done));
			}
			mBtnTrain.setVisibility(View.VISIBLE);
			mTvCountDown.setVisibility(View.INVISIBLE);
			mTvTitleCountDown.setVisibility(View.INVISIBLE);

			if (mTrainingStage < 4) {
				((OfficeNaviApplication) getApplication())
						.setOnRangeBeaconsInRegionListener(null);
				for (int i = 0; i < mRssiBeacons.size(); i++) {
					int minor = mRssiBeacons.keyAt(i); // 获得信标minor
					SparseIntArray rssiBeacon = mRssiBeacons.get(minor); // 获得该信标的概率分布

					mElAdapter.setDatas(mTrainingStage, minor, rssiBeacon);
					mElvResults.expandGroup(mTrainingStage);
					mElvResults.setSelectedGroup(mTrainingStage);
				}
				updateCluster();
			}
			mTrainingStage++;
			updateTvsOrient();
		}
	}

	public void updateCluster() {
		Bundle e = getIntent().getExtras();
		Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
		Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
		// 将mRssiBeacons中的信标数据变成均值
		SparseIntArray averageDatas = new SparseIntArray();
		for (int i = 0; i < mRssiBeacons.size(); i++) {
			int minor = mRssiBeacons.keyAt(i); // 获得信标minor
			SparseIntArray rssiBeacon = mRssiBeacons.get(minor);
			int sum = 0, total = 0;
			for (int j = 0; j < rssiBeacon.size(); j++) {
				sum += rssiBeacon.keyAt(j) * rssiBeacon.valueAt(j);
				total += rssiBeacon.valueAt(j);
			}
			averageDatas.put(minor, Math.round((float) sum / total));
		}
		// 找到用于分类的q组数据
		Map<Integer, Integer> datas = Misc.findBiggestDatas(averageDatas, C.map.CLUSTER_Q_VAL);
		// 找到这q组数据所属的分类
		ICluster c = Misc.findMatchedCluster(datas, mClusterList);
		if (c == null) // 不存在则创建分类
			c = createCluster(datas, mapId);
		// 更新节点分类列表
		INodeCluster nc = new INodeCluster();
		nc.setClusterId(c.getId());
		nc.setNodeId(nodeId);
		nc.setOrientation(mTrainingStage);
		mNodeClusterList.add(nc);
	}

	/**
	 * 创建新的分类
	 * @param datas 用于创建新的分类的数据（注意：key是信号强度，value是信标的minor）
	 * @param mapId 分类所属地图id
	 * @return 新创建的分类实体
	 */
	private ICluster createCluster(Map<Integer, Integer> datas, Long mapId) {
		ICluster c = new ICluster();
		mDaoSession.getIClusterDao().insert(c);
		IClusterItemDao clusterItemDao = mDaoSession.getIClusterItemDao();
		for (Integer minor : datas.keySet()) {
			IClusterItem item = new IClusterItem();
			item.setClusterId(c.getId());
			item.setMinor(minor);
			item.setMapId(mapId);
			clusterItemDao.insert(item);
		}
		mClusterList.add(c); // 记得更新分类列表
		return c;
	}

	/**
	 * for test
	 */
	@Override
	public boolean onLongClick(View v) {
		Bundle e = getIntent().getExtras();
		Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
		Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
		IRssiRawDao rssiRawDao = mDaoSession.getIRssiRawDao();
		List<IRssiRaw> allRawDatas = rssiRawDao
				.queryBuilder()
				.where(IRssiRawDao.Properties.MapId.eq(mapId),
						IRssiRawDao.Properties.NodeId.eq(nodeId)).list();
		mElAdapter.setAllRawDatas(allRawDatas);
		mElAdapter.notifyDataSetChanged();

		mElvResults.expandGroup(0);
		mElvResults.expandGroup(1);
		mElvResults.expandGroup(2);
		mElvResults.expandGroup(3);
		mElvResults.setSelectedGroup(0);

		mBtnTrain.setText(getString(R.string.action_done));
		mBtnTrain.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InfoDialog.newInstance(ProbabilityTrainingActivity.this,
						"训练完成", "是否保存训练结果？",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Bundle e = getIntent().getExtras();
								Long nodeId = e
										.getLong(C.map.EXTRA_SELECTED_NODE_ID);
								Long mapId = e
										.getLong(C.map.EXTRA_SELECTED_MAP_ID);
								if (which == DialogInterface.BUTTON_POSITIVE) {
									saveTrainingResult(nodeId, mapId);
								}
								dialog.dismiss();
								mDaoSession.clear();
								mDaoSession = null;
								finish();
							}
						}).show();
			}
		});
		mBtnTrain.setOnLongClickListener(null);
		mTrainingStage = 4;

		updateTvsOrient();
		return false;
	}
}
