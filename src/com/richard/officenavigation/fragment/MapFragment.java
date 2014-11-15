package com.richard.officenavigation.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;
import com.richard.officenavigation.view.DespoticTileView;

public class MapFragment extends BaseFragment {
	private DespoticTileView mTileMap;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mTileMap = (DespoticTileView) inflater.inflate(R.layout.fragment_map,
				container, false);
		// size of original image at 100% scale
		mTileMap.setSize(2292, 1310);
		mTileMap.setScaleLimits(0, 2);
		// detail levels
		mTileMap.addDetailLevel(1.000f, "tiles/plans/1000/%col%_%row%.jpg",
				"samples/plans.jpg");
		mTileMap.addDetailLevel(0.500f, "tiles/plans/500/%col%_%row%.jpg",
				"samples/plans.jpg");
		mTileMap.addDetailLevel(0.250f, "tiles/plans/250/%col%_%row%.jpg",
				"samples/plans.jpg");
		mTileMap.addDetailLevel(0.125f, "tiles/plans/125/%col%_%row%.jpg",
				"samples/plans.jpg");
		// let's use 0-1 positioning...
		mTileMap.defineRelativeBounds(0, 0, 1, 1);

		// scale it down to manageable size
		mTileMap.setScale(0);

		// center the frame
		frameTo(0.5, 0.5);
		Log.i("mytag", "map onCreateView");
		return mTileMap;
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

	public TileView getTileView() {
		return mTileMap;
	}

	public void frameTo(final double x, final double y) {
		getTileView().post(new Runnable() {
			@Override
			public void run() {
				getTileView().moveToAndCenter(x, y);
			}
		});
	}

}
