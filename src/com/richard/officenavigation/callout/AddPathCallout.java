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
import com.richard.officenavigation.dao.INode;
import com.richard.utils.Views;

public class AddPathCallout extends BaseMapCallout {
	private onConfirmPathAddListener mListener;
	private INode mNodeFrom, mNodeTo;
	private boolean mIsStageFrom;
	private TextView mTitleView;
	private Button mBtnCancel;

	public AddPathCallout(Context context, TileView mapView) {
		super(context, mapView);
		mIsStageFrom = true;
		LinearLayout layout = new LinearLayout(context);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams labelLayout = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		labelLayout.setMargins(12, 0, 12, 0);
		setContentView(layout, labelLayout);

		mTitleView = new TextView(getContext());
		mTitleView.setTextColor(0xFFFFFFFF);
		mTitleView.setTextSize(16);
		mTitleView.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		mTitleView.setText(R.string.title_path_from);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 8);
		layout.addView(mTitleView, params);

		Button btnConfirm = new Button(getContext());
		btnConfirm.setBackground(getContext().getResources().getDrawable(
				R.drawable.btn_possitive));
		btnConfirm.setTextSize(16);
		btnConfirm.setText(R.string.confirm);
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.width = dp(108);
		params.height = dp(36);
		params.setMargins(0, 0, 0, 8);
		layout.addView(btnConfirm, params);

		mBtnCancel = new Button(getContext());
		mBtnCancel.setVisibility(View.GONE);
		mBtnCancel.setBackground(getContext().getResources().getDrawable(
				R.drawable.btn_possitive));
		mBtnCancel.setTextSize(16);
		mBtnCancel.setText(R.string.cancel);
		layout.addView(mBtnCancel, params);

		btnConfirm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mIsStageFrom) {
					setPathFromStage(false);
				} else {
					if (mListener != null)
						AddPathCallout.this.mListener.onConfirmPathAdd(
								AddPathCallout.this, mNodeFrom, mNodeTo);
					setPathFromStage(true);
				}
				dismiss();
			}
		});

		mBtnCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setPathFromStage(true);
				dismiss();
			}
		});
	}

	public void setOnConfirmPathAddListener(onConfirmPathAddListener listener) {
		mListener = listener;
	}

	public static interface onConfirmPathAddListener {
		public void onConfirmPathAdd(View callout, INode from, INode to);
	}

	public void setFrom(INode node) {
		mNodeFrom = node;
	}

	public INode getFrom() {
		return mNodeFrom;
	}

	public void setTo(INode node) {
		mNodeTo = node;
	}

	public INode getTo() {
		return mNodeTo;
	}

	public void setPathFromStage(boolean isStageFrom) {
		mIsStageFrom = isStageFrom;
		if (isStageFrom) {
			mTitleView.setText(R.string.title_path_from);
			mBtnCancel.setVisibility(View.GONE);
		} else {
			mTitleView.setText(R.string.title_path_to);
			mBtnCancel.setVisibility(View.VISIBLE);
		}
	}

	public boolean IsPathFromStage() {
		return mIsStageFrom;
	}
}
