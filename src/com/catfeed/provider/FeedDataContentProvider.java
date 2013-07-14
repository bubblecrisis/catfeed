/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.catfeed.provider;

import static android.provider.BaseColumns._ID;
import static com.catfeed.provider.FeedDataContentProvider.UriOption.SUBSCRIPTION;
import static com.catfeed.provider.FeedDataContentProvider.UriOption.SUBSCRIPTIONS;
import static com.catfeed.provider.FeedDataContentProvider.UriOption.WEBFEED;
import static com.catfeed.provider.FeedDataContentProvider.UriOption.WEBFEEDS;

import java.io.File;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.catfeed.db.Repository;
import com.catfeed.R;

public class FeedDataContentProvider extends ContentProvider {
	private static final String FOLDER = Environment.getExternalStorageDirectory()+"/catfeed/";
	
	private static final int URI_FEEDS = 1;
	
	private static final int URI_FEED = 2;
	
	private static final int URI_ENTRIES = 3;
	
	private static final int URI_ENTRY= 4;
	
	private static final int URI_ALLENTRIES = 5;
	
	private static final int URI_ALLENTRIES_ENTRY = 6;
	
	private static final int URI_FAVORITES = 7;
	
	private static final int URI_FAVORITES_ENTRY = 8;
	
	protected static final String TABLE_FEEDS = "feeds";
	
	private static final String TABLE_ENTRIES = "entries";
	
	private static final String ALTER_TABLE = "ALTER TABLE ";
	
	private static final String ADD = " ADD ";
	
	private static final String EQUALS_ONE = "=1";

	public static final String IMAGEFOLDER = Environment.getExternalStorageDirectory()+"/catfeed/images/"; // faster than FOLDER+"images/"
	
	public static final File IMAGEFOLDER_FILE = new File(IMAGEFOLDER);
	
	private static final String BACKUPOPML = Environment.getExternalStorageDirectory()+"/catfeed/backup.opml";
	

	
	private static final String[] PROJECTION_PRIORITY = new String[] {FeedData.FeedColumns.PRIORITY};

	private static String SQL_SUBSCRIPTION_CREATE_TABLE;
	private static String SQL_WEBFEED_CREATE_TABLE;

	//---------------------------------------------------------------------------------------------------------
	// Content URI Matcher
	//---------------------------------------------------------------------------------------------------------
	public static class ContentUri {
		public static Uri of(String path) {
			return (new ContentUri(path)).toUrl();
		}
		private StringBuilder buffer;
		
		public ContentUri() {
			buffer = init();
		}
		
		public ContentUri(String path) {
			buffer = init().append(path);
		}
		
		public ContentUri with(Object path) {
			buffer.append(path);
			return this;
		}
		
		public Uri toUrl() {
			return Uri.parse(buffer.toString());
		}
		
		private StringBuilder init() {
			return new StringBuilder("content://").append(FeedData.AUTHORITY);
		}
	}

	//---------------------------------------------------------------------------------------------------------
	// Content URI Matcher
	//---------------------------------------------------------------------------------------------------------

	enum UriOption {
		NO_MATCH,
		SUBSCRIPTIONS, SUBSCRIPTION, 
		WEBFEEDS, WEBFEED;
		
		public static UriOption valueOf(int option) {
			if (option == UriMatcher.NO_MATCH) {
				return NO_MATCH;
			}
			return values()[option];
		}
	}
	
	private static UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(FeedData.AUTHORITY, "subscriptions", SUBSCRIPTIONS.ordinal());
		URI_MATCHER.addURI(FeedData.AUTHORITY, "subscriptions/#", SUBSCRIPTION.ordinal());
		URI_MATCHER.addURI(FeedData.AUTHORITY, "subscriptions/#/feeds", WEBFEEDS.ordinal());
		URI_MATCHER.addURI(FeedData.AUTHORITY, "subscriptions/#/feeds/#",  WEBFEED.ordinal());
	}
	
	//---------------------------------------------------------------------------------------------------------
	// Content Provider Implementation
	//---------------------------------------------------------------------------------------------------------

	private String[] MAXPRIORITY = new String[] {"MAX("+FeedData.FeedColumns.PRIORITY+")"};

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {		
		UriOption option = UriOption.valueOf(URI_MATCHER.match(uri));
		
//		
//		if ((option == URI_FEED || option == URI_FEEDS) && sortOrder == null) {
//			sortOrder = FeedData.FEED_DEFAULTSORTORDER;
//		}
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		switch(option) {
			case SUBSCRIPTIONS : {
				queryBuilder.setTables(SUBSCRIPTION.name());
				break;
			}
			case SUBSCRIPTION : {
				queryBuilder.setTables(SUBSCRIPTION.name());
				queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(1)));
				break;
			}
			case WEBFEEDS : {
				queryBuilder.setTables(WEBFEED.name());
				queryBuilder.appendWhere(new StringBuilder("sub_id=").append(uri.getPathSegments().get(1)));
				break;
			}
			case WEBFEED : {
				queryBuilder.setTables(WEBFEED.name());
				queryBuilder.appendWhere(new StringBuilder(_ID).append('=').append(uri.getPathSegments().get(3)));
				break;
			}
			case NO_MATCH:
