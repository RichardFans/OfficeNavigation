package com.richard.officenavigation.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.widget.TextView;

import com.richard.officenavigation.R;
import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.IMapDao;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.SingletonDaoSession;

public class IMapAdapter implements BaseMapAdapter {

	private IMap mMap;
	private Context mCtx;
	private List<View> mINodeViews;

	public IMapAdapter(Context ctx, Long mapId) {
		mCtx = ctx;
		mMap = getMap(mapId);
		mINodeViews = new ArrayList<>();
	}

	public IMapAdapter(Context ctx, IMap map) {
		mCtx = ctx;
		mMap = map;
		mINodeViews = new ArrayList<>();
		if (mMap.getId() == null) {
			mMap.setId(C.map.DEFAULT_MAP_ID);
		}
	}

	private IMap getMap(Long id) {
		IMap map = null;
		IMapDao mapDao = SingletonDaoSession.getInstance(mCtx).getIMapDao();
		List<IMap> maps = mapDao.queryBuilder()
				.where(IMapDao.Properties.Id.eq(id)).list();
		if (!maps.isEmpty()) {
			map = maps.get(0);
		}
		if (map == null) {
			map = new IMap();
			map.setId(C.map.DEFAULT_MAP_ID);
			map.setSrc(C.map.DEFAULT_MAP_SRC);
			map.setName(C.map.DEFAULT_MAP_NAME);
			map.setWidth(C.map.DEFAULT_MAP_WIDTH);
			map.setHeight(C.map.DEFAULT_MAP_HEIGHT);
			map.setScale(C.map.DEFAULT_MAP_SCALE);
			mapDao.insert(map);
			mapDao.refresh(map);
			map.getNodes();
		}
		return map;
	}

	public IMap getIMap() {
		return mMap;
	}

	public List<INode> getINodes() {
		return mMap.getNodes();
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

	@Override
	public List<View> getViews() {
		if (mINodeViews.isEmpty() && getType() != ASSETS_MAP) {
			List<INode> nodes = mMap.getNodes();
			for (INode node : nodes) {
				if (node.getVisible() == true) {
					TextView tv = new TextView(mCtx);
					tv.setTextSize(9);
					tv.setTextColor(mCtx.getResources().getColor(
							R.color.bright_blue));
					tv.setText(node.getName());
					tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
							R.drawable.visible_node, 0, 0, 0);

					tv.setTag(node);
					mINodeViews.add(tv);
				}
			}
		}
		return mINodeViews;
	}

	@Override
	public PointF getNodePos(Object node) {
		INode inode = (INode) node;
		return new PointF((float) (inode.getX() / mMap.getScale()),
				(float) (inode.getY() / mMap.getScale()));
	}

	@Override
	public double getScale() {
		return mMap.getScale();
	}
}
