package com.richard.officenavigation;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;

import com.richard.officenavigation.adapter.BottomTabAdapter;
import com.richard.officenavigation.fragment.HomepageFragment;
import com.richard.officenavigation.fragment.ManageFragment;
import com.richard.officenavigation.fragment.MapFragment;
import com.richard.officenavigation.fragment.SearchFragment;

import com.viewpagerindicator.CustomIconPosTabPageIndicator;

public class MainActivity extends BaseActivity implements OnPageChangeListener {
	private static final int HOME_PAGE_INDEX = 0;
	private static final int SEARCH_PAGE_INDEX = 1;
	private static final int MAP_PAGE_INDEX = 2;
	private static final int MANAGE_PAGE_INDEX = 3;

	private static final String[] TITLES = new String[] { "首页", "寻找", "地图",
			"管理" };
	private static final int[] ICONS = new int[] {
			R.drawable.tab_bottom_homepage, R.drawable.tab_bottom_search,
			R.drawable.tab_bottom_map, R.drawable.tab_bottom_manage, };
	private static final Fragment[] FRAGMENTS = new Fragment[] {
			new HomepageFragment(), new SearchFragment(), new MapFragment(),
			new ManageFragment(), };

	private CustomIconPosTabPageIndicator mTabPageIndicator;
	private ViewPager mVpMain;

	@Override
	protected void findViews() {
		setContentView(R.layout.activity_main);
		mVpMain = (ViewPager) findViewById(R.id.pager);
		mTabPageIndicator = (CustomIconPosTabPageIndicator) findViewById(R.id.indicator);
	}

	@Override
	protected void setupViews() {
		FragmentPagerAdapter adapter = new BottomTabAdapter(
				getSupportFragmentManager(), TITLES, ICONS, FRAGMENTS);

		mVpMain.setAdapter(adapter);

		mTabPageIndicator
				.setTabIconLocation(CustomIconPosTabPageIndicator.LOCATION_UP);
		mTabPageIndicator.setViewPager(mVpMain);
		mTabPageIndicator.setOnPageChangeListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mVpMain.getCurrentItem() != HOME_PAGE_INDEX) {
				mTabPageIndicator.setCurrentItem(HOME_PAGE_INDEX);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPageSelected(int position) {
		ActionBar actionBar = getActionBar();
		if (position == HOME_PAGE_INDEX) {
			actionBar.setDisplayHomeAsUpEnabled(false);
		} else {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {

	}
}
