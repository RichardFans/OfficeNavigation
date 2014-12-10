package com.richard.officenavigation.callout;

import com.qozix.tileview.TileView;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InfoCallout extends BaseMapCallout {

	private TextView mTvTitle;
	private TextView mTvText;

	public InfoCallout(Context context, TileView mapView) {
		super(context, mapView);
		LinearLayout labels = new LinearLayout(context);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		labels.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams labelLayout = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		labelLayout.setMargins(12, 0, 12, 0);
		setContentView(labels, labelLayout);

		mTvTitle = new TextView(getContext());
		mTvTitle.setTextColor(0xFFFFFFFF);
		mTvTitle.setTextSize(15);
		mTvTitle.setMaxWidth(250);
		mTvTitle.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		mTvTitle.setText("I'm the Title");
		labels.addView(mTvTitle);

		mTvText = new TextView(getContext());
		mTvText.setTextColor(0xFFFFFFFF);
		mTvText.setTextSize(12);
		mTvText.setTypeface(Typeface.SANS_SERIF);
		mTvText.setText("This is a Sub Title");
		labels.addView(mTvText);
	}

	public void setTitle(String title) {
		mTvTitle.setText(title);
	}

	public void setText(String text) {
		mTvText.setText(text);
	}
}
