package com.richard.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.SparseIntArray;

import com.richard.officenavigation.dao.DaoSession;
import com.richard.officenavigation.dao.ICluster;
import com.richard.officenavigation.dao.IClusterItem;
import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.INodeClusterDao;
import com.richard.officenavigation.dao.INodeDao;
import com.richard.officenavigation.dao.IRssi;
import com.richard.officenavigation.dao.SingletonDaoSession;

public class Misc {
	private static final String FNN_SQL = "inner join "
			+ INodeClusterDao.TABLENAME + " N ON T."
			+ INodeDao.Properties.Id.columnName + " = N."
			+ INodeClusterDao.Properties.NodeId.columnName + "WHERE N."
			+ INodeClusterDao.Properties.ClusterId + " = ? AND N."
			+ INodeClusterDao.Properties.Orientation + " = ?";

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * 从数据源datasSrc中找出最大的n组数据
	 * 
	 * @param datasSrc
	 *            数据源
	 * @param nr
	 *            指定找出最大数据的组数
	 * @return 返回最大的n组数据(数据以按值进行排序，大的在前面)
	 */
	public static Map<Integer, Integer> findBiggestDatas(
			SparseIntArray datasSrc, int nr) {
		if (datasSrc.size() == 0)
			return null;

		int[][] datas = new int[nr][2];
		for (int i = 0; i < datas.length; i++) {
			datas[i][0] = -1;
			datas[i][1] = Integer.MIN_VALUE;
		}
		for (int index = 0; index < datasSrc.size(); index++) {
			int key = datasSrc.keyAt(index);
			int value = datasSrc.get(key);
			for (int i = 0; i < datas.length; i++) {
				if (value > datas[i][1]) {
					for (int j = datas.length - 1; j > i; j--) {
						datas[j][1] = datas[j - 1][1];
						datas[j][0] = datas[j - 1][0];
					}
					datas[i][1] = value;
					datas[i][0] = key;
					break;
				}
			}
		}
		Map<Integer, Integer> biggestDatas = new LinkedHashMap<>();
		for (int[] d : datas) {
			if (d[0] != -1)
				biggestDatas.put(d[0], d[1]);
		}
		return biggestDatas;
	}

	/**
	 * 从分类列表clusterList中找到匹配的分类
	 * 
	 * @param datas
	 *            用于匹配的数据
	 * @param clusterList
	 *            分类列表
	 * @return 如果找到匹配的，返回匹配上的分类，否则返回null
	 */
	public static ICluster findMatchedCluster(Map<Integer, Integer> datas,
			List<ICluster> clusterList) {
		boolean match = false;
		for (ICluster c : clusterList) {
			match = true;
			List<IClusterItem> items = c.getItems();
			for (Integer minor : datas.keySet()) {
				boolean contain = false;
				for (IClusterItem item : items) {
					if (item.getMinor() == minor) {
						contain = true;
						break;
					}
				}
				// 只要有一个不匹配就不算match
				if (!contain) {
					match = false;
					break;
				}
			}
			if (match)
				return c;
		}
		return null;
	}

	/**
	 * 从分类列表clusterList中找到匹配的分类集合
	 * 
	 * @param datas
	 *            用于匹配的数据（注意：key是信号强度，value是信标的minor）
	 * @param clusterList
	 *            分类列表
	 * @return 如果找到匹配的，返回匹配上的分类，否则返回null
	 */
	public static List<ICluster> findMatchedClusters(
			Map<Integer, Integer> datas, List<ICluster> clusterList) {
		boolean match = false;
		List<ICluster> matchedClusterList = new ArrayList<>();
		for (ICluster c : clusterList) {
			match = true;
			List<IClusterItem> items = c.getItems();
			for (Integer minor : datas.keySet()) {
				boolean contain = false;
				for (IClusterItem item : items) {
					if (item.getMinor() == minor) {
						contain = true;
						break;
					}
				}
				// 只要有一个不匹配就不算match
				if (!contain) {
					match = false;
					break;
				}
			}
			if (match) {
				matchedClusterList.add(c);
			}
		}
		return matchedClusterList;
	}

	/**
	 * 从指定分类和方向中寻找最接近的节点
	 * 
	 * @param clusters
	 *            所属的分类，在采集值小于C.map.CLUSTER_Q_VAL时，可能属于多个分类
	 * @param orient
	 *            指定的方向
	 * @param kDatas
	 *            用于匹配的数据
	 * @return 返回最匹配的节点
	 */
	public static INode findNearestNode(Context ctx, List<ICluster> clusters,
			int orient, Map<Integer, Integer> kDatas) {
		INode nodeFind = null;
		DaoSession session = SingletonDaoSession.getInstance(ctx);
		INodeDao nodeDao = session.getINodeDao();
		List<INode> nodes = new ArrayList<>();
		String[] args = new String[2];
		args[1] = Integer.toString(orient);
		for (ICluster ic : clusters) {
			args[0] = ic.getId().toString();
			nodes.addAll(nodeDao.queryRaw(FNN_SQL, args));
		}

		double maxProb = Double.MIN_VALUE;
		Map<Integer, Integer> datas = new HashMap<>();
		for (INode node : nodes) {
			double prob = 1.0;
			datas.putAll(kDatas);
			for (IRssi rssi : node.getRssis()) {
				if (rssi.getOrientation() == orient) {
					for (Integer minor : datas.keySet()) {
						if (minor == rssi.getBeaconId()) {
							Integer value = datas.get(minor);
							if (value == rssi.getValue()) {
								prob *= rssi.getProbability();
								datas.remove(minor);
								break;
							}
						}
					}
				}
				if (datas.size() == 0) {
					if (prob > maxProb) {
						maxProb = prob;
						nodeFind = node;
					}
					break;
				}
			}
		}
		return nodeFind;
	}
}
