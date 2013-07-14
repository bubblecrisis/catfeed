package com.catfeed;

import java.util.Date;

import utils.Http;
import utils.Http.Response;
import utils.IOUtils;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.catfeed.db.Repository;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.EActivity;
import com.catfeed.R;
import com.catfeed.R.layout;

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
@EActivity(R.layout.webview)
public class BrowseWebActivity extends Activity 
{	
	private Long id;
	private WebFeed feed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Long id = getIntent().getExtras().getLong(Constants.FEED_ID);

		Repository repository = Repository.getRepository(this);
		feed = repository.findById(WebFeed.class, id);	
		this.id = feed._id;		
		
		// Set title
		ActionBar ab = getActionBar();
		ab.setTitle(feed.title);			
	}
	
		@Override
	protected void onStart() {
		super.onStart();
		
		WebView webview = (WebView) findViewById(R.id.webview);
	    webview.setWebViewClient(new MyWebViewClient());
	    WebSettings settings = webview.getSettings();
	    settings.setSupportZoom(true);
	    settings.setJavaScriptCanOpenWindowsAutomatically(true);
	    settings.setUseWideViewPort(true);
	    settings.setLoadWithOverviewMode(true);
	    //settings.setBlockNetworkImage(true);
	    
	       
	    boolean networkAvailable = IOUtils.isNetworkAvailable(this);
	    System.err.println("network available " + networkAvailable);
	    if (!networkAvailable) {
	    	webview.setNetworkAvailable(false);
	    	settings.setBlockNetworkLoads(true);
	    }
	    
	    if (feed.body != null) {
	    	Log.d(Constants.LOGTAG, "Loading " + feed.link + " web content from cache");
	    	webview.loadData(feed.body, "text/html", "utf-8");
	    	if (feed.dateread == 0) {
	    		feed.markAsRead(Repository.getRepository(this));
	    	}
	    }
	    else if (networkAvailable) {
		    // Manually and asynchronously fetch HTTP content as string
		    // and set up callback to manually load HTML into WebView on callback.
	    	Log.d(Constants.LOGTAG, "Fetching " + feed.link);
	    	Repository repository = Repository.getRepository(this);
	    	Http.get(feed.link, new LoadIntoWebView(repository, webview, feed));	    	
	    }
	    else {
	    	Log.d(Constants.LOGTAG, "Network not available. Not fetching " + feed.link);	    	
	    }
	}

		/**
		 * Handle async callback for {@link Http#get()}. Assign the HTML
		 * page into WebView when the page is fully read from network.
		 */
		public class LoadIntoWebView implements Http.Responded {
			WebView webview;
			WebFeed feed;
			Repository repository;
			public LoadIntoWebView(Repository repository, WebView webview, WebFeed feed) {
				this.webview = webview;
				this.feed = feed;
				this.repository = repository;
			}
			
			@Override
			public void received(String url, Response response) {
				if (response != null) {
					feed.body = response.body;
					feed.dateread = new Date().getTime();
					feed.cached = true;
					repository.update(feed, feed._id);
					
					// TODO: Response should contain proper mine type and encoding
			        webview.loadData(response.body, "text/html", "utf-8");				
				}
			}
		}
		
		public class MyWebViewClient extends WebViewClient {

		}

}

