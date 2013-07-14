package com.catfeed.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WebFeeds {

	/**
	 * Feed as list
	 */
	public static List<WebFeed> FEEDS = new ArrayList<WebFeed>();

	/**
	 * Feed as Map
	 */
	public static Map<String, WebFeed> FEED_MAP = new HashMap<String, WebFeed>();
	
	static {
		// Add 3 sample items.
		addItem(new WebFeed("http://www.theage.com.au/world/israel-bombs-syria-as-us-considers-own-military-options-20130504-2izkm.html", "The Age - Syria"));
		addItem(new WebFeed("http://www.theage.com.au/travel/holiday-type/business/is-a-lieflat-plane-seat-worth-the-high-price-20130426-2ijes.html", "The Age - Airlines"));
		addItem(new WebFeed("http://stackoverflow.com/questions/6578051/what-is-intent-in-android", "What is an intent?2"));
	}
	
	public static void addItem(WebFeed item) {
		FEEDS.add(item);
		FEED_MAP.put(item.url, item);
	}
	
	public static class WebFeed {

		
		public String url;
		public String title;
		
		public WebFeed(String url, String title) {
			this.url = url;
			this.title = title;
		}
		
		@Override
		public String toString() {
			return title;
		}
	}
}
