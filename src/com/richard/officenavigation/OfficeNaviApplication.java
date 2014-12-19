package com.richard.officenavigation;

import java.util.Collection;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import android.app.Application;
import android.os.RemoteException;
import android.util.Log;

public class OfficeNaviApplication extends Application implements
		BootstrapNotifier, RangeNotifier {
	private static final String TAG = "OfficeNaviApplication";
	private BeaconManager mBeaconManager;
	private Region mAllBeaconsRegion;
	private BackgroundPowerSaver mBackgroundPowerSaver;
	private RegionBootstrap mRegionBootstrap;
	private onRangeBeaconsInRegionListener mListener;

	@Override
	public void onCreate() {
		mAllBeaconsRegion = new Region("all beacons", null, null, null);

		mBeaconManager = BeaconManager.getInstanceForApplication(this);
		mBackgroundPowerSaver = new BackgroundPowerSaver(this);
		mRegionBootstrap = new RegionBootstrap(this, mAllBeaconsRegion);
		/* ibeacon layout */
		mBeaconManager
				.getBeaconParsers()
				.add(new BeaconParser()
						.setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d(TAG, "I just saw a beacon named " + region.getUniqueId()
				+ " for the first time!");
		try {
			Log.d(TAG, "entered region.  starting ranging");
			mBeaconManager.startRangingBeaconsInRegion(mAllBeaconsRegion);
			mBeaconManager.setRangeNotifier(this);
		} catch (RemoteException e) {
			Log.e(TAG, "Cannot start ranging");
		}
	}

	@Override
	public void didExitRegion(Region region) {
		Log.d(TAG, "I no longer see a beacon named " + region.getUniqueId());
	}

	@Override
	public void didDetermineStateForRegion(int state, Region region) {

	}

	@Override
	public void didRangeBeaconsInRegion(Collection<Beacon> beacons,
			Region region) {
		if (mListener != null) {
			mListener.onRangeBeaconsInRegion(beacons, region);
		}
	}
	
	public void setOnRangeBeaconsInRegionListener(onRangeBeaconsInRegionListener listener) {
		mListener = listener;
	}

	public interface onRangeBeaconsInRegionListener {
		void onRangeBeaconsInRegion(Collection<Beacon> beacons, Region region);
	}
}
