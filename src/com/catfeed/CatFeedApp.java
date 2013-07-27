package com.catfeed;

import static com.catfeed.Constants.LOGTAG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observer;

import utils.F;
import utils.Notifier;
import utils.Progress;
import android.app.Application;
import android.util.Log;
import android.view.View;

import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EApplication;

@EApplication
public class CatFeedApp extends Application {

	public List<Subscription> subscriptions = new ArrayList();
	
	@Bean
	public Repository repository;
	
	@Bean
	public RssFeeder rss;
	
	private transient Notifier notifier = new Notifier();
	
	@Override
	public void onCreate() {
		loadSubscriptions();
	}
	
	public void loadSubscriptions() {
		subscriptions.addAll(repository.all(Subscription.class));
		calculateSubscriptionsStatistics();
	}
	
	/**
	 * Refreshes ALL subscriptions.
	 * PROBLEM: Because we are doing multiple concurrent async job, we cannot properly reset
	 *          the spinning refresh button. Need to rethink this one.
	 * @param item
	 */
	public void refreshAllSubscriptions(Progress progress) {
    	
		Collection<String> urls = F.each(subscriptions, new F.Function<Subscription, String>() {
			public String apply(Subscription subscription) {
				return subscription.url;
			}			
		});
		
		rss.refresh(progress, urls.toArray(new String[urls.size()]));
		calculateSubscriptionsStatistics();
	}
	
	private void calculateSubscriptionsStatistics() {
		F.each(subscriptions, new F.Function<Subscription, Void>() {
			public Void apply(Subscription subscription) {
				subscription.calculateStatistics(repository);
				return null;
			}			
		});		
	}
	
	public void deleteSubscription(Subscription subscription) {
		repository.delete(WebFeed.class, "sub_id=?", String.valueOf(subscription._id));
		Log.d(LOGTAG, "Deleted WebFeed subscription id " + subscription._id);

		repository.delete(Subscription.class, subscription._id);
		Log.d(LOGTAG, "Deleted Subscription id " + subscription._id);
		boolean removed = subscriptions.remove(subscription);
	}
	
	//------------------------------------------------------------------------------------------------------
	// Observer Pattern
	//------------------------------------------------------------------------------------------------------
	
	public void addObserver(Observer o) {
		notifier.addObserver(o);
	}
	
	public void notifyObservers() {
		notifier.notifyObservers();
	}
	
	public void setChanged() {
		notifier.setChanged();
	}
}
