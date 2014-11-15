package com.richard.officenavigation.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.viewpagerindicator.IconPagerAdapter;

public class BottomTabAdapter extends FragmentPagerAdapter implements
		IconPagerAdapter {
	private Fragment[] mFragments;
	private String[] mTitles;
	private int[] mIcons;

	public BottomTabAdapter(FragmentManager fm, String[] titles, int[] icons,
			Fragment[] fragments) {
		super(fm);
		mTitles = titles;
		mIcons = icons;
		mFragments = fragments;
	}

	@Override
	public Fragment getItem(int position) {
		return mFragments[position];
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return mTitles[position];
	}

	@Override
	public int getIconResId(int index) {
		return mIcons[index];
	}

	@Override
	public int getCount() {
		return mTitles.length;
	}
}