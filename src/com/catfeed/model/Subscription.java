package com.catfeed.model;

import utils.Entity;
import utils.Progress;
import android.content.ContentValues;

import com.catfeed.CatFeedApp;
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

	//------------------------------------------------------------------------
	// Transients Page Statistics
	//------------------------------------------------------------------------
	private transient CatFeedApp application;
	private transient int noOfCachedArticles = 0;
	private transient int totalArticles = 0;
	private transient int unreadCount = 0;
		
	public Subscription() {
	}
	
	public Subscription(CatFeedApp application, String rssUrl, SyndFeed feed) {
		this.application = application;
		this.title = feed.getTitle();
		this.url = rssUrl;
	}

	public static Subscription findById(CatFeedApp application, Long id) {
		Subscription subscription = application.repository.findBy(Subscription.class, "_id=?", id.toString());	
		if (subscription != null) {
			subscription.calculateStatistics(application);
			subscription.application = application;		
			application.addSubscription(subscription);  // replaces existing instance
		}
		return subscription;
	}
	
	public static Subscription findByUrl(CatFeedApp application, String url) {
		Subscription subscription =  application.repository.findBy(Subscription.class, "url=?", url);		
		if (subscription != null) {
			subscription.calculateStatistics(application);
			subscription.application = application;			
			application.addSubscription(subscription);  // replaces existing instance
		}
		return subscription;
	}
	
	/**
	 * Delete all {@link WebFeed} attached to this {@link Subscription}.
	 * @param repository
	 */
	public void clear(Repository repository) {
		repository.delete(WebFeed.class, "sub_id=?", this._id.toString());
		noOfCachedArticles = 0;
		totalArticles = 0;
		unreadCount = 0;
		application.setChanged();
		application.notifyObservers();
	}
	
	/**
	 * Refresh a {@link Subscription} by deleting all the {@link WebFeed} and reloading a new
	 * set from {@link SyndFeed}.
	 * @param progress
	 * @param feed
	 */
	public void refresh(RssFeeder rss, Progress progress) {
		rss.refresh(progress, this.url);
		application.notifyObservers();
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
	
	@Override
	public boolean equals(Object o) {
		return _id.equals(((Subscription) o)._id);
	}

	@Override
	public int hashCode() {
		return _id.hashCode();
	}
	
	//------------------------------------------------------------------------------------------------------
	// Page Statistics
	//------------------------------------------------------------------------------------------------------
	
	public void calculateStatistics(CatFeedApp application) {
		this.application = application;
		noOfCachedArticles = WebFeed.countCached(application.repository, _id);
		totalArticles = WebFeed.count(application.repository, _id);
		unreadCount = WebFeed.countUnread(application.repository, _id);
	}
	
	public void resetUnreadCount() {
		unreadCount = totalArticles;
	}
	
	public void increaseArticlesBy(int increment) {
		totalArticles += increment;
		application.setChanged();
	}

	public void increaseCachedBy(int increment) {
		noOfCachedArticles += increment;
		application.setChanged();
	}	
	
	public void readArticleBy(int increment) {
		unreadCount -= increment;
		application.setChanged();
	}	
	
	public int getCachedPercentage() {
		return (noOfCachedArticles * 100)/ totalArticles;
	}
	
	public int getNoOfCachedArticles() {
		return noOfCachedArticles;
	}

	public int getTotalArticles() {
		return totalArticles;
	}

	public int getUnreadCount() {
		return unreadCount;
	}


}
