package com.richard.officenavigation;

import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import com.richard.officenavigation.adapter.ProbabilityResultsAdapter;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.IRssiDao;
import com.richard.officenavigation.dao.SingletonDaoSession;

public class ViewProbabilityTrainingResult extends BaseActivity {
	private ExpandableListView mElvResults;
	private ProbabilityResultsAdapter mElAdapter;

	private DaoSession mDaoSession;

	@Override
	protected void findViews() {
		setContentView(R.layout.activity_view_training_result);
		findViewById(R.id.container1_cb).setVisibility(View.GONE);
		findViewById(R.id.container2_cb).setVisibility(View.GONE);
		mElvResults = (ExpandableListView) findViewById(R.id.elv_results);
	}

	@Override
	protected void setupViews() {
		mElAdapter = new ProbabilityResultsAdapter(this);
		mElvResults.setAdapter(mElAdapter);
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		Bundle e = getIntent().getExtras();
		Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
		Long mapId = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);

		IRssiDao rssiDao = mDaoSession.getIRssiDao();
		List<IRssi> allDatas = rssiDao
				.queryBuilder()
				.where(IRssiDao.Properties.MapId.eq(mapId),
						IRssiDao.Properties.NodeId.eq(nodeId)).list();
		mElAdapter.setAllDatas(allDatas);
		mElvResults.expandGroup(0);
		mElvResults.expandGroup(1);
		mElvResults.expandGroup(2);
		mElvResults.expandGroup(3);
		mElvResults.setSelectedGroup(0);
	}

}
