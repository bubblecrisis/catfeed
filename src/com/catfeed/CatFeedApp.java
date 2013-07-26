package com.catfeed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;

public class CatFeedApp {

	public static List<Subscription> subscriptions = new ArrayList();
	
	public static void init(Context context) {
		Repository repository = Repository.getRepository(context);
		subscriptions.addAll(repository.all(Subscription.class));
	}
}
