package com.richard.officenavigation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.richard.officenavigation.R;
import com.richard.officenavigation.view.ClearableEditText;
import com.richard.officenavigation.view.ClearableEditText.LeftDrawableListener;

public class SearchFragment extends TabPagerFragment implements LeftDrawableListener {

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search, container, false);
		setHasOptionsMenu(true);
		return v;
	}	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.search, menu);
		MenuItem itemSearch = menu.findItem(R.id.action_search);
		View vg = itemSearch.getActionView();
		ClearableEditText cet = (ClearableEditText) vg
				.findViewById(R.id.edit_search);
		cet.setLeftDrawableListener(this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add_goods:
			m("Ìí¼ÓÎïÆ·");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(String text) {
		m("ËÑË÷£º" + text);
	}
}
