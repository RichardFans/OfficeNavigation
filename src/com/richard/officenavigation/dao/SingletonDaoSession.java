package com.richard.officenavigation.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.richard.officenavigation.Constants.C;
import com.richard.officenavigation.dao.DaoMaster.DevOpenHelper;

public class SingletonDaoSession {
	private static DaoSession mDaoSession;
	private static SQLiteDatabase mDb;

	private SingletonDaoSession(Context context) {
		DevOpenHelper helper = new DaoMaster.DevOpenHelper(context,
				C.DB_NAME, null);
		mDb = helper.getWritableDatabase();
		DaoMaster daoMaster = new DaoMaster(mDb);
		mDaoSession = daoMaster.newSession();
	}

	public static DaoSession getInstance(Context context) {
		if (mDaoSession == null) {
			synchronized (SingletonDaoSession.class) {
				new SingletonDaoSession(context);
			}
		}
		return mDaoSession;
	}
	
	public static SQLiteDatabase getDb(Context context) {
		if (mDb == null) {
			synchronized (SingletonDaoSession.class) {
				new SingletonDaoSession(context);
			}
		}
		return mDb;
	}
}
