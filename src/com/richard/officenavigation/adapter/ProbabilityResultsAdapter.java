package com.richard.officenavigation.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase.BorderPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ValueFormatter;
import com.richard.officenavigation.R;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.IRssiRaw;

/**
 * 将训练结果
 * 
 * @author Administrator
 * 
 */
public class ProbabilityResultsAdapter extends BaseExpandableListAdapter {

	private Context mContext;
	private ArrayList<String> mGroupListOrient;
	private ArrayList<SparseArray<SparseIntArray>> mChildListRssi;

	public ProbabilityResultsAdapter(Context context) {
		mContext = context;
		mGroupListOrient = new ArrayList<>();
		mGroupListOrient.add(mContext.getString(R.string.ori_east));
		mGroupListOrient.add(mContext.getString(R.string.ori_sorth));
		mGroupListOrient.add(mContext.getString(R.string.ori_west));
		mGroupListOrient.add(mContext.getString(R.string.ori_north));

		mChildListRssi = new ArrayList<>();
		mChildListRssi.add(new SparseArray<SparseIntArray>());
		mChildListRssi.add(new SparseArray<SparseIntArray>());
		mChildListRssi.add(new SparseArray<SparseIntArray>());
		mChildListRssi.add(new SparseArray<SparseIntArray>());

		// genDatas(6, 60, -80, -40);
	}

	/**
	 * 将所有原始数据设置到适配器中
	 * 
	 * @param allRawDatas
	 *            所有原始数据
	 */
	public void setAllRawDatas(List<IRssiRaw> allRawDatas) {
		// 清空
		for (SparseArray<SparseIntArray> sa : mChildListRssi) {
			sa.clear();
		}
		// 重新设置数据
		for (IRssiRaw d : allRawDatas) {
			SparseArray<SparseIntArray> datasBeacons = mChildListRssi.get(d
					.getOrientation());
			SparseIntArray datasBeacon = datasBeacons.get(d.getMinor());
			if (datasBeacon == null) {
				datasBeacon = new SparseIntArray();
				datasBeacons.put(d.getMinor(), datasBeacon);
			}
			datasBeacon.put(d.getYVal(), datasBeacon.get(d.getYVal()) + 1);
		}
	}

	/**
	 * 将所有数据设置到适配器中
	 * 
	 * @param allDatas
	 *            所有rssi数据
	 */
	public void setAllDatas(List<IRssi> allDatas) {
		// 清空
		for (SparseArray<SparseIntArray> sa : mChildListRssi) {
			sa.clear();
		}
		// 重新设置数据
		for (IRssi d : allDatas) {
			SparseArray<SparseIntArray> datasBeacons = mChildListRssi.get(d
					.getOrientation());
			SparseIntArray datasBeacon = datasBeacons
					.get((int) d.getBeaconId());
			if (datasBeacon == null) {
				datasBeacon = new SparseIntArray();
				datasBeacons.put((int) d.getBeaconId(), datasBeacon);
			}
			datasBeacon.put((int) d.getValue(),
					(int) Math.round(d.getTotal() * d.getProbability()));
		}
	}

	/**
	 * 
	 * @param nodeId
	 * @param mapId
	 * @return
	 */
	public ArrayList<IRssi> getAllDatasForPersistence(long nodeId, Long mapId) {
		ArrayList<IRssi> allDatas = new ArrayList<>();
		for (int orient = 0; orient < 4; orient++) {
			SparseArray<SparseIntArray> datasBeacons = mChildListRssi
					.get(orient);

			for (int i = 0; i < datasBeacons.size(); i++) {
				int minor = datasBeacons.keyAt(i);
				SparseIntArray datasBeacon = datasBeacons.get(minor);
				if (datasBeacon != null) {
					int total = 0;
					for (int j = 0; j < datasBeacon.size(); j++)
						total += datasBeacon.valueAt(j);

					for (int j = 0; j < datasBeacon.size(); j++) {
						int value = datasBeacon.keyAt(j);
						int count = datasBeacon.get(value);
						IRssi rssi = new IRssi();
						rssi.setNodeId(nodeId);
						rssi.setMapId(mapId);
						rssi.setBeaconId(minor); // 为了方便，目前beaconId字段存放minor
						rssi.setOrientation(orient);
						rssi.setValue(value);
						rssi.setProbability((float) count / total);
						rssi.setTotal(total);
						allDatas.add(rssi);
					}
				}
			}
		}
		return allDatas;
	}

