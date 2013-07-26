package com.catfeed.async.postevent;

import static com.catfeed.provider.CatFeedContentProvider.table;
import static utils.ContentUtils.createValues;

import java.util.ArrayList;
import java.util.List;

import utils.Dialog;
import utils.Progress;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.catfeed.async.RssAtomFeedRetriever;
import com.catfeed.async.WebContentDownloader;
import com.catfeed.async.RssAtomFeedRetriever.ReceivedFeed;
import com.catfeed.async.RssAtomFeedRetriever.RssAtomReceived;
import com.catfeed.constants.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;

//--------------------------------------------------------------------------------------------------
// Feed Refresh Callback
//--------------------------------------------------------------------------------------------------
public class FeedRefreshed implements RssAtomReceived {
	private Repository    repository;
	private Activity	  activity;
	private Progress      refreshing;
	
	public FeedRefreshed(Repository repository, Activity activity, Progress refreshing) {
		this.repository = repository;
		this.activity = activity;
		this.refreshing = refreshing;
	}
	
	@Override
	public void received(List<RssAtomFeedRetriever.ReceivedFeed> feeds) {
		if (feeds.size() == 0) {
			Dialog.error(activity, "Fetching feeds", "No response from server", null);
			if (refreshing != null) refreshing.stop();
			return;
		}
		
		List<Long> updatedSubscriptionIds = new ArrayList();
		for (ReceivedFeed feed: feeds) {
			updatedSubscriptionIds.add(updateFeed(feed));
		}
    	
    	// ----------------------------------------
    	// Download all contents (in WIFI)
    	// ----------------------------------------
    	ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    	for (Long subscriptionId: updatedSubscriptionIds) {
        	List<String> urls = WebFeed.findUncachedUrlsBySubscription(repository, subscriptionId);
        	Log.i(Constants.LOGTAG, "Found " + urls.size() + " URLs to cached for subscription: " + subscriptionId);
        	refreshing.countup();
        	WebContentDownloader downloader = new WebContentDownloader(repository, activity.getContentResolver(), connectivity, refreshing);
        	downloader.execute(urls.toArray(new String[urls.size()]));    		
    	}
    	
    	if (refreshing != null) refreshing.countdown();	
	}
	
	/**
	 * 
	 * @param receivedFeed
	 * @return List of subscription_id
	 */
	private Long updateFeed(RssAtomFeedRetriever.ReceivedFeed receivedFeed) {
		Log.i(Constants.LOGTAG, "Refreshing subscription " + receivedFeed.feedUrl);	
		
		// Return value. All receievdFeeds should belong to the same subscription.
		Long subscriptionId = null;
		
		// Because all the received feeds should belong to the same subscription, we
		// cache the first one when we find it out in the loop.
		Subscription subscription = null;
		
		SyndFeed feed = receivedFeed.syndFeed;
    	for (SyndEntry entry: (List<SyndEntry>) feed.getEntries()) {
    		// Synchronise WebFeed in local storage with the content fetched 
    		// from the RSS. If an entry of the same URL does not exist in
    		// the local storage, a new one is added.
    		
    		ContentResolver content = activity.getContentResolver();
    		WebFeed webfeed = WebFeed.findByUrl(repository, entry.getLink());
    		if (webfeed == null) {
    			
    			// Only query subscription if one does not exists. The subscription
    			// should be identical for all items in the receivedFeeds.
    			if (subscription == null) {
        			subscription = Subscription.findByUrl(repository, receivedFeed.feedUrl);    				
    			}
    			
    			webfeed = new WebFeed(entry, subscription._id);
	        	content.insert(table("webfeed"), createValues(webfeed));
	        	subscriptionId = subscription._id;
    		}
    		else {
    			webfeed.setSyndEntry(entry);
    			content.update(table("webfeed"), createValues(webfeed), "_id=?", new String[] { String.valueOf(webfeed._id) });
    			subscriptionId = webfeed.sub_id;
    		}    		
        } 
    	return subscriptionId; // All the webfeed should belong to the same subscription
	}

}