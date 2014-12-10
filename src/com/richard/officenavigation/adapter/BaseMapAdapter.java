package com.richard.officenavigation.adapter;

public interface BaseMapAdapter {
	int ASSETS_MAP = 0;
	int FS_MAP = 1;

	String getSrc();

	Long getHeight();

	Long getWidth();

	Long getId();

	int getType();
}
