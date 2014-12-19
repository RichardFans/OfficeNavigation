package com.richard.officenavigation.view;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.qozix.layouts.StaticLayout;
import com.qozix.tileview.detail.DetailManager;

public class CircleManager extends StaticLayout {

	private static final int DEFAULT_STROKE_COLOR = 0x8899FF33;
	private static final int DEFAULT_STROKE_WIDTH = 8;

	private boolean shouldDraw = true;

	private Paint defaultPaint = new Paint();
	{
		defaultPaint.setStyle(Paint.Style.STROKE);
		defaultPaint.setColor(DEFAULT_STROKE_COLOR);
		defaultPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
		defaultPaint.setAntiAlias(true);
	}

	private DetailManager detailManager;

	private ArrayList<DrawableCircle> circles = new ArrayList<DrawableCircle>();

	public CircleManager(Context context, DetailManager dm) {
		super(context);
		setWillNotDraw(false);
		detailManager = dm;
	}

	public Paint getPaint() {
		return defaultPaint;
	}

	public DrawableCircle addCircle(double cx, double cy, double radius) {
		return addCircle(cx, cy, radius, defaultPaint);
	}

	public DrawableCircle addCircle(double cx, double cy, double radius,
			Paint paint) {
		DrawableCircle drawableCircle = new DrawableCircle();
		drawableCircle.cx = cx;
		drawableCircle.cy = cy;
		drawableCircle.radius = radius;
		drawableCircle.paint = paint;
		return addCircle(drawableCircle);
	}

	public DrawableCircle addCircle(DrawableCircle drawableCircle) {
		circles.add(drawableCircle);
		invalidate();
		return drawableCircle;
	}

	public void removeCircle(DrawableCircle path) {
		circles.remove(path);
		invalidate();
	}

	public void clear() {
		circles.clear();
		invalidate();
	}

	public void setShouldDraw(boolean should) {
		shouldDraw = should;
		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (shouldDraw) {
			float drawingCx, drawingCy, drawingRadius;
			float scale = (float) detailManager.getScale();
			for (DrawableCircle drawableCircle : circles) {
				drawingCx = (float) (drawableCircle.cx * scale);
				drawingCy = (float) (drawableCircle.cy * scale);
				drawingRadius = (float) (drawableCircle.radius * scale);
				drawableCircle
						.draw(canvas, drawingCx, drawingCy, drawingRadius);
			}
		}
		super.onDraw(canvas);
	}

	public class DrawableCircle {

		/**
		 * The circle that this drawable will follow.
		 */
		public double cx, cy, radius;

		/**
		 * The paint to be used for this circle.
		 */
		public Paint paint;

		/**
		 * Draw the supplied circle onto the supplied canvas.
		 */
		@SuppressLint("NewApi")
		public void draw(Canvas canvas, float drawingCx, float drawingCy,
				float drawingRadius) {
			canvas.drawCircle(drawingCx, drawingCy, drawingRadius, paint);
		}

	}
}
