package com.richard.officenavigation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.richard.officenavigation.R;

public class MapParamsSetterDialog extends Dialog implements
		android.view.View.OnClickListener {
	private onConfirmSettingListener mListener;
	private EditText mEditWidth, mEditHeight, mEditScale;

	public MapParamsSetterDialog(Context context) {
		super(context);
	}

	public static MapParamsSetterDialog newInstance(Context context,
			onConfirmSettingListener listener) {
		MapParamsSetterDialog d = new MapParamsSetterDialog(context);
		d.mListener = listener;
		return d;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View view = View.inflate(getContext(),
				R.layout.dialog_map_params_setter, null);
		setContentView(view);
		mEditWidth = (EditText) view.findViewById(R.id.edit_width);
		mEditHeight = (EditText) view.findViewById(R.id.edit_height);
		mEditScale = (EditText) view.findViewById(R.id.edit_mm_px_scale);

		view.findViewById(R.id.btn_confirm).setOnClickListener(this);
		view.findViewById(R.id.btn_cancel).setOnClickListener(this);
	}

	public interface onConfirmSettingListener {
		void onConfirmSetting(long width, long height, double scale);
	}

	public void setOnConfirmSettingListener(onConfirmSettingListener listener) {
		mListener = listener;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_confirm:
			handleConfirm();
			break;
		case R.id.btn_cancel:
			dismiss();
			break;
		}
	}

	private void handleConfirm() {
		String w = mEditWidth.getText().toString();
		String h = mEditHeight.getText().toString();
		String s = mEditScale.getText().toString();

		if (w.equals("") || h.equals("") || s.equals("")) {
			Toast.makeText(getContext(), "参数不能为空", Toast.LENGTH_LONG).show();
			return;
		}

		if (mListener != null) {
			mListener.onConfirmSetting(Long.parseLong(w), Long.parseLong(h),
					Double.parseDouble(s));
		}
		dismiss();
	}
}
