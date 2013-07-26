package com.catfeed.db;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import utils.ContentUtils;
import utils.Entity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.catfeed.R;
import com.catfeed.activity.SubscriptionsActivity;
import com.catfeed.constants.Constants;

public class Repository extends SQLiteOpenHelper {

	// All Static variables
	private static final String FOLDER = Environment.getExternalStorageDirectory()+"/catfeed/";
	
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "catfeed.db";

	// SQL 
	private static String CREATE_SUBSCRIPTION;
	private static String CREATE_WEBFEED;
	
	private static Repository db;
	
	public Repository(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		//context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		
		CREATE_SUBSCRIPTION = context.getString(R.string.sql_subscription_create);
		CREATE_WEBFEED = context.getString(R.string.sql_webfeed_create);
	}
	
	public static Repository getRepository(Context context) {
		if (db == null) {
			db = new Repository(context);
		}
		return db;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		File databasePath = new File(FOLDER);
		try {
			// Create database location directory
			if (!databasePath.exists()) {
				databasePath.mkdir();				
			}
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, e.toString() + ": Error creating database in folder " + databasePath.getAbsolutePath());
		}
		
        db.beginTransaction();
        try {
            db.execSQL(CREATE_SUBSCRIPTION);
            db.execSQL(CREATE_WEBFEED);            
            db.setTransactionSuccessful();
        } catch (Exception e) {
           Log.e(Constants.LOGTAG, "Error creating database " + e.toString());
        } finally {
            db.endTransaction();
        }
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	/**
	 * Resolve the table name for a entity model class. See {@link Entity}.
	 * @param model
	 * @return
	 */
	public static String table(Class model) {
		Entity entityAnnotation = (Entity) model.getAnnotation(Entity.class);
		if (entityAnnotation == null || entityAnnotation.table().equals("")) {
			return model.getSimpleName();
		}
		else {
			return entityAnnotation.table();
		}
	}

