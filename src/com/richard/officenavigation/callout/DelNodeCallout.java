package com.richard.officenavigation.callout;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;
import com.richard.officenavigation.dao.INode;

public class DelNodeCallout extends BaseMapCallout {
	private TextView mTvNodeName, mTvNodePos;
	private onConfirmNodeDelListener mListener;
	private INode mNode;

	public DelNodeCallout(Context context, TileView mapView) {
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
		titleView.setText(R.string.title_node_info);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 8);
		layout.addView(titleView, params);

		mTvNodeName = new TextView(getContext());
		mTvNodeName.setTextColor(0xFFFFFFFF);
		mTvNodeName.setTextSize(12);
		mTvNodeName.setText(getResources().getString(R.string.subtitle_name));
		layout.addView(mTvNodeName);

		mTvNodePos = new TextView(getContext());
		mTvNodePos.setTextColor(0xFFFFFFFF);
		mTvNodePos.setTextSize(12);
		mTvNodeName.setText(getResources()
				.getString(R.string.subtitle_position));
		layout.addView(mTvNodePos, params);

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
					DelNodeCallout.this.mListener.onConfirmNodeDel(
							DelNodeCallout.this, mNode);
				}
				dismiss();
			}
		});
	}

	public void setOnConfirmNodeDelListener(onConfirmNodeDelListener listener) {
		mListener = listener;
	}

	public static interface onConfirmNodeDelListener {
		/**
		 * 
		 * @param callout
		 *            ÏÔÊ¾µ¯´°
		 * @param node
		 *            ÒªÉ¾³ýµÄnode
		 */
		public void onConfirmNodeDel(View callout, INode node);
	}

	public void setNode(INode node) {
		mNode = node;
		String html = getResources().getString(R.string.subtitle_name)
				+ node.getName()
				+ " <font color="
				+ (node.isVisible() ? "'green'>"
						+ getResources().getString(R.string.visible) : "'red'>"
						+ getResources().getString(R.string.invisible))
				+ "</font>";
		mTvNodeName.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
		mTvNodePos.setText(getResources().getString(R.string.subtitle_position)
				+ node.getX() + ", " + node.getY());
	}
}
