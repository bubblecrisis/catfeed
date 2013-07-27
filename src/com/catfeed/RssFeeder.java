package com.catfeed;

import static com.catfeed.Constants.LOGTAG;
import static com.catfeed.provider.CatFeedContentProvider.table;
import static utils.ContentUtils.createValues;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import utils.Dialog;
import utils.Progress;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.catfeed.activity.WebFeedsActivity_;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.catfeed.provider.CatFeedContentProvider;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndImage;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.FeedFetcher;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;
import com.googlecode.androidannotations.annotations.UiThread;

@EBean
public class RssFeeder {
	@RootContext
	Context context;

	@RootContext
	// Only injected if the root context is an activity
	Activity activity;
	
	@App
	CatFeedApp application;
	
	@Bean
	WebPageFeeder webPageFeeder;
	
	@Bean
	Repository repository;

	/**
	 * Refresh RSS feed
	 * @param progress
	 * @param feedUrl
	 */
	@Background
	public void refresh(Progress progress, String... feedUrl) {
		try {
			List<ReceivedFeed> feeds = retrieveFeeds(feedUrl);
			if (feeds.size() == 0) {
				Dialog.error(activity, "Fetching feeds", "No response from server", null);
				return;
			}

			List<Long> updatedSubscriptionIds = new ArrayList();
			for (ReceivedFeed feed : feeds) {
				updatedSubscriptionIds.add(updateFeed(feed));
			}

			// ----------------------------------------
			// Download all contents (in WIFI)
			// ----------------------------------------
			if (progress != null) progress.setCountDown(updatedSubscriptionIds.size());
			for (Long subscriptionId : updatedSubscriptionIds) {
				List<String> urls = WebFeed.findUncachedUrlsBySubscription(repository, subscriptionId);
				Log.i(Constants.LOGTAG, "Found " + urls.size() + " URLs to cached for subscription: " + subscriptionId);
				webPageFeeder.fetchPage(activity.getContentResolver(), urls.toArray(new String[urls.size()]));
				if (progress != null) progress.countdown();
			}		
		}
		finally {
			if (progress != null) progress.stop();	
		}
	}

