package com.richard.officenavigation.findpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import com.richard.officenavigation.dao.INode;
import com.richard.officenavigation.dao.IPath;

public class Dijkstra {
	private INode mNodeFrom;
	private boolean mNeedCompute;
	private List<INode> mNodesAll;
	private PriorityQueue<INode> mNodeQueue;

	public Dijkstra(List<INode> nodes) {
		mNodesAll = nodes;
		mNeedCompute = true;
		mNodeQueue = new PriorityQueue<INode>();
	}
	
	public void changeNodes(List<INode> nodes) {
		mNodesAll = nodes;
	}

	public void setFrom(INode from) {
		if (mNodeFrom != from) {
			mNodeFrom = from;
			mNeedCompute = true;
		}
	}

	private void init() {
		for (INode node : mNodesAll) {
			node.minDistance = Double.POSITIVE_INFINITY;
		}
		mNodeQueue.clear();
		mNodeFrom.minDistance = 0.;
		mNodeFrom.previous = null;
		mNodeQueue.add(mNodeFrom);
	}

	public void computePaths() {
		if (!mNeedCompute)
			return;

		init();
		while (!mNodeQueue.isEmpty()) {
			INode u = mNodeQueue.poll();

			for (IPath e : u.getAdjacencies()) {
				INode v = e.getTarget();
				double weight = e.getDistance();
				double distanceThroughU = u.minDistance + weight;
				if (distanceThroughU < v.minDistance) {
					mNodeQueue.remove(v);

					v.minDistance = distanceThroughU;
					v.previous = u;
					mNodeQueue.add(v);
				}
			}
		}
	}

	public List<INode> getShortestPathTo(INode target) {
		List<INode> path = new ArrayList<INode>();
		for (INode node = target; node != null; node = node.previous)
			path.add(node);
		Collections.reverse(path);
		return path;
	}
}
