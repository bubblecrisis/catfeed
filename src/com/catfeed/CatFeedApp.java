package com.catfeed;

import static com.catfeed.Constants.LOGTAG;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import utils.F;
import utils.Notifier;
import utils.Progress;
import android.app.Application;
import android.util.Log;

import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EApplication;

@EApplication
public class CatFeedApp extends Application {

	public Map<Long, Subscription> subscriptions = new HashMap();
	
	@Bean
	public Repository repository;
	
	private transient Notifier notifier = new Notifier();
	
	@Override
	public void onCreate() {
		loadSubscriptions();
	}
	
	public void loadSubscriptions() {
		for (Subscription sub: repository.all(Subscription.class)) {
			sub.calculateStatistics(this);
			subscriptions.put(sub._id, sub);
		}
	}

	/**
	 * Refreshes ALL subscriptions.
	 * @param item
	 */
	public void refreshAllSubscriptions(RssFeeder rss, Progress progress) {
    	
		Collection<String> urls = F.each(subscriptions.values(), new F.Function<Subscription, String>() {
			public String apply(Subscription subscription) {
				return subscription.url;
			}			
		});
		rss.refresh(progress, urls.toArray(new String[urls.size()]));
		calculateSubscriptionsStatistics();
	}
	
	private void calculateSubscriptionsStatistics() {
		for (Subscription subscription: subscriptions.values()) {
				subscription.calculateStatistics(this);
		}		
	}
	
	public void addSubscription(Subscription subscription) {
		subscriptions.put(subscription._id, subscription);
	}
	
	public Subscription getSubscription(Long id) {
		return subscriptions.get(id);
	}
	
	public void deleteSubscription(Subscription subscription) {
		repository.delete(WebFeed.class, "sub_id=?", String.valueOf(subscription._id));
		Log.d(LOGTAG, "Deleted WebFeed subscription id " + subscription._id);

		repository.delete(Subscription.class, subscription._id);
		Log.d(LOGTAG, "Deleted Subscription id " + subscription._id);
		subscriptions.remove(subscription._id);
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
