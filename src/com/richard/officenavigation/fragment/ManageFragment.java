package com.richard.officenavigation.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.richard.officenavigation.R;
import com.richard.officenavigation.Constants.C;

public class ManageFragment extends BaseFragment implements OnClickListener {

	private CheckBox mCbBootStart, mCbVibration, mCbRecvMsg;
	private SharedPreferences mSpManage;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_manage, container, false);
		v.findViewById(R.id.rl_boot_start).setOnClickListener(this);
		v.findViewById(R.id.rl_recv_msg).setOnClickListener(this);
		v.findViewById(R.id.rl_vibration).setOnClickListener(this);
		v.findViewById(R.id.rl_aboutus).setOnClickListener(this);
		v.findViewById(R.id.rl_feedback).setOnClickListener(this);
		v.findViewById(R.id.rl_share).setOnClickListener(this);
		v.findViewById(R.id.rl_chk_update).setOnClickListener(this);
		v.findViewById(R.id.rl_logout).setOnClickListener(this);

		mCbBootStart = (CheckBox) v.findViewById(R.id.cb_boot_start);
		mCbVibration = (CheckBox) v.findViewById(R.id.cb_vibration);
		mCbRecvMsg = (CheckBox) v.findViewById(R.id.cb_recv_msg);

		mSpManage = getActivity().getSharedPreferences(C.PREFERENCES_MANAGE,
				Context.MODE_PRIVATE);
		mCbBootStart.setChecked(mSpManage.getBoolean(C.manage.KEY_BOOTSTART,
				C.manage.DEFVAL_BOOTSTART));
		mCbVibration.setChecked(mSpManage.getBoolean(C.manage.KEY_VIBRATION,
				C.manage.DEFVAL_VIBRATION));
		mCbRecvMsg.setChecked(mSpManage.getBoolean(C.manage.KEY_RECV_MSG,
				C.manage.DEFVAL_RECV_MSG));
		return v;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_boot_start:
		case R.id.rl_recv_msg:
		case R.id.rl_vibration:
			handlePreferences(v.getId());
			break;
		case R.id.rl_aboutus:
			d("rl_aboutus");
			break;
		case R.id.rl_feedback:
			d("rl_feedback");
			break;
		case R.id.rl_share:
			d("rl_share");
			break;
		case R.id.rl_chk_update:
			d("rl_chk_update");
			break;
		case R.id.rl_logout:
			d("rl_logout");
			break;
		}
	}

	private void handlePreferences(int id) {
		SharedPreferences.Editor edit = mSpManage.edit();
		switch (id) {
		case R.id.rl_boot_start:
			mCbBootStart.toggle();
			edit.putBoolean(C.manage.KEY_BOOTSTART, mCbBootStart.isChecked());
			break;
		case R.id.rl_recv_msg:
			mCbRecvMsg.toggle();
			edit.putBoolean(C.manage.KEY_RECV_MSG, mCbRecvMsg.isChecked());
			break;
		case R.id.rl_vibration:
			mCbVibration.toggle();
			edit.putBoolean(C.manage.KEY_VIBRATION, mCbVibration.isChecked());
			break;
		}
		edit.commit();
	}
}
