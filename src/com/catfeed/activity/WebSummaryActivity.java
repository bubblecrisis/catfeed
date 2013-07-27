package com.catfeed.activity;

import utils.Dialog;
import utils.IOUtils;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.catfeed.CatFeedApp;
import com.catfeed.Constants;
import com.catfeed.R;
import com.catfeed.RssFeeder;
import com.catfeed.db.Repository;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.ViewById;

/**
 * An activity representing a list of WebFeeds. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link WebFeedDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link SubscriptionsFragment} and the item details (if present) is a
 * {@link WebFeedDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link SubscriptionsFragment.Callbacks} interface to listen for item
 * selections.
 */
@OptionsMenu(R.menu.websummary)
@EActivity(R.layout.websummary)
public class WebSummaryActivity extends Activity 
{	
	@App
	CatFeedApp application;
	
	@Bean
	RssFeeder rss;
	
	@Bean
	Repository repository;

	@Extra
	Long feedId;
	
	@Extra
	String subscriptionTitle;
	
	@ViewById
	TextView title;
	
	private WebFeed feed;
//	private SwipeDetector swipeDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		feed = repository.findById(WebFeed.class, feedId);	

//		// Handle Gesture
//		swipeDetector = new SwipeDetector() {
//			public boolean swipeLeft(View v, MotionEvent event) {
//				finish();
//				return false;
//			}
//			public boolean swipeRight(View v, MotionEvent event) {
//				browseWeb();
//				return false;
//			}
//		};
	}
	
	/**
	 * Set View
	 */
	@AfterViews
	void prepopulate() {
		ActionBar ab = getActionBar();
		ab.setTitle(subscriptionTitle);	
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		title.setText(feed.title);
//		text.setOnTouchListener(swipeDetector);

		if (feed.summary != null) {
			WebView websummary = (WebView) findViewById(R.id.websummary);
			websummary.setWebViewClient(new MyWebViewClient());
			websummary.getSettings().setSupportZoom(true);
			websummary.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
			websummary.getSettings().setAllowFileAccess(false);
			websummary.getSettings().setDomStorageEnabled(false);
//			websummary.setOnTouchListener(swipeDetector);
			websummary.loadData(feed.summary, "text/html", "utf-8");	
			
			// Mark article as read
			if (feed.dateread == 0) {
				feed.markAsRead(Repository.getRepository(this));
			}
		}
	}

	public class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browser);
			return true;
		}
	}
	
	//--------------------------------------------------------------------------------------------------
	// MENU - Requires setHasOptionsMenu(true);
	//--------------------------------------------------------------------------------------------------

	@OptionsItem(R.id.menuitem_search)
    void searchMenuClicked() {
		browseWeb();
    }

	private void browseWeb() {
		if (IOUtils.isNetworkAvailable(this) || feed.body != null) {
			Intent intent = new Intent(this, BrowseWebActivity_.class);
		    intent.putExtra(Constants.FEED_ID, feedId);
		    startActivity(intent);			
		}
		else {
			Dialog.error(this, feed.title, "Internet connection is unavailable", null);
		}
	}
}

