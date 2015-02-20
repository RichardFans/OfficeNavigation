package com.richard.officenavigation;

import java.util.List;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ExpandableListView;

import com.richard.officenavigation.adapter.AverageResultsAdapter;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IRssiRaw;
import com.richard.officenavigation.dao.IRssiRawDao;
import com.richard.officenavigation.dao.SingletonDaoSession;

public class ViewAverageTrainingResult extends BaseActivity {
	private CheckBox[] mCbsDataType = new CheckBox[4];
	private ExpandableListView mElvResults;
	private AverageResultsAdapter mElAdapter;

	private DaoSession mDaoSession;

	@Override
	protected void findViews() {
		setContentView(R.layout.activity_view_training_result);
		mCbsDataType[0] = (CheckBox) findViewById(R.id.cb_raw);
		mCbsDataType[1] = (CheckBox) findViewById(R.id.cb_kalman);
		mCbsDataType[2] = (CheckBox) findViewById(R.id.cb_wavelet);
		mCbsDataType[3] = (CheckBox) findViewById(R.id.cb_gauss);
		mElvResults = (ExpandableListView) findViewById(R.id.elv_results);
	}

	@Override
	protected void setupViews() {
		mElAdapter = new AverageResultsAdapter(this);
		mElvResults.setAdapter(mElAdapter);
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		Bundle e = getIntent().getExtras();
		Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
		Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);

		IRssiRawDao rssiRawDao = mDaoSession.getIRssiRawDao();
		List<IRssiRaw> allRawDatas = rssiRawDao
				.queryBuilder()
				.where(IRssiRawDao.Properties.MapId.eq(mapId),
						IRssiRawDao.Properties.NodeId.eq(nodeId)).list();
		mElAdapter.setAllRawDatas(allRawDatas);
		mElvResults.expandGroup(0);
		mElvResults.expandGroup(1);
		mElvResults.expandGroup(2);
		mElvResults.expandGroup(3);
		mElvResults.setSelectedGroup(0);
	}

}