	/**
	 * 将采集到的数据存入适配器中
	 * 
	 * @param orient
	 * @param beaconMinor
	 * @param datas
	 */
	public void setDatas(int orient, int beaconMinor, SparseIntArray datas) {
		orient %= 4;
		SparseArray<SparseIntArray> datasBeacons = mChildListRssi.get(orient);
		datasBeacons.put(beaconMinor, datas);
		notifyDataSetChanged();
	}

	/**
	 * for test
	 */
	public void genDatas(int beacon_nr, int data_nr, float data_min,
			float data_max) {
		for (SparseArray<SparseIntArray> sa : mChildListRssi) {
			for (int i = 0; i < beacon_nr; i++) {
				SparseIntArray datasBeacon = new SparseIntArray();
				for (int j = 0; j < data_nr; j++) {
					int data = (int) Math.round((data_max - data_min)
							* Math.random() + data_min);
					datasBeacon.put(data, datasBeacon.get(data) + 1);
				}
				sa.put(i + 1, datasBeacon);
			}
		}
	}

	@Override
	public int getGroupCount() {
		return mGroupListOrient.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mChildListRssi.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mGroupListOrient.get(groupPosition);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mChildListRssi.get(groupPosition).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		String title = getGroup(groupPosition).toString();
		ViewHolderGroup holder = null;
		if (convertView == null) {
			holder = new ViewHolderGroup();
			convertView = new LinearLayout(mContext);
			convertView.setPadding(0, 10, 0, 10);
			holder.title = new TextView(mContext);
			((LinearLayout) convertView).addView(holder.title);
			convertView.setTag(holder);
			holder.title.setTextSize(18);
			holder.title.setPadding(128, 0, 0, 0);
		} else {
			holder = (ViewHolderGroup) convertView.getTag();
		}
		holder.title.setText(title);
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		SparseArray<SparseIntArray> datasBeacons = mChildListRssi
				.get(groupPosition);
		int key = datasBeacons.keyAt(childPosition);
		SparseIntArray datasBeacon = datasBeacons.get(key);

		ViewHolderChild holder = null;
		if (convertView == null) {
			holder = new ViewHolderChild();

			convertView = View.inflate(mContext, R.layout.list_item_barchart,
					null);
			holder.chart = (BarChart) convertView.findViewById(R.id.chart);
			holder.chart.setStartAtZero(false);
			holder.chart.setDrawYValues(true);
			holder.chart.setDrawValueAboveBar(true);
			holder.chart.setMaxVisibleValueCount(60);
			holder.chart.setDrawBorder(true);
			holder.chart.set3DEnabled(false);
			holder.chart
					.setBorderPositions(new BorderPosition[] { BorderPosition.BOTTOM });
			holder.chart.setDragEnabled(true);
			holder.chart.setScaleEnabled(true);
			holder.chart.setDrawGridBackground(true);
			holder.chart.setDrawVerticalGrid(true);
			holder.chart.setDrawHorizontalGrid(true);
			holder.chart.setPinchZoom(false);
			holder.chart.setValueFormatter(new ValueFormatter() {

				@Override
				public String getFormattedValue(float value) {
					return String.valueOf((int) value);
				}
			});
			convertView.setTag(holder);
		} else {
			holder = (ViewHolderChild) convertView.getTag();
		}
		holder.chart.animateX(1800);
		String desc = mContext.getString(R.string.beacon) + key;
		holder.chart.setDescription(desc);
		holder.chart.setScaleX(1);

		setData(holder.chart, datasBeacon);

		return convertView;
	}

	private void setData(BarChart chart, SparseIntArray datasBeacon) {
		ArrayList<String> xVals = new ArrayList<String>();
		for (int i = 0; i < 50; i++) {
			xVals.add((-40 - i) + "");
		}

		BarDataSet set = getDataSet(datasBeacon, ColorTemplate.getHoloBlue());

		ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
		dataSets.add(set);

		BarData data = new BarData(xVals, dataSets);

		chart.setData(data);
	}

	private BarDataSet getDataSet(SparseIntArray datas, int color) {
		ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();
		int total = 0;
		for (int i = 0; i < datas.size(); i++)
			total += datas.valueAt(i);

		for (int i = 0; i < datas.size(); i++) {
			int value = datas.keyAt(i);
			int count = datas.valueAt(i);
			// y轴显示的是百分比
			yVals.add(new BarEntry(Math.round(100f * count / total),
					-value - 40));
		}
		BarDataSet set = new BarDataSet(yVals, "概率分布图（共" + datas.size() + "组"
				+ total + "个数据）");
		set.setColor(color);
		set.setBarSpacePercent(35f);
		return set;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	private class ViewHolderChild {
		BarChart chart;
	}

	private class ViewHolderGroup {
		TextView title;
	}
}
