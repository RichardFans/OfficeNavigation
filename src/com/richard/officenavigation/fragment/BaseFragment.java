package com.richard.officenavigation.fragment;

import com.richard.officenavigation.Constants.C;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

public class BaseFragment extends Fragment {

	private String tag;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tag = getClass().getSimpleName();
	}

	protected void m(String msg) {
		Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
	}

	protected void d(String msg) {
		if (C.DEBUG)
			Log.d(tag, msg);
	}

	protected void i(String msg) {
		if (C.DEBUG)
			Log.i(tag, msg);
	}

	protected void w(String msg) {
		if (C.DEBUG)
			Log.w(tag, msg);
	}

	protected void v(String msg) {
		if (C.DEBUG)
			Log.v(tag, msg);
	}

	protected void e(String msg) {
		if (C.DEBUG)
			Log.e(tag, msg);
	}
}
