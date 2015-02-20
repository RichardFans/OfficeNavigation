package com.richard.officenavigation;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.ImageView;

import com.qozix.tileview.markers.MarkerEventListener;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.callout.TrainNodeCallout;
import com.richard.officenavigation.callout.TrainNodeCallout.onConfirmNodeTrainListener;
import com.richard.officenavigation.constants.C;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.INodeDao;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.view.MapTileView;

public class TrainNodesActivity extends BaseActivity implements
		onConfirmNodeTrainListener {
	private static final int REQ_TRAIN_NODE = 1;
	private MapTileView mTileMap;
	private IMap mMap;

	private LongSparseArray<View> mSpareArrayMarker;

	private DaoSession mDaoSession;

	private TrainNodeCallout mCalloutTrainNode;

	@Override
	protected void findViews() {
		mTileMap = new MapTileView(this);
		setContentView(mTileMap);
		mCalloutTrainNode = new TrainNodeCallout(this, mTileMap);
	}

	@Override
	protected void setupViews() {
		Bundle e = getIntent().getExtras();
		Long id = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
		mTileMap.setAdapter(new IMapAdapter(this, id));
		mTileMap.setupMapDefault(false);

		mCalloutTrainNode.setOnConfirmNodeTrainListener(this);
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		mMap = ((IMapAdapter) mTileMap.getAdapter()).getIMap();
		List<INode> nodes = mMap.getNodes();

		if (!nodes.isEmpty()) {
			mSpareArrayMarker = new LongSparseArray<>();
			for (INode node : nodes) {
				ImageView marker = new ImageView(this);
				if (node.isTrained()) {
					marker.setImageResource(R.drawable.map_node_trained);
				} else {
					marker.setImageResource(R.drawable.map_node_icon);
				}
				marker.setTag(node);
				mSpareArrayMarker.put(node.getId(), marker);
				mTileMap.addMarker(marker, node.getX() / mMap.getScale(),
						node.getY() / mMap.getScale(), -0.5f, -1.0f);
			}
		}
		mTileMap.addMarkerEventListener(mTrainNodeMarkerListener);
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQ_TRAIN_NODE:
			handleTrainNodeResult(resultCode, data);
			break;
		}
	}

	private void handleTrainNodeResult(int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED)
			return;
		Bundle e = data.getExtras();
		Long nodeId = e.getLong(C.map.EXTRA_SELECTED_NODE_ID);
		INodeDao nodeDao = mDaoSession.getINodeDao();
		ImageView marker = (ImageView) mSpareArrayMarker.get(nodeId);
		INode node = (INode) marker.getTag();
		node.setTrained(true);
		nodeDao.update(node);
		if (marker != null) {
			marker.setImageResource(R.drawable.map_node_trained);
		}
	}

	@Override
	public void onConfirmNodeTrain(View callout, INode node) {
		Intent intent = new Intent(this, ProbabilityTrainingActivity.class);
		intent.putExtra(C.map.EXTRA_SELECTED_NODE_ID, node.getId());
		intent.putExtra(C.map.EXTRA_SELECTED_MAP_ID, node.getMapId());
		startActivityForResult(intent, REQ_TRAIN_NODE);
	}

	@Override
	public void onConfirmResultView(View callout, INode node) {
		Intent intent = new Intent(this, ViewProbabilityTrainingResult.class);
		intent.putExtra(C.map.EXTRA_SELECTED_NODE_ID, node.getId());
		intent.putExtra(C.map.EXTRA_SELECTED_MAP_ID, node.getMapId());
		startActivity(intent);
	}

	private MarkerEventListener mTrainNodeMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			double scale = mTileMap.getScale();
			mCalloutTrainNode.setNode((INode) view.getTag());
			mCalloutTrainNode.setTag(view);
			mTileMap.addCallout(mCalloutTrainNode, x / scale, y / scale);
		}
	};
}
