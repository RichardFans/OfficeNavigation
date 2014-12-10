package com.richard.officenavigation.fragment;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.richard.officenavigation.CreateMapActivity;
import com.richard.officenavigation.ManageMapActivity;
import com.richard.officenavigation.R;
import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.adapter.BaseMapAdapter;
import com.richard.officenavigation.adapter.IMapAdapter;
import com.richard.officenavigation.dialog.DirectoryChooserDialog;
import com.richard.officenavigation.dialog.MapChooserDialog;
import com.richard.officenavigation.dialog.DirectoryChooserDialog.OnConfirmDirectoryChooseListener;
import com.richard.officenavigation.dialog.MapChooserDialog.onMapSelectedListener;
import com.richard.officenavigation.dialog.MapParamsSetterDialog;
import com.richard.officenavigation.dialog.MapParamsSetterDialog.onConfirmSettingListener;
import com.richard.officenavigation.view.DespoticTileView;

public class MapFragment extends BaseFragment implements
		OnConfirmDirectoryChooseListener, onConfirmSettingListener,
		onMapSelectedListener {
	private static final int REQ_CREATE_MAP = 1;
	private static final int REQ_MANAGE_MAP = 2;
	private DespoticTileView mTileMap;
	private DirectoryChooserDialog mDlgChooseMapDir;
	private MapParamsSetterDialog mDlgMapParamsSetter;
	private MapChooserDialog mDlgChooseMap;

	private Bundle mExtraData;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
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
				getResources()
				.getString(R.string.title_selected_map), this);
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
		}
		return super.onOptionsItemSelected(item);
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
			handleResultForCreateMap(resultCode, data);
			mDlgChooseMap.notifyMapsChanged();
			break;
		}

	}

	private void handleResultForCreateMap(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Long id = data.getLongExtra(C.map.KEY_CURRENT_MAPID,
					C.map.DEFAULT_MAP_ID);
			mTileMap.setAdapter(new IMapAdapter(getActivity(), id));
		}
	}
}
