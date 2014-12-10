package com.richard.officenavigation.adapter;

import java.util.List;

import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.IMapDao;
import com.richard.officenavigation.dao.SingletonDaoSession;

import android.content.Context;
import android.util.Log;

public class IMapAdapter implements BaseMapAdapter {

	private IMap mMap;
	private Context mCtx;

	public IMapAdapter(Context ctx, Long mapId) {
		mCtx = ctx;
		mMap = getMap(mapId);
		Log.d("mytag", "mapid = " + mMap.getId());
		Log.d("mytag", "map.src = " + mMap.getSrc());
	}

	public IMapAdapter(Context ctx, IMap map) {
		mCtx = ctx;
		mMap = map;
		if (mMap.getId() == null) {
			mMap.setId(C.map.DEFAULT_MAP_ID);
		}
	}

	private IMap getMap(Long id) {
		IMap map = null;
		if (!id.equals(C.map.DEFAULT_MAP_ID)) {
			IMapDao mapDao = SingletonDaoSession.getInstance(mCtx).getIMapDao();
			List<IMap> maps = mapDao.queryBuilder()
					.where(IMapDao.Properties.Id.eq(id)).list();
			if (!maps.isEmpty()) {
				map = maps.get(0);
			}
		}
		if (map == null) {
			map = new IMap();
			map.setId(C.map.DEFAULT_MAP_ID);
			map.setSrc(C.map.DEFAULT_MAP_SRC);
			map.setName(C.map.DEFAULT_MAP_NAME);
			map.setWidth(C.map.DEFAULT_MAP_WIDTH);
			map.setHeight(C.map.DEFAULT_MAP_HEIGHT);
		}
		return map;
	}

	public IMap getIMap() {
		return mMap;
	}

	@Override
	public int getType() {
		return (mMap.getId() == C.map.DEFAULT_MAP_ID && !mMap.getSrc()
				.startsWith("/")) ? ASSETS_MAP : FS_MAP;
	}

	@Override
	public String getSrc() {
		return mMap.getSrc();
	}

	@Override
	public Long getHeight() {
		return mMap.getHeight();
	}

	@Override
	public Long getWidth() {
		return mMap.getWidth();
	}

	@Override
	public Long getId() {
		return mMap.getId();
	}
}
