package com.richard.officenavigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.richard.officenavigation.adapter.AverageResultsAdapter;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.IRssiDao;
import com.richard.officenavigation.dao.IRssiRaw;
import com.richard.officenavigation.dao.IRssiRawDao;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.dialog.CompassDialog;
import com.richard.officenavigation.dialog.CompassDialog.onConfirmOrientationListener;
import com.richard.officenavigation.dialog.InfoDialog;

public class AverageTrainingActivity extends BaseActivity implements
		OnClickListener, onConfirmOrientationListener,
		onRangeBeaconsInRegionListener, OnLongClickListener {
	private TextView[] mTvsOrient = new TextView[4];
	private TextView mTvCountDown, mTvTitleCountDown;
	private EditText mEditTrainTime;
	private Button mBtnTrain;
	private ExpandableListView mElvResults;
	private AverageResultsAdapter mElAdapter;
	private CompassDialog mDlgCompass;

	private int mTrainingStage;
	private TimerCountDown mTimerCountDown;
	private SparseArray<SparseIntArray> mRssiBeacons;
	private int mXVal;

	private DaoSession mDaoSession;
	private final int[] mOrients = { R.string.ori_east, R.string.ori_sorth,
			R.string.ori_west, R.string.ori_north };

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
		mElAdapter = new AverageResultsAdapter(this);
		mElvResults.setAdapter(mElAdapter);
		mBtnTrain.setOnClickListener(this);
		// for test
		mBtnTrain.setOnLongClickListener(this);
		// -- end for test
		for (TextView tv : mTvsOrient) {
			tv.setOnClickListener(this);
		}
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
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
		} else if (mTrainingStage == 4) { //滤波
			mElAdapter.filteringByKalman();
			mElAdapter.filteringByGaussian();
			mElAdapter.notifyDataSetChanged();

			mElvResults.expandGroup(0);
			mElvResults.expandGroup(1);
			mElvResults.expandGroup(2);
			mElvResults.expandGroup(3);
			mElvResults.setSelectedGroup(0);

			mBtnTrain.setText(getString(R.string.action_done));
			mTrainingStage++;
		} else {
			handleActionDone();
		}
	}

	private void handleActionDone() {
		InfoDialog.newInstance(this, "训练完成", "是否保存所有原始数据？",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Bundle e = getIntent().getExtras();
						Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
						Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
						if (which == DialogInterface.BUTTON_POSITIVE) {
							saveRawDatas(nodeId, mapId);
						}
						saveTrainingResult(nodeId, mapId);
						dialog.dismiss();
						mDaoSession.clear();
						mDaoSession = null;
						setResult(RESULT_OK, getIntent());
						finish();
					}

				}).show();
	}

	private void saveRawDatas(Long nodeId, Long mapId) {
		// 清除与该节点相关的原始数据
		IRssiRawDao rssiRawDao = mDaoSession.getIRssiRawDao();
		rssiRawDao
				.queryBuilder()
				.where(IRssiRawDao.Properties.MapId.eq(mapId),
						IRssiRawDao.Properties.NodeId.eq(nodeId)).buildDelete()
				.executeDeleteWithoutDetachingEntities();
		// 保存新的原始数据
		ArrayList<IRssiRaw> datas = mElAdapter.getAllRawDatasForPersistence(
				nodeId, mapId);
		rssiRawDao.insertInTx(datas);
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
				mXVal = 0;
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
				rssiBeacon.put(mXVal, rssi);
			}
			mXVal++;
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
				mBtnTrain.setText(getString(R.string.action_filter));
			}
			mBtnTrain.setVisibility(View.VISIBLE);
			mTvCountDown.setVisibility(View.INVISIBLE);
			mTvTitleCountDown.setVisibility(View.INVISIBLE);

			if (mTrainingStage < 4) {
				((OfficeNaviApplication) getApplication())
						.setOnRangeBeaconsInRegionListener(null);
				for (int i = 0; i < mRssiBeacons.size(); i++) {
					int key = mRssiBeacons.keyAt(i);
					SparseIntArray rssiBeacon = mRssiBeacons.get(key);
					mElAdapter.setRawDatas(mTrainingStage, key, rssiBeacon);
					mElvResults.expandGroup(mTrainingStage);
					mElvResults.setSelectedGroup(mTrainingStage);
				}
			}
			mTrainingStage++;
			updateTvsOrient();
		}
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
		mElAdapter.filteringByKalman();
		mElAdapter.filteringByGaussian();
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
				InfoDialog.newInstance(AverageTrainingActivity.this, "训练完成",
						"是否保存训练结果？", new DialogInterface.OnClickListener() {

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
