package com.richard.officenavigation.callout;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.qozix.tileview.TileView;
import com.richard.officenavigation.R;

public class AddBeaconCallout extends BaseMapCallout {
	private EditText[] mEditUuidParts = new EditText[5];
	private EditText mEditMajor, mEditMinor;
	private onConfirmBeaconAddListener mListener;
	private double x, y;

	public AddBeaconCallout(Context context, TileView mapView) {
		super(context, mapView);
		View v = View.inflate(getContext(), R.layout.callout_add_beacon, null);
		setContentView(v);
		mEditUuidParts[0] = (EditText) v.findViewById(R.id.edit_uuid_part1);
		mEditUuidParts[1] = (EditText) v.findViewById(R.id.edit_uuid_part2);
		mEditUuidParts[2] = (EditText) v.findViewById(R.id.edit_uuid_part3);
		mEditUuidParts[3] = (EditText) v.findViewById(R.id.edit_uuid_part4);
		mEditUuidParts[4] = (EditText) v.findViewById(R.id.edit_uuid_part5);
		mEditMajor = (EditText) v.findViewById(R.id.edit_major);
		mEditMinor = (EditText) v.findViewById(R.id.edit_minor);

		Button confirmBtn = (Button) v.findViewById(R.id.btn_confirm);

		confirmBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mListener != null) {
					StringBuilder sb = new StringBuilder();
					for (EditText part : mEditUuidParts) {
						sb.append(part.getText().toString());
						sb.append('-');
					}
					sb.deleteCharAt(sb.length() - 1);
					String uuid = sb.toString();
					int major = Integer.parseInt(mEditMajor.getText()
							.toString(), 16);
					int minor = Integer.parseInt(mEditMinor.getText()
							.toString(), 16);
					AddBeaconCallout.this.mListener.onConfirmBeaconAdd(
							AddBeaconCallout.this, uuid, major, minor, x, y);
				}
				dismiss();
			}
		});
	}

	public void setOnConfirmBeaconAddListener(
			onConfirmBeaconAddListener listener) {
		mListener = listener;
	}

	public static interface onConfirmBeaconAddListener {
		/**
		 * 
		 * @param callout
		 *            显示弹窗
		 * @param name
		 *            添加Beacon的名称
		 * @param x
		 *            添加Beacon的x坐标（相对值）
		 * @param y
		 *            添加Beacon的y坐标（相对值）
		 */
		public void onConfirmBeaconAdd(View callout, String uuid, int major,
				int minor, double x, double y);
	}

	public void setPos(double x, double y) {
		this.x = x;
		this.y = y;
	}
}
