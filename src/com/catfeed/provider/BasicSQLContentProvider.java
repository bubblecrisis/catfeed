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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.catfeed.db.Repository;

/**
 * This is a {@link ContentProvider} that provides some basic {@link SQLiteDatabase} implementations
 * to get functionality quickly. Use this by extending it and defining
 * the {@link UriMatcher} and matching database table names by one
 * of the Constructor.
 * 
 * This content provider basically map a table name to a URI. Eg.
 * 
 * <code>
 * public class MyContentProvider extends AbstractContentProvider {
 * public MyContentProvider() {
 * 		super(AUTHORITY, "myTable1", "myTable2");
 * 	}
 * }
 * </code>
 * 
 * The AUTHORITY is the first argument of the constructor, and must
 * match 'android:authorities' declared in the provider section of the manifest.
 * 
 */
public abstract class BasicSQLContentProvider extends ContentProvider  {

	protected String authority;
	protected Repository databaseHelper;
	protected UriMatcher uriMatcher;
	protected List<String> tables = new ArrayList<String>();
		
	public BasicSQLContentProvider(String authority, UriMatcher uriMatcher, List<String> tables) {
		this.authority = authority;
		this.uriMatcher = uriMatcher;
		this.tables = tables;
	}
	
	public BasicSQLContentProvider(String authority, String... tableUrl) {
		this.authority = authority;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		for (int i=0; i < tableUrl.length; i++) {
			uriMatcher.addURI(authority, tableUrl[i], i);		
			tables.add(tableUrl[i]);
		}
	}
	
	public BasicSQLContentProvider(String authority, Map<String, String> tableUrl) {
		this.authority = authority;
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		int i = 0;
		for (String url: tableUrl.keySet()) {
			uriMatcher.addURI(authority, url, i);		
			tables.add(tableUrl.get(url));
			i++;
		}
	}
	
	//---------------------------------------------------------------------------------------------------------
	// URI Helper
	//---------------------------------------------------------------------------------------------------------
	public static Uri uri(String authority, String table) {
		String uri = new StringBuilder("content://")
		 	.append(authority)
		 	.append("/")
		 	.append(table)
		 	.toString();
		return Uri.parse(uri);
	}
	
	public String getTable(Uri uri) {
		try {
			int option = this.uriMatcher.match(uri);
			if (option == UriMatcher.NO_MATCH) {
				Log.e("BasicSQLContentProvider", "getTable(): No matching Url. url=" + uri);
				return null;
			}
			return this.tables.get(option);						
		}
		catch(Exception e) {
			Log.e("BasicSQLContentProvider", e.toString() + ":getTable(): url=" + uri, e);
			return null;
		}
	}
	
	//---------------------------------------------------------------------------------------------------------
	// Content Provider Implementation
	//---------------------------------------------------------------------------------------------------------

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {		
		String table = getTable(uri);

		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(table);
		
		SQLiteDatabase database = databaseHelper.getReadableDatabase();
		Cursor cursor = query.query(database, projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		String table = getTable(uri);
		
		int count = database.delete(table, selection, selectionArgs);
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}
	
	/**
	 * getType(Uri uri) will usually only be called after a call to ContentResolver#getType(Uri uri). 
	 * It is used by applications (either other third-party applications, if your ContentProvider has been exported,
	 *  or your own) to retrieve the MIME type of the given content URL. If your app isn't concerned with the data's 
	 *  MIME type, it's perfectly fine to simply have the method return null.
	 */
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long newId = -1;
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		newId = database.insert(getTable(uri), null, values);
		database.close();	
		
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
		databaseHelper = new Repository(getContext());
		return true;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		String table = getTable(uri);
		
		int count = database.update(table, values, selection, selectionArgs);
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}


}
