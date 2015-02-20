package com.richard.officenavigation.callout;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;
import com.richard.officenavigation.dao.INode;

public class TrainNodeCallout extends BaseMapCallout {
	private TextView mTvNodeName, mTvNodePos;
	private Button mBtnTrain, mBtnViewResult;
	private INode mNode;

	private onConfirmNodeTrainListener mListener;

	public TrainNodeCallout(Context context, TileView mapView) {
		super(context, mapView);
		View v = View.inflate(getContext(), R.layout.callout_train_node, null);
		setContentView(v);
		mTvNodeName = (TextView) v.findViewById(R.id.tv_name);
		mTvNodePos = (TextView) v.findViewById(R.id.tv_position);
		mBtnTrain = (Button) v.findViewById(R.id.btn_training);
		mBtnViewResult = (Button) v.findViewById(R.id.btn_view_result);

		mBtnTrain.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mListener != null) {
					TrainNodeCallout.this.mListener.onConfirmNodeTrain(
							TrainNodeCallout.this, mNode);
				}
				dismiss();
			}
		});

		mBtnViewResult.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mListener != null) {
					TrainNodeCallout.this.mListener.onConfirmResultView(
							TrainNodeCallout.this, mNode);
				}
				dismiss();
			}
		});
	}

	public void setNode(INode node) {
		mNode = node;
		String html = node.getName()
				+ " <font color="
				+ (node.isTrained() ? "'green'>"
						+ getResources().getString(R.string.trained) : "'red'>"
						+ getResources().getString(R.string.not_trained))
				+ "</font>";
		mTvNodeName.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
		mTvNodePos.setText(node.getX() + ", " + node.getY());
		if (node.isTrained()) {
			mBtnTrain.setText(getResources().getString(R.string.btn_retrain));
			mBtnViewResult.setVisibility(View.VISIBLE);
		} else {
			mBtnTrain.setText(getResources().getString(R.string.btn_train));
			mBtnViewResult.setVisibility(View.GONE);
		}
	}

	public void setOnConfirmNodeTrainListener(
			onConfirmNodeTrainListener listener) {
		mListener = listener;
	}

	public static interface onConfirmNodeTrainListener {
		/**
		 * 
		 * @param callout
		 *            显示弹窗
		 * @param node
		 *            要训练的node
		 */
		public void onConfirmNodeTrain(View callout, INode node);

		/**
		 * 
		 * @param callout
		 *            显示弹窗
		 * @param node
		 *            要查看训练结果的node
		 */
		public void onConfirmResultView(View callout, INode node);
	}
}
