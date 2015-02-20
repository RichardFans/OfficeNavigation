package com.richard.officenavigation;

import com.richard.officenavigation.constants.C;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

public class BaseActivity extends FragmentActivity {

	private String tag;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tag = getClass().getSimpleName();
		findViews();
		setupViews();
		initDatas(savedInstanceState);
	}

	/**
	 * You should do two thing: 1. setContentView 2. findView
	 */
	protected void findViews() {

	}

	/**
	 * Call after findView(). You should do something like: setListener or
	 * setAdapter.
	 */
	protected void setupViews() {

	}

	/**
	 * Call after setupViews().
	 */
	protected void initDatas(Bundle savedInstanceState) {

	}

	protected void m(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
