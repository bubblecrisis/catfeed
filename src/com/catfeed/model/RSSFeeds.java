package com.catfeed.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of RSS feed.
 */
public class RSSFeeds {

	/**
	 * Feed as list
	 */
	public static List<RSSFeed> FEEDS = new ArrayList<RSSFeed>();

	/**
	 * Feed as Map
	 */
	public static Map<String, RSSFeed> FEED_MAP = new HashMap<String, RSSFeed>();
	
	public static void addItem(RSSFeed item) {
		FEEDS.add(item);
		FEED_MAP.put(item.rssUrl, item);
	}
	
	public static class RSSFeed {
		public String title;
		public String rssUrl;
		public String htmlUrl;
		
		public RSSFeed(String title, String rssUrl, String htmlUrl) {
			this.title = title;
			this.rssUrl = rssUrl;
			this.htmlUrl = htmlUrl;
		}
	}
}
