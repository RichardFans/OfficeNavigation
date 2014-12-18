package com.richard.officenavigation;

import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.qozix.tileview.markers.MarkerEventListener;
import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.callout.AddBeaconCallout;
import com.richard.officenavigation.callout.AddBeaconCallout.onConfirmBeaconAddListener;
import com.richard.officenavigation.callout.DelBeaconCallout;
import com.richard.officenavigation.callout.DelBeaconCallout.onConfirmBeaconDelListener;
import com.richard.officenavigation.dao.Beacon;
import com.richard.officenavigation.dao.BeaconDao;
import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.SingletonDaoSession;
import com.richard.officenavigation.view.MapTileView;

@SuppressLint("ClickableViewAccessibility")
public class ManageBeaconActivity extends BaseActivity implements
		OnTouchListener, onConfirmBeaconAddListener, onConfirmBeaconDelListener {
	private MapTileView mTileMap;
	private IMap mMap;

	private DaoSession mDaoSession;

	private AddBeaconCallout mCalloutAddBeacon;
	private DelBeaconCallout mCalloutDelBeacon;

	@Override
	protected void findViews() {
		mTileMap = new MapTileView(this);
		setContentView(mTileMap);
		mCalloutAddBeacon = new AddBeaconCallout(this, mTileMap);
		mCalloutDelBeacon = new DelBeaconCallout(this, mTileMap);
	}

	@Override
	protected void setupViews() {
		Bundle e = getIntent().getExtras();
		Long id = e.getLong(C.map.EXTRA_SELECTED_MAP_ID);
		mTileMap.setAdapter(new IMapAdapter(this, id));
		mTileMap.setupMapDefault(false);

		mCalloutAddBeacon.setOnConfirmBeaconAddListener(this);
		mCalloutDelBeacon.setOnConfirmBeaconDelListener(this);
	}

	@Override
	protected void initDatas(Bundle savedInstanceState) {
		mDaoSession = SingletonDaoSession.getInstance(this);
		mMap = ((IMapAdapter) mTileMap.getAdapter()).getIMap();
		List<Beacon> beacons = mMap.getBeacons();
		if (!beacons.isEmpty()) {
			for (Beacon beacon : beacons) {
				ImageView marker = new ImageView(this);
				marker.setImageResource(R.drawable.map_beacon_icon);
				marker.setTag(beacon);
				mTileMap.addMarker(marker, beacon.getX() / mMap.getScale(),
						beacon.getY() / mMap.getScale(), -0.5f, -0.5f);
			}
		}
		mTileMap.addMarkerEventListener(mDelBeaconMarkerListener);
	}

	private void handleActionAddNode() {
		getActionBar().setTitle(R.string.action_add_beacon);
		mTileMap.setOnTouchListener(this);
	}

	private void handleActionDelNode() {
		getActionBar().setTitle(R.string.action_del_beacon);
		mTileMap.addMarkerEventListener(mDelBeaconMarkerListener);
	}

	private void handleActionDone() {
		mDaoSession.clear();
		mDaoSession = null;
		finish();
	}

	private void removeAllMarkerListenersAndCallouts() {
		if (mPrevItem == null) {
			mTileMap.removeMarkerEventListener(mDelBeaconMarkerListener);
			mTileMap.removeCallout(mCalloutDelBeacon);
			return;
		}
		switch (mPrevItem.getItemId()) {
		case R.id.action_add_beacon:
			mTileMap.setOnTouchListener(null);
			mTileMap.removeCallout(mCalloutAddBeacon);
			break;
		case R.id.action_del_beacon:
			mTileMap.removeMarkerEventListener(mDelBeaconMarkerListener);
			mTileMap.removeCallout(mCalloutDelBeacon);
			break;
		}
	}

	private MenuItem mPrevItem;

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() != R.id.menu_manage_map) {
			item.setChecked(true);
			removeAllMarkerListenersAndCallouts();
			mPrevItem = item;
			switch (item.getItemId()) {
			case R.id.action_add_beacon:
				handleActionAddNode();
				break;
			case R.id.action_del_beacon:
				handleActionDelNode();
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
		getMenuInflater().inflate(R.menu.manage_beacon, menu);
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
	public void onConfirmBeaconAdd(View callout, String uuid, int major,
			int minor, double x, double y) {
		if (uuid.equals("")) {
			m("UUID不能为空！");
			return;
		}
		// 保存节点到数据库
		BeaconDao beaconDao = mDaoSession.getBeaconDao();
		Beacon beacon = new Beacon();
		beacon.setUuid(uuid);
		beacon.setMajor(major);
		beacon.setMinor(minor);
		beacon.setX((long) (x * mMap.getScale()));
		beacon.setY((long) (y * mMap.getScale()));
		beacon.setMapid(mMap.getId());
		List<Beacon> beacons = beaconDao
				.queryBuilder()
				.whereOr(
						beaconDao.queryBuilder().and(
								BeaconDao.Properties.Mapid.eq(mMap.getId()),
								BeaconDao.Properties.X.eq(beacon.getX()),
								BeaconDao.Properties.Y.eq(beacon.getY())),
						beaconDao
								.queryBuilder()
								.and(BeaconDao.Properties.Mapid
										.eq(mMap.getId()),
										BeaconDao.Properties.Uuid.eq(beacon
												.getUuid()),
										BeaconDao.Properties.Major.eq(beacon
												.getMajor()),
										BeaconDao.Properties.Minor.eq(beacon
												.getMinor()))).list();
		if (!beacons.isEmpty()) {
			m("该信标已存在！");
			return;
		}

		beaconDao.insert(beacon);
		d("加入信标: " + uuid + "(" + major + ", " + minor + "), x, y = " + x
				+ ", " + y);
		// 添加一个mark
		ImageView marker = new ImageView(this);
		marker.setImageResource(R.drawable.map_beacon_icon);
		marker.setTag(beacon);
		mTileMap.addMarker(marker, x, y, -0.5f, -0.5f);
	}

	@Override
	public void onConfirmBeaconDel(View callout, Beacon beacon) {
		// 删除所画节点
		mTileMap.removeMarker((View) callout.getTag());

		// 删除数据库中的节点
		BeaconDao beaconDao = mDaoSession.getBeaconDao();
		beaconDao.delete(beacon);
	}

	private MarkerEventListener mDelBeaconMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			double scale = mTileMap.getScale();
			mCalloutDelBeacon.setBeacon((Beacon) view.getTag());
			mCalloutDelBeacon.setTag(view);
			mTileMap.addCallout(mCalloutDelBeacon, x / scale, y / scale);
		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			double scale = mTileMap.getScale();
			double x = (event.getX() + mTileMap.getScrollX()) / scale;
			double y = (event.getY() + mTileMap.getScrollY()) / scale;
			mTileMap.addCallout(mCalloutAddBeacon, x, y);
			mCalloutAddBeacon.setPos(x, y);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			v.performClick();
		}
		return false;
	}

}