//			case URI_ALLENTRIES : {
//				queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
//				break;
//			}
//			case URI_FAVORITES_ENTRY : 
//			case URI_ALLENTRIES_ENTRY : {
//				queryBuilder.setTables(TABLE_ENTRIES);
//				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1)));
//				break;
//			}
//			case URI_FAVORITES : {
//				queryBuilder.setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
//				queryBuilder.appendWhere(new StringBuilder(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE));
//				break;
//			}
		}
		
		SQLiteDatabase database = databaseHelper.getReadableDatabase();
		Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
//		int option = URI_MATCHER.match(uri);
//		
//		String table = null;
//		
//		StringBuilder where = new StringBuilder();
//		
//		SQLiteDatabase database = databaseHelper.getWritableDatabase();
//		
//		switch(option) {
//			case URI_FEED : {
//				table = TABLE_FEEDS;
//			
//				final String feedId = uri.getPathSegments().get(1);
//				
//				new Thread() {
//					public void run() {
//						delete(FeedData.EntryColumns.CONTENT_URI(feedId), null, null);
//					}
//				}.start();
//				
//				where.append(FeedData.FeedColumns._ID).append('=').append(feedId);
//				
//				/** Update the priorities */
//				Cursor priorityCursor = database.query(TABLE_FEEDS, PROJECTION_PRIORITY, FeedData.FeedColumns._ID+"="+feedId, null, null, null, null);
//				
//				if (priorityCursor.moveToNext()) {
//					database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"-1 WHERE "+FeedData.FeedColumns.PRIORITY+" > "+priorityCursor.getInt(0));
//					priorityCursor.close();
//				} else {
//					priorityCursor.close();
//				}
//				break;
//			}
//			case URI_FEEDS : {
//				table = TABLE_FEEDS;
//				break;
//			}
//			case URI_ENTRY : {
//				table = TABLE_ENTRIES;
//				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
//				break;
//			}
//			case URI_ENTRIES : {
//				table = TABLE_ENTRIES;
//				where.append(FeedData.EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
//				break;
//			}
//			case URI_ALLENTRIES : {
//				table = TABLE_ENTRIES;
//				break;
//			}
//			case URI_FAVORITES_ENTRY : 
//			case URI_ALLENTRIES_ENTRY : {
//				table = TABLE_ENTRIES;
//				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
//				break;
//			}
//			case URI_FAVORITES : {
//				table = TABLE_ENTRIES;
//				where.append(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE);
//				break;
//			}
//		}
//		
//		if (!TextUtils.isEmpty(selection)) {
//			if (where.length() > 0) {
//				where.append(Strings.DB_AND);
//			}
//			where.append(selection);
//		}
//		
//		int count = database.delete(table, where.toString(), selectionArgs);
//		
//		if (table == TABLE_FEEDS) { // == is ok here
//			OPML.exportToFile(BACKUPOPML, database);
//		}
//		if (count > 0) {
//			getContext().getContentResolver().notifyChange(uri, null);
//		}
//		return count;
		return 0;
	}
	
	/**
	 * getType(Uri uri) will usually only be called after a call to ContentResolver#getType(Uri uri). 
	 * It is used by applications (either other third-party applications, if your ContentProvider has been exported,
	 *  or your own) to retrieve the MIME type of the given content URL. If your app isn't concerned with the data's 
	 *  MIME type, it's perfectly fine to simply have the method return null.
	 */
	@Override
	public String getType(Uri uri) {
		UriOption option = UriOption.valueOf(URI_MATCHER.match(uri));		
		switch(option) {
			case SUBSCRIPTIONS : return "vnd.android.cursor.dir/vnd.catfeed.subscription";
			case SUBSCRIPTION  : return "vnd.android.cursor.item/vnd.catfeed.subscription";
			case WEBFEEDS :      return "vnd.android.cursor.dir/vnd.catfeed.webfeed";
			case WEBFEED  :      return "vnd.android.cursor.item/vnd.catfeed.webfeed";
			default : throw new IllegalArgumentException("Unknown URI: "+uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long newId = -1;
		
		int option = URI_MATCHER.match(uri);
		
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		
		switch (option) {
			case URI_FEEDS : {
				Cursor cursor = database.query(TABLE_FEEDS, MAXPRIORITY, null, null, null, null, null, null);
				
				if (cursor.moveToNext()) {
					values.put(FeedData.FeedColumns.PRIORITY, cursor.getInt(0)+1);
				} else {
					values.put(FeedData.FeedColumns.PRIORITY, 1);
				}
				cursor.close();
				newId = database.insert(TABLE_FEEDS, null, values);
				OPML.exportToFile(BACKUPOPML, database);
				break;
			}
			case URI_ENTRIES : {
				values.put(FeedData.EntryColumns.FEED_ID, uri.getPathSegments().get(1));
				newId = database.insert(TABLE_ENTRIES, null, values);
				break;
			}
			case URI_ALLENTRIES : {
				newId = database.insert(TABLE_ENTRIES, null, values);
				break;
			}
			default : throw new IllegalArgumentException("Illegal insert");
		}
		if (newId > -1) {
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, newId);
		} else {
			throw new SQLException("Could not insert row into "+uri);
		}
	}

	/**
	 * Create application database folder and database
	 */
	@Override
	public boolean onCreate() {
		try {
			// Create database location directory
			File folder = new File(FOLDER);
			folder.mkdir();
		} catch (Exception e) {}
		databaseHelper = new Repository(getContext());
		return true;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int option = URI_MATCHER.match(uri);
		
		String table = null;
		
		StringBuilder where = new StringBuilder();
		
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		
		switch(option) {
			case URI_FEED : {
				table = TABLE_FEEDS;
				
				long feedId = Long.parseLong(uri.getPathSegments().get(1));
				
				where.append(FeedData.FeedColumns._ID).append('=').append(feedId);
				if (values != null && values.containsKey(FeedData.FeedColumns.PRIORITY)) {
					int newPriority = values.getAsInteger(FeedData.FeedColumns.PRIORITY);
					
					Cursor priorityCursor = database.query(TABLE_FEEDS, PROJECTION_PRIORITY, FeedData.FeedColumns._ID+"="+feedId, null, null, null, null);
				
					if (priorityCursor.moveToNext()) {
						int oldPriority = priorityCursor.getInt(0);
						
						priorityCursor.close();
						if (newPriority > oldPriority) {
							database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"-1 WHERE "+FeedData.FeedColumns.PRIORITY+" BETWEEN "+(oldPriority+1)+" AND "+newPriority);
						} else if (newPriority < oldPriority) {
							database.execSQL("UPDATE "+TABLE_FEEDS+" SET "+FeedData.FeedColumns.PRIORITY+" = "+FeedData.FeedColumns.PRIORITY+"+1 WHERE "+FeedData.FeedColumns.PRIORITY+" BETWEEN "+newPriority+" AND "+(oldPriority-1));
						}
					} else {
						priorityCursor.close();
					}
				}
				break;
			}
			case URI_FEEDS : {
				table = TABLE_FEEDS;
				// maybe this should be disabled
				break;
			}
			case URI_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(3));
				break;
			}
			case URI_ENTRIES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FEED_ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_ALLENTRIES: {
				table = TABLE_ENTRIES;
				break;
			}
			case URI_FAVORITES_ENTRY : 
			case URI_ALLENTRIES_ENTRY : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns._ID).append('=').append(uri.getPathSegments().get(1));
				break;
			}
			case URI_FAVORITES : {
				table = TABLE_ENTRIES;
				where.append(FeedData.EntryColumns.FAVORITE).append(EQUALS_ONE);				
				break;
			}
		}
		
		if (!TextUtils.isEmpty(selection)) {
			if (where.length() > 0) {
				where.append(Strings.DB_AND).append(selection);
			} else {
				where.append(selection);
			}
		}
		
		int count = database.update(table, values, where.toString(), selectionArgs);
		
		if (table == TABLE_FEEDS && (values.containsKey(FeedData.FeedColumns.NAME) || values.containsKey(FeedData.FeedColumns.URL) || values.containsKey(FeedData.FeedColumns.PRIORITY))) { // == is ok here
			OPML.exportToFile(BACKUPOPML, database);
		}
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	//---------------------------------------------------------------------------------------------------------
	// Database Helper
	//---------------------------------------------------------------------------------------------------------
	
	private Repository databaseHelper;

}
