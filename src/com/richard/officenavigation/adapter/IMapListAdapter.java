package com.richard.officenavigation.adapter;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.dao.IMap;

public class IMapListAdapter extends BaseAdapter {
	private Context mCtx;
	private int mIdTitle, mIdSample, mItemLayout;
	private List<IMap> mMaps;

	public IMapListAdapter(Context ctx, List<IMap> maps, int itemLayout,
			int idTitle, int idSample) {
		mCtx = ctx;
		mIdTitle = idTitle;
		mIdSample = idSample;
		mItemLayout = itemLayout;
		mMaps = maps;
	}

	public void changeMaps(List<IMap> maps) {
		mMaps = maps;
	}

	@Override
	public int getCount() {
		return mMaps.size();
	}

	@Override
	public Object getItem(int position) {
		return mMaps.get(position);
	}

	@Override
	public long getItemId(int position) {
		return mMaps.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = View.inflate(mCtx, mItemLayout, null);
			TextView title = (TextView) convertView.findViewById(mIdTitle);
			ImageView image = (ImageView) convertView.findViewById(mIdSample);
			IMap map = mMaps.get(position);
			title.setText(map.getName());
			title.setTextColor(0xFFDDDDDD);
			Bitmap bm = null;
			if (map.getId() == C.map.DEFAULT_MAP_ID) {
				AssetManager assets = mCtx.getAssets();
				try {
					InputStream input = assets.open(map.getSrc()
							+ File.separator + "sample.jpg");
					if (input != null) {
						bm = BitmapFactory.decodeStream(input);
					}
				} catch (Exception e) {
				}
			} else {
				bm = BitmapFactory.decodeFile(map.getSrc() + File.separator
						+ "sample.jpg");
			}
			image.setImageBitmap(bm);
		}
		return convertView;
	}
}