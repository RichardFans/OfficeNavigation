package com.richard.officenavigation.callout;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;

public class AddNodeCallout extends BaseMapCallout {
	private EditText mEditNodeName;
	private onConfirmNodeAddListener mListener;
	private double x, y;
	
	public AddNodeCallout(Context context, TileView mapView) {
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
		titleView.setText(R.string.title_input_node_name);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 8);
		layout.addView(titleView, params);

		mEditNodeName = new EditText(getContext());
		mEditNodeName.setBackgroundColor(0xFFCCCCCC);
		mEditNodeName.setTextSize(15);
		mEditNodeName.setPadding(dp(3), 0, 0, 0);
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.width = dp(108);
		params.height = dp(36);
		params.setMargins(0, 0, 0, 8);
		layout.addView(mEditNodeName, params);

		Button confirmBtn = new Button(getContext());
		confirmBtn.setBackground(getContext().getResources().getDrawable(
				R.drawable.btn_possitive));
		confirmBtn.setTextSize(16);
		confirmBtn.setText(R.string.confirm);
		layout.addView(confirmBtn, params);

		confirmBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mListener != null) {
					AddNodeCallout.this.mListener.onConfirmNodeAdd(
							AddNodeCallout.this, mEditNodeName.getText()
									.toString(), x, y);
				}
				dismiss();
			}
		});
	}

	public void setOnConfirmNodeAddListener(onConfirmNodeAddListener listener) {
		mListener = listener;
	}

	public static interface onConfirmNodeAddListener {
		/**
		 * 
		 * @param callout 显示弹窗
		 * @param name 添加node的名称
		 * @param x 添加node的x坐标（相对值）
		 * @param y 添加node的y坐标（相对值）
		 */
		public void onConfirmNodeAdd(View callout, String name, double x, double y);
	}

	public void setPos(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void clearName() {
		mEditNodeName.setText("");
	}
}
