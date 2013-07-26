package com.catfeed.async.postevent;

import static com.catfeed.constants.Constants.LOGTAG;

import java.util.List;

import utils.Dialog;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.catfeed.activity.WebFeedsActivity_;
import com.catfeed.async.RssAtomFeedRetriever.ReceivedFeed;
import com.catfeed.async.RssAtomFeedRetriever.RssAtomReceived;
import com.catfeed.async.SubscriptionIconDownloader;
import com.catfeed.constants.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.catfeed.provider.CatFeedContentProvider;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndImage;

public class Subscribed implements RssAtomReceived {
	
	/**
	 * 
	 */
	private Repository repository;
	private Activity   activity;
	
	public Subscribed(Repository repository, Activity activity) {
		this.repository = repository;
		this.activity = activity;
	}
	
	@Override
	public void received(List<ReceivedFeed> feeds) {
		if (feeds == null || feeds.size() == 0) {
			Dialog.error(activity, "Subscribing", "No response from server", null);
			return;
		}			
		
		ReceivedFeed feed = feeds.get(0);
	    Subscription subscription = new Subscription(feed.feedUrl, feed.syndFeed);		
	    
	    // Check if there is already a copy in the database
		Subscription exists = Subscription.findByUrl(repository, subscription.url);
		if (exists == null) {
			// Subscription id is -1 or 0 if there there is an error, including
	        // duplicated subscription.
			subscription._id = repository.add(subscription);  
			
			activity.getContentResolver().notifyChange(CatFeedContentProvider.table(Subscription.class.getName()), null);
			
			// Fetch subscription image
			SyndImage image = feed.syndFeed.getImage();
			if (image != null) {
				Log.d(Constants.LOGTAG, "Feed " + feed.feedUrl + " contains image " + image.getUrl());
				SubscriptionIconDownloader subscribeIcon = new SubscriptionIconDownloader(repository, subscription._id);
				subscribeIcon.execute(image.getUrl());				
			}
			
	    	// Only add feeds if subscription is added successfully
	    	if (subscription._id > 0) {
	    		try {
		        	for (SyndEntry entry: (List<SyndEntry>) feed.syndFeed.getEntries()) {
		            	WebFeed webfeed = new WebFeed(entry, subscription._id);
		            	repository.add(webfeed);
		            }        	        			    			
	    		}
	    		catch(Exception e) {
	    			Log.e(Constants.LOGTAG, "Error adding subscription webfeed " + subscription.title, e);
	    		}
	    	}
		}	
		else {
			Log.i(LOGTAG, "Subscription " + subscription.url + " already exists");
			subscription = exists;									
		}				

		//activity.getLoaderManager().restartLoader(0, null, loaderCallback);
		Intent intent = new Intent(activity, WebFeedsActivity_.class);
	    intent.putExtra(Constants.SUBSCRIPTION_ID, subscription._id);
	    activity.startActivity(intent);
	}
}