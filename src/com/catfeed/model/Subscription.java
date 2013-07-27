package com.catfeed.model;

import utils.Entity;
import utils.Progress;
import android.content.ContentValues;

import com.catfeed.RssFeeder;
import com.catfeed.db.Repository;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;

@Entity
public class Subscription {

	public Long _id;
	public String url;
	public String title;
	public Integer dlimages = 1; /* true */
	public Integer dlpage = 1;   /* true */
	public Integer retain = 5;   /* defaults to 5 days */    

	public Subscription() {
	}
	
	public Subscription(String rssUrl, SyndFeed feed) {
		this.title = feed.getTitle();
		this.url = rssUrl;
	}

	public static Subscription findById(Repository repository, Long id) {
		return repository.findBy(Subscription.class, "_id=?", id.toString());		
	}
	
	public static Subscription findByUrl(Repository repository, String url) {
		return repository.findBy(Subscription.class, "url=?", url);		
	}
	
	/**
	 * Delete all {@link WebFeed} attached to this {@link Subscription}.
	 * @param repository
	 */
	public void clear(Repository repository) {
		repository.delete(WebFeed.class, "sub_id=?", this._id.toString());
	}
	
	/**
	 * Refresh a {@link Subscription} by deleting all the {@link WebFeed} and reloading a new
	 * set from {@link SyndFeed}.
	 * @param progress
	 * @param feed
	 */
	public void refresh(RssFeeder rss, Progress progress) {
		rss.refresh(progress, this.url);
	}
	
	/**
	 * Update a subscription image. The subscription image is not part of the instance
	 * because we don't want the bytes moving around unnecessarily.
	 * @param repository
	 * @param subscriptionId
	 * @param data
	 */
	public static void setImage(Repository repository, Long subscriptionId, byte[] data) {
		ContentValues values = new ContentValues(1);
		values.put("icon", data);
		repository.update(Subscription.class, subscriptionId, values);
	}

	/**
	 * Get a subscription image. The subscription image is not part of the instance
	 * because we don't want the bytes moving around unnecessarily.
	 * @param repository
	 * @param subscriptionId
	 * @param data
	 */
	public static byte[] getImage(Repository repository, Long subscriptionId) {
		return (byte[]) repository.valueOf(Subscription.class, subscriptionId, new String[] { "icon" } )[0];
	}
}
