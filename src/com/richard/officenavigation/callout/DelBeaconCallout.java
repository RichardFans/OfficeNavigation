package com.richard.officenavigation.callout;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;
import com.richard.officenavigation.dao.Beacon;

public class DelBeaconCallout extends BaseMapCallout {
	private TextView mTvBeaconUuid, mTvBeaconPos, mTvBeaconMajorMinor;
	private onConfirmBeaconDelListener mListener;
	private Beacon mBeacon;

	public DelBeaconCallout(Context context, TileView mapView) {
		super(context, mapView);
		LinearLayout layout = new LinearLayout(context);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams labelLayout = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		labelLayout.setMargins(12, 0, 12, 0);
		setContentView(layout, labelLayout);

		TextView titleView = new TextView(getContext());
		titleView.setTextColor(0xFFFFFFFF);
		titleView.setTextSize(16);
		titleView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		titleView.setText(R.string.title_beacon_info);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 8);
		layout.addView(titleView, params);

		mTvBeaconUuid = new TextView(getContext());
		mTvBeaconUuid.setTextColor(0xFFFFFFFF);
		mTvBeaconUuid.setTextSize(12);
		mTvBeaconUuid.setText("UUID：");
		layout.addView(mTvBeaconUuid);

		mTvBeaconMajorMinor = new TextView(getContext());
		mTvBeaconMajorMinor.setTextColor(0xFFFFFFFF);
		mTvBeaconMajorMinor.setTextSize(12);
		mTvBeaconMajorMinor.setText("主次编号：");
		layout.addView(mTvBeaconMajorMinor, params);

		mTvBeaconPos = new TextView(getContext());
		mTvBeaconPos.setTextColor(0xFFFFFFFF);
		mTvBeaconPos.setTextSize(12);
		mTvBeaconUuid.setText("位置：");
		layout.addView(mTvBeaconPos, params);

		Button confirmBtn = new Button(getContext());
		confirmBtn.setBackground(getContext().getResources().getDrawable(
				R.drawable.btn_possitive));
		confirmBtn.setTextSize(16);
		confirmBtn.setText(R.string.delete);
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.width = dp(108);
		params.height = dp(36);
		layout.addView(confirmBtn, params);

		confirmBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mListener != null) {
					DelBeaconCallout.this.mListener.onConfirmBeaconDel(
							DelBeaconCallout.this, mBeacon);
				}
				dismiss();
			}
		});
	}

	public void setOnConfirmBeaconDelListener(
			onConfirmBeaconDelListener listener) {
		mListener = listener;
	}

	public static interface onConfirmBeaconDelListener {
		/**
		 * 
		 * @param callout
		 *            显示弹窗
		 * @param name
		 *            添加node的名称
		 * @param x
		 *            添加node的x坐标（相对值）
		 * @param y
		 *            添加node的y坐标（相对值）
		 */
		public void onConfirmBeaconDel(View callout, Beacon beacon);
	}

	public void setBeacon(Beacon beacon) {
		mBeacon = beacon;
		mTvBeaconUuid.setText("UUID：" + beacon.getUuid());
		mTvBeaconPos.setText("位置：" + beacon.getX() + ", " + beacon.getY());
		mTvBeaconMajorMinor.setText("主次编号："
				+ Integer.toHexString(beacon.getMajor()) + ", "
				+ Integer.toHexString(beacon.getMinor()));
	}
}
