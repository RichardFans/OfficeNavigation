package com.richard.officenavigation.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DespoticTileView extends MapTileView {

	public DespoticTileView(Context context, AttributeSet attrs) {
		super(context);
	}

	public DespoticTileView(Context context) {
		super(context);
	}

	private boolean mNeedMoreTest, mOnLeftEdge, mOnRightEdge;
	private float mStartX;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (needDespotic()) {
				mNeedMoreTest = false;
			} else {
				mNeedMoreTest = true;
				mStartX = event.getX();
			}
			getParent().requestDisallowInterceptTouchEvent(true);
			break;
		case MotionEvent.ACTION_MOVE:
			if (mNeedMoreTest
					&& Math.abs(event.getX() - mStartX) > getWidth() / 8) {
				if (!needDespoticMoreTest(event.getX() - mStartX < 0)) {
					getParent().requestDisallowInterceptTouchEvent(false);
				}
				mNeedMoreTest = false;
			}
			break;
		case MotionEvent.ACTION_UP:
			getParent().requestDisallowInterceptTouchEvent(false);
			break;
		}
		return super.onTouchEvent(event);
	}

	private boolean needDespoticMoreTest(boolean toLeft) {
		boolean need = false;
		if ((mOnLeftEdge && toLeft) || (mOnRightEdge && !toLeft)) {
			need = true;
		}
		return need;
	}

	private boolean needDespotic() {
		boolean need = false;
		int l = getScrollX();
		int w = getScaledWidth();
		int vw = getWidth();
		mOnRightEdge = l == (w - vw);
		mOnLeftEdge = l == 0;
		if (!mOnLeftEdge && !mOnRightEdge) {
			need = true;
		}
		return need;
	}
}
