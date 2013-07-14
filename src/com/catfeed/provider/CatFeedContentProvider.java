package com.catfeed.provider;

import android.net.Uri;


public class CatFeedContentProvider extends BasicSQLContentProvider {

	public static final String AUTHORITY = "com.catfeed.provider.FeedData";
	
	public CatFeedContentProvider() {
		super(AUTHORITY, "subscription", "webfeed");
	}
	
	public static Uri table(String table) {
		String uri = new StringBuilder("content://")
		 	.append(AUTHORITY)
		 	.append("/")
		 	.append(table)
		 	.toString();
		return Uri.parse(uri);
	}

}
