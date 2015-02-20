package com.richard.officenavigation.adapter;

import jama.Matrix;

import java.util.ArrayList;
import java.util.List;

import jkalman.JKalman;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarLineChartBase.BorderPosition;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.richard.officenavigation.R;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.IRssiRaw;

public class AverageResultsAdapter extends BaseExpandableListAdapter
		implements OnChartValueSelectedListener {
	public static final int INDEX_RAW_DATAS = 0;
	public static final int INDEX_KALMAN_DATAS = 1;
	public static final int INDEX_WAVELET_DATAS = 2;
	public static final int INDEX_GAUSSIAN_DATAS = 3;

	private Context mContext;
	private ArrayList<String> mGroupListOrient;
	private ArrayList<SparseArray<ArrayList<SparseIntArray>>> mChildListRssi;

	public AverageResultsAdapter(Context context) {
		mContext = context;
		mGroupListOrient = new ArrayList<>();
		mGroupListOrient.add(mContext.getString(R.string.ori_east));
		mGroupListOrient.add(mContext.getString(R.string.ori_sorth));
		mGroupListOrient.add(mContext.getString(R.string.ori_west));
		mGroupListOrient.add(mContext.getString(R.string.ori_north));

		mChildListRssi = new ArrayList<>();
		mChildListRssi.add(new SparseArray<ArrayList<SparseIntArray>>());
		mChildListRssi.add(new SparseArray<ArrayList<SparseIntArray>>());
		mChildListRssi.add(new SparseArray<ArrayList<SparseIntArray>>());
		mChildListRssi.add(new SparseArray<ArrayList<SparseIntArray>>());

		// genDatas(6, 60, -80, -40);
	}

	/**
	 * 高斯滤波 目前只对拥有卡尔曼滤波结果的数据进行高斯滤波
	 */
	public void filteringByGaussian() {
		for (SparseArray<ArrayList<SparseIntArray>> sa : mChildListRssi) {
			for (int i = 0; i < sa.size(); i++) {
				int minor = sa.keyAt(i);
				ArrayList<SparseIntArray> datasBeacon = sa.get(minor);
				SparseIntArray kalmanDatas = datasBeacon
						.get(INDEX_KALMAN_DATAS);
				SparseIntArray gaussianDatas = datasBeacon
						.get(INDEX_GAUSSIAN_DATAS);
				SparseIntArray datas = null;
				if (kalmanDatas.size() > 0) {
					// 用卡尔曼处理结果求平均：
					datas = kalmanDatas;
					// 目前只对拥有卡尔曼滤波结果的数据进行高斯滤波
					ftGaussian(datas, gaussianDatas);
				}
			}
		}
	}

	/**
	 * 高斯滤波
	 * 
	 * @param datas
	 * @param gaussianDatas
	 */
	private void ftGaussian(SparseIntArray datas, SparseIntArray gaussianDatas) {
		double u = 0, σ = 0, d = 0;
		// 求平均u
		for (int i = 0; i < datas.size(); i++) {
			int x = datas.keyAt(i);
			double y = datas.get(x);
			u += y;
		}
		u /= datas.size();
		// 求方差σ
		for (int i = 0; i < datas.size(); i++) {
			int x = datas.keyAt(i);
			double y = datas.get(x);
			d += (y - u) * (y - u);
		}
		σ = Math.sqrt(d / datas.size());
		// 滤波
		int low = (int) Math.round(u - 0.15 * σ);
		int high = (int) Math.round(u + 3.09 * σ);
		for (int i = 0; i < datas.size(); i++) {
			int x = datas.keyAt(i);
			double y = datas.get(x);
			if (y < low) {
				y = low;
			} else if (y > high) {
				y = high;
			}
			gaussianDatas.put(x, (int) y);
		}
	}

	class AverageSizeHolder {
		public int a, s;
	}

	public void filteringByKalman() {
		ArrayList<AverageSizeHolder> asHolderList = new ArrayList<>();
		for (SparseArray<ArrayList<SparseIntArray>> sa : mChildListRssi) {
			if (asHolderList.size() < sa.size()) {
				int d = sa.size() - asHolderList.size();
				for (int i = 0; i < d; i++)
					asHolderList.add(new AverageSizeHolder());
			}
			for (int i = 0; i < sa.size(); i++) {
				int minor = sa.keyAt(i);
				ArrayList<SparseIntArray> datasBeacon = sa.get(minor);
				SparseIntArray rawDatas = datasBeacon.get(INDEX_RAW_DATAS);
				AverageSizeHolder ash = asHolderList.get(i);
				ash.s = rawDatas.size();
				ash.a = 0;
				for (int j = 0; j < rawDatas.size(); j++) {
					int xVal = rawDatas.keyAt(j);
					int yVal = rawDatas.get(xVal);
					ash.a += yVal;
				}
				ash.a /= ash.s;
			}
			// 利用asHolderList，选出最合理的4组原始数据进行Kalman滤波
			int[] rIndexs = findReasonableDatasIndexs(asHolderList, sa.size());
			for (int index : rIndexs) {
				int minor = sa.keyAt(index);
				ArrayList<SparseIntArray> datasBeacon = sa.get(minor);
				SparseIntArray rawDatas = datasBeacon.get(INDEX_RAW_DATAS);
				SparseIntArray kalmanDatas = datasBeacon
						.get(INDEX_KALMAN_DATAS);
				ftKalman(rawDatas, kalmanDatas);
			}
		}
	}

	private void ftKalman(SparseIntArray rawDatas, SparseIntArray kalmanDatas) {
		ArrayList<Integer> datas = new ArrayList<>();
		// 插值
		int xLast = 0, yLast = 0;
		for (int i = 0; i < rawDatas.size(); i++) {
			int xVal = rawDatas.keyAt(i);
			int yVal = rawDatas.get(xVal);
			if (i > 0) {
				if (xVal > xLast + 1) {
					int dy = (yVal - yLast) / (xVal - xLast);
					for (int j = 1; j < xVal - xLast; j++)
						datas.add(yLast + dy * j);
				}
			} else {
				for (int j = 0; j < xVal; j++)
					datas.add(yVal);
			}
			datas.add(yVal);
			xLast = xVal;
			yLast = yVal;
		}
		// 滤波
		try {
			JKalman kalman = new JKalman(2, 1);
			Matrix s = new Matrix(2, 1); // state [y, dy]
			Matrix c = new Matrix(2, 1); // corrected state [y, dy]
			Matrix m = new Matrix(1, 1); // measurement [x]
			double[][] tr = { { 1, 1 }, { 0, 1 } };
			kalman.setTransition_matrix(new Matrix(tr));
			kalman.setError_cov_post(kalman.getError_cov_post().identity());
			// 设置初始状态
			s.set(0, 0, datas.get(0));
			s.set(0, 0, 0);
			kalmanDatas.clear();
			for (int i = 0; i < datas.size(); i++) {
				s = kalman.Predict();
				m.set(0, 0, datas.get(i));
				c = kalman.Correct(m);
				kalmanDatas.put(i, (int) c.get(0, 0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 利用asHolderList，选出最合理的5组原始数据进行Kalman滤波
	 * 
	 * @param sa
	 * @param asHolderList
	 * @param size
	 * @return
	 */
	private int[] findReasonableDatasIndexs(
			ArrayList<AverageSizeHolder> asHolderList, int size) {
		int[] rIndexs = new int[] { -1, -1, -1, -1, -1 };
		double score;
		int maxS = 0, minA = 0;
		for (AverageSizeHolder ash : asHolderList) {
			if (maxS < ash.s) {
				maxS = ash.s;
			}
			if (minA > ash.a) {
				minA = ash.a;
			}
		}
		double[] scores = new double[] { Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
				Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY }; // 合理程度评估分数
		for (int index = 0; index < size; index++) {
			AverageSizeHolder ash = asHolderList.get(index);
			score = (double) ash.s / maxS - (double) ash.a / (minA << 1);
			for (int i = 0; i < rIndexs.length; i++) {
				if (score > scores[i]) {
					for (int j = rIndexs.length - 1; j > i; j--) {
						scores[j] = scores[j - 1];
						rIndexs[j] = rIndexs[j - 1];
					}
					scores[i] = score;
					rIndexs[i] = index;
					break;
				}
			}
		}
		return rIndexs;
	}

	public void setAllRawDatas(List<IRssiRaw> allRawDatas) {
		// 清空
		for (SparseArray<ArrayList<SparseIntArray>> sa : mChildListRssi) {
			sa.clear();
		}
		// 重新设置数据
		for (IRssiRaw d : allRawDatas) {
			SparseArray<ArrayList<SparseIntArray>> datasBeacons = mChildListRssi
					.get(d.getOrientation());
			ArrayList<SparseIntArray> datasBeacon = datasBeacons.get(d
					.getMinor());
			if (datasBeacon == null) {
				datasBeacon = new ArrayList<>();
				datasBeacon.add(new SparseIntArray()); // 原始数据
				datasBeacon.add(new SparseIntArray()); // 卡尔曼滤波
				datasBeacon.add(new SparseIntArray()); // 小波变换
				datasBeacon.add(new SparseIntArray()); // 高斯滤波
				datasBeacons.put(d.getMinor(), datasBeacon);
			}
			SparseIntArray rawDatas = datasBeacon.get(INDEX_RAW_DATAS);
			rawDatas.put(d.getXVal(), d.getYVal());
		}
	}

	/**
	 * 目前只保留经过筛选，进行了卡尔曼滤波和高斯滤波，最后求得均值的结果
	 * 
	 * @param nodeId
	 * @param mapId
	 * @return
	 */
	public ArrayList<IRssi> getAllDatasForPersistence(long nodeId, Long mapId) {
		ArrayList<IRssi> allDatas = new ArrayList<>();
		for (int orient = 0; orient < 4; orient++) {
			SparseArray<ArrayList<SparseIntArray>> datasBeacons = mChildListRssi
					.get(orient);

			for (int i = 0; i < datasBeacons.size(); i++) {
				int minor = datasBeacons.keyAt(i);
				ArrayList<SparseIntArray> datasBeacon = datasBeacons.get(minor);
				SparseIntArray datas = datasBeacon.get(INDEX_GAUSSIAN_DATAS);
				if (datas.size() > 0) {
					int u = 0;
					for (int j = 0; j < datas.size(); j++) {
						int xVal = datas.keyAt(j);
						int yVal = datas.get(xVal);
						u += yVal;
					}
					u /= datas.size();
					IRssi rssi = new IRssi();
					rssi.setNodeId(nodeId);
					rssi.setMapId(mapId);
					rssi.setBeaconId(minor); // 为了方便，目前beaconId字段存放minor
					rssi.setOrientation(orient);
					rssi.setValue(u);
					allDatas.add(rssi);
					Log.d("mytag", "orient = " + orient + ", minor = " + minor
							+ ", rssi = " + u);

				}
			}
		}
		return allDatas;
	}

	public ArrayList<IRssiRaw> getAllRawDatasForPersistence(long nodeId,
			Long mapId) {
		ArrayList<IRssiRaw> allRawDatas = new ArrayList<>();
		for (int orient = 0; orient < 4; orient++) {
			SparseArray<ArrayList<SparseIntArray>> datasBeacons = mChildListRssi
					.get(orient);

			for (int i = 0; i < datasBeacons.size(); i++) {
				int minor = datasBeacons.keyAt(i);
				ArrayList<SparseIntArray> datasBeacon = datasBeacons.get(minor);
				SparseIntArray rawDatas = datasBeacon.get(INDEX_RAW_DATAS);

				for (int j = 0; j < rawDatas.size(); j++) {
					int xVal = rawDatas.keyAt(j);
					int yVal = rawDatas.get(xVal);
					IRssiRaw rssiRaw = new IRssiRaw();
					rssiRaw.setNodeId(nodeId);
					rssiRaw.setMapId(mapId);
					rssiRaw.setMinor(minor);
					rssiRaw.setOrientation(orient);
					rssiRaw.setXVal(xVal);
					rssiRaw.setYVal(yVal);
					allRawDatas.add(rssiRaw);
				}
			}
		}
		return allRawDatas;
	}

	public void setRawDatas(int orient, int beaconMinor, SparseIntArray rawDatas) {
		orient %= 4;
		SparseArray<ArrayList<SparseIntArray>> datasBeacons = mChildListRssi
				.get(orient);
		ArrayList<SparseIntArray> datasBeacon = datasBeacons.get(beaconMinor);
		if (datasBeacon == null) {
			datasBeacon = new ArrayList<>();
			datasBeacon.add(new SparseIntArray()); // 原始数据
			datasBeacon.add(new SparseIntArray()); // 卡尔曼滤波
			datasBeacon.add(new SparseIntArray()); // 小波变换
			datasBeacon.add(new SparseIntArray()); // 高斯滤波
			datasBeacons.put(beaconMinor, datasBeacon);
		}
		datasBeacon.set(INDEX_RAW_DATAS, rawDatas);
		notifyDataSetChanged();
	}

	/**
	 * for test
	 */
	public void genDatas(int beacon_nr, int data_nr, float data_min,
			float data_max) {
		for (SparseArray<ArrayList<SparseIntArray>> sa : mChildListRssi) {
			for (int i = 0; i < beacon_nr; i++) {
				ArrayList<SparseIntArray> datasBeacon = new ArrayList<>();
				datasBeacon.add(new SparseIntArray()); // 原始数据
				datasBeacon.add(new SparseIntArray()); // 卡尔曼滤波
				datasBeacon.add(new SparseIntArray()); // 小波变换
				datasBeacon.add(new SparseIntArray()); // 高斯滤波
				SparseIntArray datasRaw = datasBeacon.get(INDEX_RAW_DATAS);
				for (int j = 0; j < data_nr; j++) {
					int data = (int) Math.round((data_max - data_min)
							* Math.random() + data_min);
					datasRaw.put(j, data);
				}
				// Kalman
				SparseIntArray datasKalman = datasBeacon
						.get(INDEX_KALMAN_DATAS);
				for (int j = 0; j < data_nr; j++) {
					int data = (int) Math.round((data_max - data_min)
							* Math.random() + data_min);
					datasKalman.put(j, data);
				}
				// Wavelet
				SparseIntArray datasWavelet = datasBeacon
						.get(INDEX_WAVELET_DATAS);
				for (int j = 0; j < (data_nr >> 2); j++) {
					int data = (int) Math.round((data_max - data_min)
							* Math.random() + data_min);
					datasWavelet.put(j << 2, data);
				}
				//
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
		SparseArray<ArrayList<SparseIntArray>> datasBeacons = mChildListRssi
				.get(groupPosition);
		int key = datasBeacons.keyAt(childPosition);
		ArrayList<SparseIntArray> datasBeacon = datasBeacons.get(key);

		ViewHolderChild holder = null;
		if (convertView == null) {
			holder = new ViewHolderChild();

			convertView = View.inflate(mContext, R.layout.list_item_linechart,
					null);
			holder.chart = (LineChart) convertView.findViewById(R.id.chart);
			holder.chart.setOnChartValueSelectedListener(this);
			holder.chart.setStartAtZero(false);
			holder.chart.setDrawYValues(true);
			holder.chart.setMaxVisibleValueCount(60);
			holder.chart.setDrawBorder(true);
			holder.chart
					.setBorderPositions(new BorderPosition[] { BorderPosition.BOTTOM });
			holder.chart.setDragEnabled(true);
			holder.chart.setScaleEnabled(true);
			holder.chart.setDrawGridBackground(true);
			holder.chart.setDrawVerticalGrid(true);
			holder.chart.setDrawHorizontalGrid(true);
			holder.chart.setPinchZoom(false);

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

	private void setData(LineChart chart, ArrayList<SparseIntArray> datasBeacon) {
		SparseIntArray datasRaw = datasBeacon.get(INDEX_RAW_DATAS);
		SparseIntArray datasKalman = datasBeacon.get(INDEX_KALMAN_DATAS);
		SparseIntArray datasWavelet = datasBeacon.get(INDEX_WAVELET_DATAS);
		SparseIntArray datasGaussian = datasBeacon.get(INDEX_GAUSSIAN_DATAS);

		ArrayList<String> xVals = new ArrayList<String>();
		int xNr = datasRaw.keyAt(datasRaw.size() - 1) + 1;
		for (int i = 0; i < xNr; i++) {
			xVals.add((i) + "");
		}

		// Raw
		LineDataSet set1 = getDataSet(datasRaw,
				mContext.getString(R.string.raw_datas),
				ColorTemplate.PASTEL_COLORS[INDEX_RAW_DATAS]);
		// Kalman
		LineDataSet set2 = getDataSet(datasKalman,
				mContext.getString(R.string.kalman_datas),
				ColorTemplate.PASTEL_COLORS[INDEX_KALMAN_DATAS]);
		// Wavelet
		LineDataSet set3 = getDataSet(datasWavelet,
				mContext.getString(R.string.wavelet_datas),
				ColorTemplate.PASTEL_COLORS[INDEX_WAVELET_DATAS]);

		// Wavelet
		LineDataSet set4 = getDataSet(datasGaussian,
				mContext.getString(R.string.gaussian_datas),
				ColorTemplate.PASTEL_COLORS[INDEX_GAUSSIAN_DATAS]);

		ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
		dataSets.add(set1);
		dataSets.add(set2);
		dataSets.add(set3);
		dataSets.add(set4);

		LineData data = new LineData(xVals, dataSets);

		chart.setData(data);
	}

	private LineDataSet getDataSet(SparseIntArray datas, String datasName,
			int color) {
		ArrayList<Entry> yVals = new ArrayList<Entry>();
		for (int i = 0; i < datas.size(); i++) {
			int x = datas.keyAt(i);
			int y = datas.valueAt(i);
			yVals.add(new Entry(y, x));
		}
		LineDataSet set = new LineDataSet(yVals, datasName);
		set.setColor(color);
		set.setCircleColor(color);
		set.setLineWidth(1f);
		set.setCircleSize(3f);
		set.setFillAlpha(65);
		set.setFillColor(color);
		return set;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	@Override
	public void onValueSelected(Entry e, int dataSetIndex) {
		Log.i("Entry selected", e.toString());
	}

	@Override
	public void onNothingSelected() {

	}

	private class ViewHolderChild {
		LineChart chart;
	}

	private class ViewHolderGroup {
		TextView title;
	}
}
