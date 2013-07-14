package com.catfeed.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import utils.CursorUtils;
import utils.Entity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.catfeed.async.RssAtomFeedRetriever;
import com.catfeed.db.Repository;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndCategory;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndContent;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;

@Entity
public class WebFeed {
	public Long _id;
	public Long sub_id;
	public String link;
	public String title;
	public String category;
	public String summary;
	public Long date;
	public Long dateread;
	public String contenttype;
	public String body;
	public Boolean cached;
	
	public WebFeed() {
	}    
		
	public WebFeed(SyndEntry entry, Long subscriptionId) {
    	this.sub_id = subscriptionId;
    	setSyndEntry(entry);
	}
	
	public void setSyndEntry(SyndEntry entry) {
		Date publishDate = entry.getPublishedDate();
		if (publishDate == null) publishDate = new Date();
		
    	this.date = publishDate.getTime();
    	this.title = entry.getTitle();
    	this.link = entry.getLink();
    	
    	SyndContent content = entry.getDescription();
    	if (content != null) {
        	this.summary = content.getValue();
        	this.contenttype = content.getType();    		
    	}
    	
    	List<SyndCategory> categories = entry.getCategories();
    	if (categories != null && categories.size() > 0) {
    		this.category =  categories.get(0).getName();
    	}
	}
	
	public static WebFeed findByUrl(Repository repository, String url) {
		return repository.findBy(WebFeed.class, "link=?", url);		
	}
	
	/**
	 * List WebFeed URLs that are not cached, for a subscription.
	 * @param repository
	 * @param sub_id
	 * @return
	 */
	public static List<String> findUncachedUrlsBySubscription(Repository repository, Long sub_id) {
		List<String> results = new ArrayList();
		SQLiteDatabase db = repository.getReadableDatabase();
		Cursor c = db.query("webfeed", new String[] { "link" }, "sub_id=? AND cached=0", new String[] { String.valueOf(sub_id) }, null, null, null);
		
		// Don't use isAfterLast(). http://stackoverflow.com/questions/7452850/will-an-empty-sqlite-cursor-return-true-for-isbeforefirst-isafterlast-both-or
		while (c.moveToNext()) {
			results.add(CursorUtils.getString(c, "link"));
		}
		db.close();
		return results;	
	}
	
	public static int count(Repository repository, Long sub_id) {
		SQLiteDatabase db = repository.getReadableDatabase();
		Cursor c = db.rawQuery("select count(*) from WebFeed where sub_id=?", new String[] { sub_id.toString() });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		db.close();
		return count;
	}

	public static int countUnread(Repository repository, Long id) {
		SQLiteDatabase db = repository.getReadableDatabase();
		Cursor c = db.rawQuery("select count(*) from WebFeed where sub_id=? AND dateread = 0", new String[] { id.toString() });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		db.close();
		return count;
	}

	public static int countCached(Repository repository, Long id) {
		SQLiteDatabase db = repository.getReadableDatabase();
		Cursor c = db.rawQuery("select count(*) from WebFeed where sub_id=? AND cached > 0", new String[] { id.toString() });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		db.close();
		return count;
	}	
	
	public void markAsRead(Repository repository) {
		SQLiteDatabase db = repository.getWritableDatabase();
		ContentValues values = new ContentValues(1);
		values.put("dateread", new Date().getTime());
		db.update(Repository.table(WebFeed.class), values, "_id=?", new String[] { String.valueOf(_id) } );
		db.close();
	}
}