	/**
	 * Retrieve all records.
	 * @param modelClass
	 * @return
	 */
	public <T> List<T> all(Class<T> modelClass) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(table(modelClass), columns(modelClass), null, new String[]{}, null, null, null);
		List<T> models = new ArrayList<T>();
		if (c.moveToFirst()) {
			while(!c.isAfterLast()) {
				models.add(populateModel(modelClass, c));
				c.moveToNext();
			}
		}
		c.close();
		db.close();
		return models;
	}
	
	
	/**
	 * Add a model into SQLITE database. See {@link Entity}.
	 * @param model
	 * @return last inserted rowid.
	 */
	public long add(Object model) {
		String table = table(model.getClass());
		ContentValues values = ContentUtils.createValues(model);
		
		// insert() returns the rowid (autoincrement column)
		// According to the doc returns -1 if error, but it returns 0 instead.
		SQLiteDatabase db = this.getWritableDatabase();
		long id = db.insert(table, null, values);
		db.close();
		return id;
	}
	
	/**
	 * Update a model into SQLITE database. See {@link Entity}.
	 * @param model
	 * @param model _id value
	 * @return last inserted rowid.
	 */
	public long update(Object model, Long id) {
		String table = table(model.getClass());
		ContentValues values = ContentUtils.createValues(model);
		
		// insert() returns the rowid (autoincrement column)
		// According to the doc returns -1 if error, but it returns 0 instead.
		SQLiteDatabase db = this.getWritableDatabase();
		long rowid = db.update(table, values, "_id=?", new String[] { id.toString() });
		db.close();
		return rowid;
	}
	
	
	public String[] columns(Class modelClass) {
		Field[] fields = modelClass.getDeclaredFields();
		String[] columns = new String[fields.length];
		int i = 0;
		for (Field f: fields) {
			columns[i++] = f.getName();
		}
		return columns;
	}

	private <T> T populateModel(Class<T> modelClass, Cursor c) {
		int i = 0;
		try {
			T model = modelClass.newInstance();
			for (Field f: modelClass.getDeclaredFields()) {
				if   (f.getType().equals(String.class)) {
					f.set(model, c.getString(i));
				}
				else if (f.getType().equals(Long.class)) {
					f.set(model, c.getLong(i));
				}
				else if (f.getType().equals(Integer.class)) {
					f.set(model, c.getInt(i));
				}
				else if (f.getType().equals(Date.class)) {
					f.set(model, new Date(c.getLong(i)));
				}
				else if (f.getType().equals(Boolean.class)) {
					Integer intValue = c.getInt(i);
					f.set(model, (intValue != null && intValue > 0));
				}
				else {
					Log.e(Constants.LOGTAG, "Repository.populateModel(): Cannot automatically assign value to field " + f.getName());
				}
				i++;
			}
			return model;			
		}
		catch(Exception e) {
			Log.e(Constants.LOGTAG, "Repository.populateModel():" + e.toString());
			return null;
		}
	}
	
	public <T> T findById(Class<T> modelClass, Long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(table(modelClass), columns(modelClass), "_id=" + id, new String[]{}, null, null, null);
		T model = null;
		if (c.moveToFirst()) {
			model = populateModel(modelClass, c);
		}
		c.close();
		db.close();
		return model;		
	}

	public <T> T findBy(Class<T> modelClass, String selection, String... criteria) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(table(modelClass), columns(modelClass), selection, criteria, null, null, null);
		T model = null;
		if (c.moveToFirst()) {
			model = populateModel(modelClass, c);
		}
		db.close();
		return model;		
	}	
	

	/**
	 * Retrieve values (as Object[]) from a model. You will have to know exactly what types
	 * are the field names.
	 * @param modelClass
	 * @param id
	 * @param fieldnames
	 * @return
	 */
	public Object[] valueOf(Class modelClass, Long id, String[] fieldnames) {
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(table(modelClass), fieldnames, "_id=" + id, new String[]{}, null, null, null);
		Object[] values = new Object[fieldnames.length];
		if (c.moveToFirst()) {
			for (int i=0; i<fieldnames.length; i++) {
				int col = c.getColumnIndex(fieldnames[i]);
				int type = c.getType(col);
				switch (type) {
				case Cursor.FIELD_TYPE_STRING: values[i] = c.getString(col); break;
				case Cursor.FIELD_TYPE_INTEGER: values[i] = c.getInt(col); break;
				case Cursor.FIELD_TYPE_FLOAT: values[i] = c.getFloat(col); break;
				case Cursor.FIELD_TYPE_BLOB: values[i] = c.getBlob(col); break;
				case Cursor.FIELD_TYPE_NULL: values[i] = null; break;
				}
			}
		}
		db.close();
		return values;		
	}	
//
//	public <T> List<T> list(Class<T> modelClass, String selection, String... criteria) {
//		List<T> results = new ArrayList();
//		SQLiteDatabase db = this.getReadableDatabase();
//		Cursor c = db.query(table(modelClass), columns(modelClass), selection, criteria, null, null, null);
//		T model = null;
//		c.moveToFirst();
//		// Don't use isAfterLast(). http://stackoverflow.com/questions/7452850/will-an-empty-sqlite-cursor-return-true-for-isbeforefirst-isafterlast-both-or
//		while (c.moveToNext()) {
//			System.err.println(c.getPosition() + " isLast " + c.isLast() + " isAfterLast " + c.isAfterLast());
//			results.add(populateModel(modelClass, c));
//		}
//		db.close();
//		return results;		
//	}	
	
	public void delete(Class modelClass, Long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(table(modelClass), "_id=" + id, new String[] {});
		db.close();		
	}
	
	public void delete(Class modelClass, String selection, String... criteria) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(table(modelClass), selection, criteria);
		db.close();		
	}	
	
	/**
	 * Update arbitrary fields into a single table.
	 * @param modelClass
	 * @param id
	 * @param fieldValues
	 */
	public void update(Class modelClass, Long id, ContentValues values) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.update(table(modelClass), values, "_id=" + id, new String[] {});
		db.close();		
	}	
	
	public Integer count(Class modelClass) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery("select count(*) from " + table(modelClass), new String[] {});
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		db.close();
		return count;
	}
	
	public Integer countBy(Class modelClass, String where, String... criteria) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery("select count(*) from " + table(modelClass) + " where " + where, criteria);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		db.close();
		return count;
	}
}