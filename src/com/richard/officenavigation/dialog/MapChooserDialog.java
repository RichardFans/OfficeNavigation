package com.richard.officenavigation.dialog;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.richard.officenavigation.R;
import com.richard.officenavigation.adapter.IMapListAdapter;
import com.richard.officenavigation.dao.IMap;
import com.richard.officenavigation.dao.IMapDao;
import com.richard.officenavigation.dao.SingletonDaoSession;

public class MapChooserDialog extends Dialog implements OnItemClickListener {
	private String mTitle;
	private onMapSelectedListener mListener;
	private Context mContext;
	private IMapListAdapter mAdapter;

	public MapChooserDialog(Context context) {
		super(context);
		mContext = context;
	}

	public static MapChooserDialog newInstance(Context context, String title,
			onMapSelectedListener listener) {
		MapChooserDialog d = new MapChooserDialog(context);
		d.mTitle = title;
		d.mListener = listener;
		return d;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View v = View.inflate(getContext(), R.layout.dialog_map_chooser, null);
		setContentView(v);
		((TextView) v.findViewById(R.id.title)).setText(mTitle);
		GridView mapsView = (GridView) v.findViewById(R.id.gv_maps);
		IMapDao mapDao = SingletonDaoSession.getInstance(mContext).getIMapDao();
		List<IMap> maps = mapDao.queryBuilder().list();
		mAdapter = new IMapListAdapter(mContext, maps,
				R.layout.simple_gradview_item, R.id.tv_name, R.id.iv_pic);
		mAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				List<IMap> maps = SingletonDaoSession.getInstance(mContext)
						.getIMapDao().queryBuilder().list();
				mAdapter.changeMaps(maps);
			}
		});
		mapsView.setAdapter(mAdapter);
		mapsView.setOnItemClickListener(this);
	}

	public void notifyMapsChanged() {
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (((ContextThemeWrapper) mContext).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Point outSize = new Point();
			((Activity) mContext).getWindowManager().getDefaultDisplay()
					.getSize(outSize);
			getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					outSize.y / 2);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mListener != null) {
			mListener.onMapSelected(id);
		}
		dismiss();
	}

	public interface onMapSelectedListener {
		void onMapSelected(Long id);
	};
}
