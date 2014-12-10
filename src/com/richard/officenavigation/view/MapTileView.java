package com.richard.officenavigation.view;

import java.io.File;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;

import com.qozix.tileview.TileView;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderFile;
import com.richard.officenavigation.adapter.BaseMapAdapter;
import com.richard.officenavigation.callout.BaseMapCallout;
import com.richard.utils.Views;

public class MapTileView extends TileView {
	private static final int DEFAULT_CALLOUT_WIDTH = 128;
	private static final int DEFAULT_CALLOUT_HEIGHT = 118;
	private BaseMapAdapter mAdapter;
	private BaseMapCallout mCallout;
	private double mCalloutX, mCalloutY;

	public MapTileView(Context context, AttributeSet attrs) {
		this(context);
	}

	public MapTileView(Context context) {
		super(context);
	}

	public void setAdapter(BaseMapAdapter adaper) {
		mAdapter = adaper;
		setupMapDefault();
	}

	public BaseMapAdapter getAdapter() {
		return mAdapter;
	}

	private void setupMapDefault() {
		String path = mAdapter.getSrc();
		int width = mAdapter.getWidth().intValue();
		int height = mAdapter.getHeight().intValue();
		int type = mAdapter.getType();

		setSize(width, height);
		setScaleLimits(0, 2);
		if (type == BaseMapAdapter.FS_MAP) {
			BitmapDecoder decoder = new BitmapDecoderFile();
			setTileDecoder(decoder);
			setDownsampleDecoder(decoder);
		}
		resetDetailLevels();
		addDetailLevel(1.000f, path + File.separator + "1000/%col%_%row%.jpg",
				path + File.separator + "sample.jpg");
		addDetailLevel(0.500f, path + File.separator + "500/%col%_%row%.jpg",
				path + File.separator + "sample.jpg");
		addDetailLevel(0.250f, path + File.separator + "250/%col%_%row%.jpg",
				path + File.separator + "sample.jpg");
		addDetailLevel(0.125f, path + File.separator + "125/%col%_%row%.jpg",
				path + File.separator + "sample.jpg");

		setScale(0);
		frameTo(width / 2, height / 2);
	}

	public BaseMapCallout addCallout(BaseMapCallout callout, double x, double y) {
		mCallout = callout;
		mCalloutX = x;
		mCalloutY = y;
		return addCallout().transitionIn();
	}

	// 368, 340
	private BaseMapCallout addCallout() {
		int gravity = 0;
		float anchorX, anchorY;
		Point size = new Point();
		Point pos = translate(mCalloutX, mCalloutY);
		pos.x = (int) (pos.x * getScale() - getScrollX());
		pos.y = (int) (pos.y * getScale() - getScrollY());
		size.x = mCallout.getWidth() == 0 ? Views.dip2px(getContext(),
				DEFAULT_CALLOUT_WIDTH) : mCallout.getWidth();
		size.y = mCallout.getHeight() == 0 ? Views.dip2px(getContext(),
				DEFAULT_CALLOUT_HEIGHT) : mCallout.getHeight();
//		Log.d("mytag", "pos x, y = " + pos.x + ", " + pos.y + "; mapwidth = "
//				+ getWidth());
//		Log.d("mytag", "callout w, h = " + mCallout.getWidth() + ", "
//				+ mCallout.getHeight());

		if (pos.x - size.x / 2 < 0) {
			gravity |= Gravity.LEFT;
			anchorX = -0.0f;
		} else if (pos.x + size.x / 2 > this.getWidth()) {
			gravity |= Gravity.RIGHT;
			anchorX = -1.0f;
		} else {
			gravity |= Gravity.CENTER_HORIZONTAL;
			anchorX = -0.5f;
		}
		if (pos.y - size.y < 0) {
			gravity |= Gravity.TOP;
			anchorY = 0.0f;
		} else {
			gravity |= Gravity.BOTTOM;
			anchorY = -1.0f;
		}
		mCallout.setNubGravity(gravity);
		return (BaseMapCallout) addCallout(mCallout, mCalloutX, mCalloutY,
				anchorX, anchorY);
	}

	public void frameTo(final double x, final double y) {
		post(new Runnable() {
			@Override
			public void run() {
				MapTileView.this.moveToAndCenter(x, y);
			}
		});
	}
}