	/**
	 * Subscribe to a new RSS subscription.
	 * @param progress
	 * @param feedUrl
	 */
	@Background
	public void subscribe(Progress progress, String... feedUrl) {
		List<ReceivedFeed> feeds = retrieveFeeds(feedUrl);
		if (feeds == null || feeds.size() == 0) {
			Dialog.error(activity, "Subscribing", "No response from server", null);
			return;
		}			
		
		ReceivedFeed feed = feeds.get(0);
	    Subscription subscription = new Subscription(application, feed.feedUrl, feed.syndFeed);		
	    
	    // Check if there is already a copy in the database
		Subscription exists = Subscription.findByUrl(application, feed.feedUrl);
		if (exists == null /* does not exists */) {
			// Subscription id is -1 or 0 if there there is an error, including
	        // duplicated subscription.
			subscription._id = repository.add(subscription);  
			activity.getContentResolver().notifyChange(CatFeedContentProvider.table(Subscription.class.getName()), null);
			
			// Fetch subscription image
			SyndImage image = feed.syndFeed.getImage();
			if (image != null) {
				Log.d(Constants.LOGTAG, "Feed " + feed.feedUrl + " contains image " + image.getUrl());
				fetchIcon(subscription._id, image.getUrl());
			}
			
	    	// Only add feeds if subscription is added successfully
	    	if (subscription._id > 0) {
	    		try {
		        	for (SyndEntry entry: (List<SyndEntry>) feed.syndFeed.getEntries()) {
		            	WebFeed webfeed = new WebFeed(entry, subscription._id);
		            	repository.add(webfeed);
		            	subscription.increaseArticlesBy(1);
		            }        	        			    			
	    		}
	    		catch(Exception e) {
	    			Log.e(Constants.LOGTAG, "Error adding subscription webfeed " + subscription.title, e);
	    		}
	    		finally {
	    			application.notifyObservers();
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

	public static Set<String> VALID_CONTENT_TYPE = new HashSet();
	static {
		VALID_CONTENT_TYPE.add("image/gif");
		VALID_CONTENT_TYPE.add("image/jpeg");
		VALID_CONTENT_TYPE.add("image/png");
	}
	
	@Background
	public void downloadIcon(Long subscriptionId, String url) {
		fetchIcon(subscriptionId, url);
	}
	
	/**
	 * Retrieve list of RSS feeds
	 * @param feedUrl
	 * @return
	 */
	private List<ReceivedFeed> retrieveFeeds(String... feedUrl) {
		List<ReceivedFeed> receivedFeeds = new ArrayList<ReceivedFeed>(
				feedUrl.length);

		try {
			FeedFetcher feedFetcher = new HttpURLFeedFetcher();
			for (String url : feedUrl) {
				ReceivedFeed receivedFeed = new ReceivedFeed();
				receivedFeed.feedUrl = url;
				receivedFeed.syndFeed = feedFetcher.retrieveFeed(new URL(url));
				receivedFeeds.add(receivedFeed);
			}

		} catch (Exception e) {
			Log.e(Constants.LOGTAG, "fetchRecentNews: " + e.toString());
			// Probably want to prompt error
		}
		return receivedFeeds;
	}
	
	private void fetchIcon(Long subscriptionId, String url) {
		try {
			Log.i(getClass().getName(), "Downloading image " + url);	
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(url));
			
			HttpEntity entity = response.getEntity();
			Header contentTypeHeader = entity.getContentType();
			String contentType = contentTypeHeader.getValue();
			if (VALID_CONTENT_TYPE.contains(contentType)) {
				InputStream in = entity.getContent();			
				byte[] data = IOUtils.toByteArray(in);
				Subscription.setImage(repository, subscriptionId, data);				
			}
			else {
				Log.w(getClass().getName(), "Invalid content type " + contentType);
			}
			
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, e.toString() + ". Error downloading image " + url, e);
		}
	}
	
	/**
	 * Update {@link ReceivedFeed} into database
	 * 
	 * @param receivedFeed
	 * @return List of subscription_id
	 */
	private Long updateFeed(ReceivedFeed receivedFeed) {
		Log.i(Constants.LOGTAG, "Refreshing subscription "
				+ receivedFeed.feedUrl);

		// Return value. All receievdFeeds should belong to the same
		// subscription.
		Long subscriptionId = null;

		// Because all the received feeds should belong to the same
		// subscription, we
		// cache the first one when we find it out in the loop.
		Subscription subscription = null;

		SyndFeed feed = receivedFeed.syndFeed;
		for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {
			// Synchronise WebFeed in local storage with the content fetched
			// from the RSS. If an entry of the same URL does not exist in
			// the local storage, a new one is added.

			ContentResolver content = activity.getContentResolver();
			WebFeed webfeed = WebFeed.findByUrl(repository, entry.getLink());
			if (webfeed == null) {

				// Only query subscription if one does not exists. The
				// subscription should be identical for all items in the receivedFeeds.
				if (subscription == null) {
					subscription = Subscription.findByUrl(application, receivedFeed.feedUrl);
				}

				webfeed = new WebFeed(entry, subscription._id);
				content.insert(table("webfeed"), createValues(webfeed));
				subscriptionId = subscription._id;
				subscription.increaseArticlesBy(1);  // Is a new article
				application.notifyObservers();
			} else {
				webfeed.setSyndEntry(entry);
				content.update(table("webfeed"), createValues(webfeed),
						"_id=?", new String[] { String.valueOf(webfeed._id) });
				subscriptionId = webfeed.sub_id;
			}
		}
		return subscriptionId; // All the webfeed should belong to the same
							   // subscription
	}

	@UiThread
	void finish(Progress progress) {
		if (progress != null) progress.countdown();
	}
	
	public static class ReceivedFeed {
		public SyndFeed syndFeed;
		public String feedUrl;
	}
}
