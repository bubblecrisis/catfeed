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

import com.catfeed.R;
import com.catfeed.constants.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.EActivity;

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
@EActivity(R.layout.websummary)
public class WebSummaryActivity extends Activity 
{	
	private Long id;
	private WebFeed feed;
//	private SwipeDetector swipeDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Long id = getIntent().getExtras().getLong(Constants.FEED_ID);
		String title = getIntent().getExtras().getString(Constants.SUBSCRIPTION_TITLE);
		
		Repository repository = Repository.getRepository(this);
		feed = repository.findById(WebFeed.class, id);	
		this.id = feed._id;		

		// Set title
		ActionBar ab = getActionBar();
		ab.setTitle(title);			

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
	
	@Override
	protected void onStart() {
		super.onStart();
		
		TextView text = (TextView) findViewById(R.id.title);
		text.setText(feed.title);
//		text.setOnTouchListener(swipeDetector);

		if (feed.summary != null) {
			WebView webview = (WebView) findViewById(R.id.websummary);
			webview.setWebViewClient(new MyWebViewClient());
			webview.getSettings().setSupportZoom(true);
			webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
			webview.getSettings().setAllowFileAccess(false);
			webview.getSettings().setDomStorageEnabled(false);
//			webview.setOnTouchListener(swipeDetector);
			webview.loadData(feed.summary, "text/html", "utf-8");	
			
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
	
	/**
	 * Make sure to set setHasOptionsMenu(true);
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflator = getMenuInflater();
		menuInflator.inflate(R.menu.websummary, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	   	   switch (item.getItemId()) {
		    case R.id.menuitem_search:
		    	browseWeb();
				return true;
		    }
		    return super.onOptionsItemSelected(item);
	}

	private void browseWeb() {
		if (IOUtils.isNetworkAvailable(this) || feed.body != null) {
			Intent intent = new Intent(this, BrowseWebActivity_.class);
		    intent.putExtra(Constants.FEED_ID, id);
		    startActivity(intent);			
		}
		else {
			Dialog.error(this, feed.title, "Internet connection is unavailable", null);
		}
	}
}

