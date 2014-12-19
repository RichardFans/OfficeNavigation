package com.richard.officenavigation.view;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.qozix.tileview.TileView;
import com.qozix.tileview.detail.DetailManager;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.graphics.BitmapDecoderFile;
import com.qozix.tileview.markers.MarkerEventListener;
import com.richard.officenavigation.adapter.BaseMapAdapter;
import com.richard.officenavigation.callout.BaseMapCallout;
import com.richard.officenavigation.view.CircleManager.DrawableCircle;
import com.richard.utils.Views;

public class MapTileView extends TileView {
	private static final int DEFAULT_CALLOUT_WIDTH = 128;
	private static final int DEFAULT_CALLOUT_HEIGHT = 118;
	private BaseMapAdapter mAdapter;

	private CircleManager circleManager;
	private onNodeClickListener mNodeClickListener;
	private MarkerEventListener mNodeMarkerListener = new MarkerEventListener() {

		@Override
		public void onMarkerTap(View view, int x, int y) {
			if (mNodeClickListener != null) {
				mNodeClickListener.onNodeClick(view.getTag(), x, y);
			}
		}
	};

	public MapTileView(Context context, AttributeSet attrs) {
		this(context);
	}

	public MapTileView(Context context) {
		super(context);
		Field detailManager;
		try {
			detailManager = TileView.class.getDeclaredField("detailManager");
			detailManager.setAccessible(true);
			circleManager = new CircleManager(context,
					(DetailManager) detailManager.get(this));
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		addView(circleManager, 3);
	}

	public void setAdapter(BaseMapAdapter adaper) {
		mAdapter = adaper;
	}

	public BaseMapAdapter getAdapter() {
		return mAdapter;
	}

	/**
	 * Register a Path and Paint that will be drawn on a layer above the tiles,
	 * but below markers. This Path's will be scaled with the TileView, but will
	 * always be as wide as the stroke set for the Paint.
	 * 
	 * @param cx
	 *            X-coordinate of the circle center.
	 * @param cy
	 *            Y-coordinate of the circle center.
	 * @param radius
	 *            Radius of the circle.
	 * @param paint
	 *            (Paint) the Paint instance that defines the style of the drawn
	 *            circle.
	 */
	public DrawableCircle drawCircle(double cx, double cy, double radius, Paint paint) {
		Point c = translate(cx, cy);
		return circleManager.addCircle(c.x, c.y, radius, paint);
	}
	
	public DrawableCircle drawCircle(double cx, double cy, double radius) {
		Point c = translate(cx, cy);
		return circleManager.addCircle(c.x, c.y, radius);
	}
	
	public DrawableCircle drawCircle(DrawableCircle drawableCircle, Paint paint) {
		return circleManager.addCircle(drawableCircle);
	}

	public void removeCircle( DrawableCircle drawableCircle ) {
		circleManager.removeCircle( drawableCircle );
	}
	
	public void setupMapDefault(boolean drawNodes) {
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
		refresh();
		setScale(0);

		if (drawNodes)
			setupNodeViews();

		moveToAndCenter(width / 2, height / 2);
	}

	private void setupNodeViews() {
		List<View> views = mAdapter.getViews();
		for (View view : views) {
			PointF pos = mAdapter.getNodePos(view.getTag());
			addMarker(view, pos.x, pos.y, -0.1f, -0.5f);
		}
		addMarkerEventListener(mNodeMarkerListener);
	}

	public void clearNodeViews() {
		List<View> views = mAdapter.getViews();
		for (View view : views) {
			removeMarker(view);
		}
		removeMarkerEventListener(mNodeMarkerListener);
	}

	public BaseMapCallout addCallout(BaseMapCallout callout, double x, double y) {
		int gravity = 0;
		float anchorX, anchorY;
		Point size = new Point();
		Point pos = translate(x, y);
		pos.x = (int) (pos.x * getScale() - getScrollX());
		pos.y = (int) (pos.y * getScale() - getScrollY());
		size.x = callout.getWidth() == 0 ? Views.dip2px(getContext(),
				DEFAULT_CALLOUT_WIDTH) : callout.getWidth();
		size.y = callout.getHeight() == 0 ? Views.dip2px(getContext(),
				DEFAULT_CALLOUT_HEIGHT) : callout.getHeight();
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
		callout.setNubGravity(gravity);
		addCallout(callout, x, y, anchorX, anchorY);
		return callout.transitionIn();
	}

	public void frameTo(final double x, final double y) {
		post(new Runnable() {
			@Override
			public void run() {
				MapTileView.this.moveToAndCenter(x, y);
			}
		});
	}

	public void setOnNodeClickListener(onNodeClickListener listener) {
		mNodeClickListener = listener;
	}

	public interface onNodeClickListener {
		void onNodeClick(Object node, int x, int y);
	}
}
