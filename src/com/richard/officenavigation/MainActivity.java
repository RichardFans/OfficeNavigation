package com.richard.officenavigation;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.richard.officenavigation.adapter.BottomTabAdapter;
import com.richard.officenavigation.fragment.HomepageFragment;
import com.richard.officenavigation.fragment.ManageFragment;
import com.richard.officenavigation.fragment.MapFragment;
import com.richard.officenavigation.fragment.SearchFragment;
import com.richard.officenavigation.view.ClearableEditText;
import com.richard.officenavigation.view.ClearableEditText.LeftDrawableListener;
import com.viewpagerindicator.CustomIconPosTabPageIndicator;

public class MainActivity extends BaseActivity implements OnPageChangeListener,
		LeftDrawableListener {
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

	private MenuItem mItemSearch, mItemCheckIn;

	private CustomIconPosTabPageIndicator mTabPageIndicator;
	private ViewPager mVpMain;
	private AlertDialog mDlgCheckIn;

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

		createDialog();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private int mCheckWhich;
	private static final String[] CHECK_IN_OUT = new String[] { "签到", "离开" };

	private void createDialog() {
		// 考勤对话框
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("选择方式");
		b.setSingleChoiceItems(CHECK_IN_OUT, 0, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mCheckWhich = which;
			}
		});
		b.setPositiveButton("确认", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				m(CHECK_IN_OUT[mCheckWhich]);
			}
		});
		b.setNegativeButton("取消", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		mDlgCheckIn = b.create();
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int page) {
		d("onPageSelected page = " + page);
		ActionBar actionBar = getActionBar();
		if (page == HOME_PAGE_INDEX) {
			actionBar.setDisplayHomeAsUpEnabled(false);
			if (mItemCheckIn != null) {
				mItemCheckIn.setVisible(true);
				mItemSearch.setVisible(true);
			}
		} else {
			actionBar.setDisplayHomeAsUpEnabled(true);
			if (mItemCheckIn != null) {
				mItemCheckIn.setVisible(false);
				mItemSearch.setVisible(false);
				mItemSearch.collapseActionView();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = false;
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mVpMain.getCurrentItem() != HOME_PAGE_INDEX) {
				mTabPageIndicator.setCurrentItem(HOME_PAGE_INDEX);
			}
			break;
		case R.id.action_check_in:
			mDlgCheckIn.show();
			break;
		default:
			ret = super.onOptionsItemSelected(item);
		}
		return ret;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		d("onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.main, menu);
		mItemCheckIn = menu.findItem(R.id.action_check_in);
		mItemSearch = menu.findItem(R.id.action_search);
		if (mVpMain.getCurrentItem() != HOME_PAGE_INDEX) {
			mItemCheckIn.setVisible(false);
			mItemSearch.setVisible(false);
		}

		View vg = mItemSearch.getActionView();
		ClearableEditText cet = (ClearableEditText) vg
				.findViewById(R.id.edit_search);
		cet.setLeftDrawableListener(this);
		return true;
	}

	@Override
	public void onClick(String text) {
		m("搜索：" + text);
	}
}
