package com.richard.officenavigation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.richard.officenavigation.R;

public class InfoDialog extends Dialog implements
		android.view.View.OnClickListener {
	private String mTitle, mContent;
	private DialogInterface.OnClickListener mListener;

	public InfoDialog(Context context) {
		super(context);
	}

	public static InfoDialog newInstance(Context context, String title,
			String content, DialogInterface.OnClickListener listener) {
		InfoDialog d = new InfoDialog(context);
		d.mTitle = title;
		d.mContent = content;
		d.mListener = listener;
		return d;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		View v = View.inflate(getContext(), R.layout.dialog_info, null);
		setContentView(v);
		((TextView) v.findViewById(R.id.title)).setText(mTitle);
		((TextView) v.findViewById(R.id.content)).setText(mContent);
		v.findViewById(R.id.btn_confirm).setOnClickListener(this);
		v.findViewById(R.id.btn_cancel).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_confirm:
			mListener.onClick(this, DialogInterface.BUTTON_POSITIVE);
			break;
		case R.id.btn_cancel:
			mListener.onClick(this, DialogInterface.BUTTON_NEGATIVE);
			break;
		}
	}
}
