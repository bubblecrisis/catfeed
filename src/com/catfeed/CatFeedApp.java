package com.catfeed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import utils.F;
import android.app.Application;

import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EApplication;

@EApplication
public class CatFeedApp extends Application {

	public List<Subscription> subscriptions = new ArrayList();
	
	@Bean
	public Repository repository;
	
	@Bean
	public RssFeeder rss;
	
	public int noOfCachedArticles = 0;
	public int totalArticles = 0;
	public int unreadCount = 0;
	
	@Override
	public void onCreate() {
		subscriptions.addAll(repository.all(Subscription.class));
	}
	
	/**
	 * Refreshes ALL subscriptions.
	 * PROBLEM: Because we are doing multiple concurrent async job, we cannot properly reset
	 *          the spinning refresh button. Need to rethink this one.
	 * @param item
	 */
	public void refreshAllSubscriptions() {
    	
		Collection<String> urls = F.each(subscriptions, new F.Function<Subscription, String>() {
			public String apply(Subscription subscription) {
				return subscription.url;
			}			
		});
		
		rss.refresh(null, urls.toArray(new String[urls.size()]));
	}
}
