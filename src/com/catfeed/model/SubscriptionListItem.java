package com.catfeed.model;

import com.catfeed.db.Repository;

public class SubscriptionListItem {

	public Subscription subscription;
	public int feeds;
	public int cached;
	public int read;
	
	public SubscriptionListItem(Subscription subscription, int feeds,
			int cached, int read) {
		super();
		this.subscription = subscription;
		this.feeds = feeds;
		this.cached = cached;
		this.read = read;
	}

	public SubscriptionListItem(Repository repository, Subscription subscription) {
		super();
		this.subscription = subscription;
		this.feeds = repository.countBy(WebFeed.class, "sub_id = ?", new String[] { String.valueOf(subscription._id) });
		this.feeds = repository.countBy(WebFeed.class, "sub_id = ? and read = 1", new String[] { String.valueOf(subscription._id) });
		this.feeds = repository.countBy(WebFeed.class, "sub_id = ? ", new String[] { String.valueOf(subscription._id) });
		
	}
	
	public float getCachedPercentage() {
		return cached / feeds;
	}

	public float getReadPercentage() {
		return read / feeds;
	}

}
