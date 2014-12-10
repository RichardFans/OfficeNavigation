package com.richard.officenavigation.callout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.qozix.tileview.TileView;
import com.richard.utils.Views;

public class BaseMapCallout extends RelativeLayout {
	private static final int CORNER_RADIUS = 6;
	public static final int NUB_WIDTH = 18;
	public static final int NUB_HEIGHT = 15;
	private FrameLayout mBubble;
	private Nub mNub;
	private TileView mTileMap;
	private GradientDrawable mBgTop, mBgBottom;

	public BaseMapCallout(Context context, TileView tileMap) {
		super(context);
		mTileMap = tileMap;

		mBubble = new FrameLayout(context);
		int[] colors = { 0xE6888888, 0xFF000000 };

		mBgBottom = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM, colors);
		mBgBottom.setCornerRadius(CORNER_RADIUS);
		mBgBottom.setStroke(2, 0xDD000000);

		mBgTop = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
				colors);
		mBgTop.setCornerRadius(CORNER_RADIUS);
		mBgTop.setStroke(2, 0xDD000000);

		mBubble.setBackground(mBgBottom);
		mBubble.setId(1);
		mBubble.setPadding(10, 10, 10, 10);
		addView(mBubble);

		mNub = new Nub(context);
		mNub.setId(2);
		mNub.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
		LayoutParams nubLayout = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		nubLayout.addRule(RelativeLayout.BELOW, mBubble.getId());
		nubLayout.addRule(RelativeLayout.CENTER_IN_PARENT);
		addView(mNub, nubLayout);
	}

	public void setNubGravity(int gravity) {
		if (gravity != mNub.getGravity()) {
			removeAllViews();
			LayoutParams nubLayout = new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			LayoutParams bubbleLayout = new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.LEFT:
				nubLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				break;
			case Gravity.CENTER_HORIZONTAL:
				nubLayout.addRule(RelativeLayout.CENTER_HORIZONTAL);
				break;
			case Gravity.RIGHT:
				// nubLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT); //
				// 这种方法是错误的!!
				nubLayout.addRule(RelativeLayout.ALIGN_RIGHT, mBubble.getId());
				break;
			}

			switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
			case Gravity.TOP:
				mBubble.setBackground(mBgTop);
				addView(mNub, nubLayout);
				bubbleLayout.addRule(RelativeLayout.BELOW, mNub.getId());
				addView(mBubble, bubbleLayout);
				break;
			case Gravity.BOTTOM:
				mBubble.setBackground(mBgBottom);
				addView(mBubble, bubbleLayout);
				nubLayout.addRule(RelativeLayout.BELOW, mBubble.getId());
				addView(mNub, nubLayout);
				break;
			}
			mNub.setGravity(gravity);
		}
	}

	public void dismiss() {
		mTileMap.removeCallout(this);
	}

	public void setContentView(View v) {
		mBubble.removeAllViews();
		mBubble.addView(v);
	}

	public void setContentView(View v, ViewGroup.LayoutParams params) {
		mBubble.removeAllViews();
		mBubble.addView(v, params);
	}

	public BaseMapCallout transitionIn() {
		ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				1f);
		scaleAnimation.setInterpolator(new OvershootInterpolator(1.2f));
		scaleAnimation.setDuration(250);

		AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1f);
		alphaAnimation.setDuration(200);

		AnimationSet animationSet = new AnimationSet(false);

		animationSet.addAnimation(scaleAnimation);
		animationSet.addAnimation(alphaAnimation);

		startAnimation(animationSet);
		return this;
	}

	protected int dp(int dp) {
		return Views.dip2px(getContext(), dp);
	}

	private class Nub extends View {

		private Paint mPaint = new Paint();
		private Path mPath = new Path();
		private int mGravity;

		public Nub(Context context) {
			super(context);
			mPaint.setColor(0xFF000000);
			mPaint.setAntiAlias(true);

			mGravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			mPath.lineTo(dp(NUB_WIDTH), 0);
			mPath.lineTo(10, dp(NUB_HEIGHT));
			mPath.close();
		}

		public int getGravity() {
			return mGravity;
		}

		public void setGravity(int gravity) {
			if (mGravity != gravity) {
				mPath.reset();
				switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
					handleTopGravity(gravity);
					break;
				case Gravity.BOTTOM:
					handleBottomGravity(gravity);
					break;
				}
				mPath.close();
				mGravity = gravity;
			}
		}

		private void handleBottomGravity(int gravity) {
			switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.LEFT:
				mPath.moveTo(0 + CORNER_RADIUS, 0);
				mPath.lineTo(dp(NUB_WIDTH) + CORNER_RADIUS, 0);
				mPath.lineTo(0, dp(NUB_HEIGHT));
				break;
			case Gravity.CENTER_HORIZONTAL:
				mPath.lineTo(dp(NUB_WIDTH), 0);
				mPath.lineTo(dp(NUB_WIDTH) / 2, dp(NUB_HEIGHT));
				break;
			case Gravity.RIGHT:
				mPath.lineTo(dp(NUB_WIDTH), 0);
				mPath.lineTo(dp(NUB_WIDTH) + CORNER_RADIUS, dp(NUB_HEIGHT));
				break;
			}
		}

		private void handleTopGravity(int gravity) {
			switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.LEFT:
				mPath.lineTo(dp(NUB_WIDTH) + CORNER_RADIUS, dp(NUB_HEIGHT));
				mPath.lineTo(0 + CORNER_RADIUS, dp(NUB_HEIGHT));
				break;
			case Gravity.CENTER_HORIZONTAL:
				mPath.moveTo(dp(NUB_WIDTH) / 2, 0);
				mPath.lineTo(dp(NUB_WIDTH), dp(NUB_HEIGHT));
				mPath.lineTo(0, dp(NUB_HEIGHT));
				break;
			case Gravity.RIGHT:
				mPath.moveTo(dp(NUB_WIDTH) + CORNER_RADIUS, 0);
				mPath.lineTo(dp(NUB_WIDTH), dp(NUB_HEIGHT));
				mPath.lineTo(0, dp(NUB_HEIGHT));
				break;
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = dp(NUB_WIDTH);
			if ((mGravity & Gravity.CENTER) > 0)
				width += CORNER_RADIUS;
			setMeasuredDimension(width, dp(NUB_HEIGHT));
		}

		@Override
		public void onDraw(Canvas canvas) {
			canvas.drawPath(mPath, mPaint);
			super.onDraw(canvas);
		}
	}
}
