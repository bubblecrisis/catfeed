package com.catfeed.async;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import utils.Notifier;
import android.os.AsyncTask;
import android.util.Log;

import com.catfeed.constants.Constants;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.FeedFetcher;
import com.google.code.rome.android.repackaged.com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

public class RssAtomFeedRetriever extends AsyncTask<String, Void, List<RssAtomFeedRetriever.ReceivedFeed>> {
	
	private RssAtomReceived callback;
	public static Notifier progressListeners = new Notifier();
	
	public RssAtomFeedRetriever(RssAtomReceived callback) {
		this.callback = callback;
	}	

	@Override
	protected List<ReceivedFeed> doInBackground(String... feedUrl) {
		try {
			List<ReceivedFeed> receivedFeeds = new ArrayList(feedUrl.length);
			FeedFetcher feedFetcher = new HttpURLFeedFetcher();
			for (String url: feedUrl) {
				ReceivedFeed receivedFeed = new ReceivedFeed();
				receivedFeed.feedUrl = url;
				receivedFeed.syndFeed = feedFetcher.retrieveFeed(new URL(url));
				receivedFeeds.add(receivedFeed);
			}
			return receivedFeeds;
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, "fetchRecentNews: " + e.toString());
			return new ArrayList<ReceivedFeed>(0);
		}
	}

	@Override
	protected void onPostExecute(List<ReceivedFeed> results) {
		super.onPostExecute(results);
		callback.received(results);
		
		// Notify observers here after feed is updated to database.
		// We can't notify earlier because we count the database records, 
		// and we cannot count the number of received events because
		// there may be duplication.
		for (ReceivedFeed receivedFeed: results) {
			progressListeners.notifyObservers(receivedFeed);			
		}
	}
		
	public static class ReceivedFeed {
		public SyndFeed syndFeed;
		public String feedUrl;
	}
	
	public interface RssAtomReceived {
		public void received(List<ReceivedFeed> feeds);
	}
}
