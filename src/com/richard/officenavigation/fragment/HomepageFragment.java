package com.richard.officenavigation.fragment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;

import com.richard.officenavigation.MainActivity;
import com.richard.officenavigation.R;
import com.richard.officenavigation.Constants.C;

public class HomepageFragment extends TabPagerFragment implements
		OnItemClickListener, OnClickListener {
	private static final int[] PICS = { R.drawable.pic_miss_liu,
			R.drawable.pic_miss_li, R.drawable.pic_mr_fan,
			R.drawable.pic_mr_luo, R.drawable.pic_mr_yang,
			R.drawable.pic_mr_ma, R.drawable.pic_mr_zhang,
			R.drawable.pic_mr_li, };
	private static final String[] NAMES = { "刘芳老师", "李瑶老师", "范展源老师", "罗福强老师",
			"杨峰老师", "马磊老师", "张志亮老师", "李恒毅老师", };
	private static final String PIC_KEY = "pic";
	private static final String NAME_KEY = "name";

	private GridView mGvTeachers;

	private Dialog mDlgSelectMethod, mDlgCheckInOrOut;
	private Button mBtnDsmConfirm, mBtnDsmCancel, mBtnCioConfirm,
			mBtnCioCancel;
	private RadioGroup mRgDsmSelectMethod, mRgCioSelectMethod;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		createDialog();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_homepage, container, false);
		mGvTeachers = (GridView) v.findViewById(R.id.container);

		ListAdapter adapter = new SimpleAdapter(getActivity(), getData(),
				R.layout.simple_gradview_item,
				new String[] { PIC_KEY, NAME_KEY }, new int[] { R.id.iv_pic,
						R.id.tv_name });
		mGvTeachers.setAdapter(adapter);
		mGvTeachers.setOnItemClickListener(this);

		setHasOptionsMenu(true);
		return v;
	}

	private void createDialog() {
		// 选择操作对话框
		mDlgSelectMethod = new Dialog(getActivity());
		mDlgSelectMethod.requestWindowFeature(Window.FEATURE_NO_TITLE);
		View v = View.inflate(getActivity(), R.layout.dialog_select_method,
				null);
		mDlgSelectMethod.setContentView(v);
		mRgDsmSelectMethod = (RadioGroup) v.findViewById(R.id.rg_select_method);
		mBtnDsmCancel = (Button) v.findViewById(R.id.btn_cancel);
		mBtnDsmConfirm = (Button) v.findViewById(R.id.btn_confirm);
		mBtnDsmConfirm.setOnClickListener(this);
		mBtnDsmCancel.setOnClickListener(this);

		// 考勤对话框
		mDlgCheckInOrOut = new Dialog(getActivity());
		mDlgCheckInOrOut.requestWindowFeature(Window.FEATURE_NO_TITLE);
		v = View.inflate(getActivity(), R.layout.dialog_check_in_or_out, null);
		mDlgCheckInOrOut.setContentView(v);
		mRgCioSelectMethod = (RadioGroup) v
				.findViewById(R.id.rg_select_in_or_out);
		mBtnCioCancel = (Button) v.findViewById(R.id.btn_cancel);
		mBtnCioConfirm = (Button) v.findViewById(R.id.btn_confirm);
		mBtnCioConfirm.setOnClickListener(this);
		mBtnCioCancel.setOnClickListener(this);
	}

	private List<? extends Map<String, ?>> getData() {
		List<Map<String, Object>> data = new LinkedList<Map<String, Object>>();
		Map<String, Object> m = null;
		for (int i = 0; i < PICS.length; i++) {
			m = new HashMap<String, Object>();
			m.put(NAME_KEY, NAMES[i]);
			m.put(PIC_KEY, PICS[i]);
			data.add(m);
		}
		return data;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.homepage, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_check_in:
			mDlgCheckInOrOut.show();
			break;
		case R.id.action_add_contact:
			m("添加联系人");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		d("你点击了" + NAMES[position]);
		mBtnDsmConfirm.setTag(NAMES[position] + "办公桌");
		mDlgSelectMethod.show();
	}

	@Override
	public void onClick(View v) {
		if (v == mBtnDsmConfirm) {
			mDlgSelectMethod.dismiss();
			String name = (String) mBtnDsmConfirm.getTag();
			if (mRgDsmSelectMethod.getCheckedRadioButtonId() == R.id.rb_find_desk) {
				handleFindDesk(name);
			} else {
				m("给" + name + "留言");
			}
		} else if (v == mBtnDsmCancel) {
			mDlgSelectMethod.dismiss();
		} else if (v == mBtnCioConfirm) {
			mDlgCheckInOrOut.dismiss();
			if (mRgCioSelectMethod.getCheckedRadioButtonId() == R.id.rb_check_in) {
				m("签到");
			} else {
				m("离开");
			}
		} else if (v == mBtnCioCancel) {
			mDlgCheckInOrOut.dismiss();
		}
	}

	private void handleFindDesk(String name) {
		Bundle data = new Bundle();
		data.putInt(C.map.KEY_ACTION, C.map.ACTION_NAVIGATION);
		data.putString(C.map.KEY_NAVI_ARG_NAME, name);
		jumpToPage(C.main.MAP_PAGE_INDEX, data);
	}

	@Override
	public void jumpToPage(int page, Bundle data) {
		MainActivity main = (MainActivity) getActivity();
		main.jump(page, data);
	}
}
