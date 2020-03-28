package com.jw.stepcounter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class GyroDBAdapter {
	private static final String DATABASE_NAME = "gyro.db";
	private static final String DATABASE_TABLE = "tb_gyro";

	public static final int DATABASE_VERSION = 3;
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + GyroEntry.Gyro_IDX
			+ " INTEGER primary key, " +  GyroEntry.Gyro_NAME
			+ " TEXT not null, " + GyroEntry.GYRO_X
			+ " INTEGER not null, " + GyroEntry.GYRO_Y
			+ " INTEGER not null, " + GyroEntry.GYRO_Z
			+ " INTEGER not null, " + GyroEntry.GYRO_FLAG
			+ " TEXT not null, " + GyroEntry.GYRO_TIME
            + " TEXT not null, " + GyroEntry.GYRO_STEP
            + " TEXT not null, " + GyroEntry.GYRO_INTERVAL
            + " TEXT not null, " + GyroEntry.GYRO_SPEED
			+ " TEXT not null);";
	private static final String TAG = "BoothDBAdapter";

	public String[] COLUMNS = new String[] {GyroEntry.Gyro_NAME, GyroEntry.GYRO_X, GyroEntry.GYRO_Y,
				GyroEntry.GYRO_Z, GyroEntry.GYRO_FLAG, GyroEntry.GYRO_TIME, GyroEntry.GYRO_STEP, GyroEntry.GYRO_INTERVAL, GyroEntry.GYRO_SPEED
			};
	private String[] CountCOLUMNS = new String[] {"count(idx)"
			};
	private Context mContext;
	private GYRODBHelper mDbHelper;
	private SQLiteDatabase mDb;

	public GyroDBAdapter(Context context) {
		mContext = context;
	}

	public GyroDBAdapter open() throws SQLException {
		mDbHelper = new GYRODBHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		if(mDbHelper!=null)
			mDbHelper.close();
	}
	
	
	public long createEntry(String strNAME, String strX, String strY, String strZ, String strFlag,String strTIme,String strStep,String strInterval,String strSpeed) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(GyroEntry.Gyro_NAME, strNAME);
		initialValues.put(GyroEntry.GYRO_X, strX);
		initialValues.put(GyroEntry.GYRO_Y, strY);
		initialValues.put(GyroEntry.GYRO_Z, strZ);
		initialValues.put(GyroEntry.GYRO_FLAG, strFlag);
		initialValues.put(GyroEntry.GYRO_TIME, strTIme);
        initialValues.put(GyroEntry.GYRO_STEP, strStep);
        initialValues.put(GyroEntry.GYRO_INTERVAL, strInterval);
        initialValues.put(GyroEntry.GYRO_SPEED, strSpeed);


		
		
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public long updateEntry(String strIdx, String strBtn_Idx, String strNo)  {
		ContentValues initialValues = new ContentValues();

		initialValues.put(GyroEntry.Gyro_NAME, strBtn_Idx);
		initialValues.put(GyroEntry.GYRO_X, strNo);


		return mDb.update(DATABASE_TABLE, initialValues, GyroEntry.Gyro_IDX + " = " + strIdx, null);

	}

	

	public Cursor selectIDXEntry(String strIdx) {
		//
		Cursor qu = mDb.query(DATABASE_TABLE, COLUMNS,
				GyroEntry.Gyro_IDX+" = "+strIdx,
				null, null, null, null);

		return qu;

	}
	public Cursor selectBtnEntry(String strBtn_Idx) {
		//
		Cursor qu = mDb.query(DATABASE_TABLE, COLUMNS,
				GyroEntry.Gyro_NAME +" = "+strBtn_Idx,
				null, null, null, null);

		return qu;

	}
	public Cursor fetchAllEntry(String fileName) {
		return mDb.query(DATABASE_TABLE, COLUMNS, GyroEntry.Gyro_NAME +" = '"+fileName+"'", null, null, null, null);
	}

	
	public Cursor fetchAllEntry() {
		return mDb.query(DATABASE_TABLE, COLUMNS, null, null, null, null, null);
	}
	public Cursor fetchAllEntryASC() {
		return mDb.query(DATABASE_TABLE, COLUMNS, null, null, null, null, GyroEntry.Gyro_IDX+" asc");
	}
	
	
	public int fetchAllEntryLength() {
		return mDb.query(DATABASE_TABLE, COLUMNS, null, null, null, null, null).getCount();
	}
	
	public void delIDXEntry(String strIdx) {
		mDb.delete(DATABASE_TABLE, GyroEntry.Gyro_IDX+"= "+strIdx, null);
	}

	public void delALLEntry() {
		mDb.delete(DATABASE_TABLE, null, null);
	}

	private class GYRODBHelper extends SQLiteOpenHelper {

		public GYRODBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destory all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

	}


	public class GyroEntry implements BaseColumns {
		public static final String Gyro_IDX = "idx";
		public static final String Gyro_NAME = "gyro_name";
		public static final String GYRO_X = "gyro_x";
		public static final String GYRO_Y = "gyro_y";
		public static final String GYRO_Z = "gyro_z";
		public static final String GYRO_FLAG = "gyro_flag";
		public static final String GYRO_TIME = "gyro_time";
        public static final String GYRO_STEP = "gyro_step";
        public static final String GYRO_INTERVAL = "gyro_interval";
        public static final String GYRO_SPEED = "gyro_speed";


	}
	
}
