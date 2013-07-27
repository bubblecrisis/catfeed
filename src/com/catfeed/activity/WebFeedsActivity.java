package com.catfeed.activity;



import java.util.Observable;
import java.util.Observer;

import utils.AnimatingItem;
import utils.ListPosition;
import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.catfeed.CatFeedApp;
import com.catfeed.Constants;
import com.catfeed.R;
import com.catfeed.RssFeeder;
import com.catfeed.RssFeeder.ReceivedFeed;
import com.catfeed.adaptor.WebFeedsAdaptor;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.catfeed.provider.CatFeedContentProvider;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.UiThread;

/**
 * Activity for list of web RSS feeds
 */
@OptionsMenu(R.menu.webfeeds)
@EActivity(R.layout.webfeed_activity)
public class WebFeedsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, Observer
{	
	@App
	CatFeedApp application;
	
	@Bean
	RssFeeder rss;
	
	@Bean
	Repository repository;

	@Extra
	public Long subscriptionId;
	
	private Menu menu;
	
	private ListPosition lastPosition;
	
	public Subscription subscription;
	protected CursorAdapter adaptor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		application.addObserver(this);
		
//		if (subscription == null && savedInstanceState != null) {
//			subscriptionId = savedInstanceState.getLong(Constants.SUBSCRIPTION_ID);
//		}

		// Load subscription information
		subscription = application.getSubscription(subscriptionId);
		Log.d(Constants.LOGTAG, "Loaded subscription " + subscriptionId + " into view");
	}
	
	/**
	 * Set View
	 */
	@AfterViews
	void prepopulate() {
		ActionBar ab = getActionBar();
		ab.setTitle(subscription.title);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		adaptor = new WebFeedsAdaptor(this, null);
		setListAdapter(adaptor);

		// ------------------------------------------------------------------------------------------
		// Handle Swipe Gesture.
		// ------------------------------------------------------------------------------------------
//		getListView().setOnTouchListener(swipeDetector);
//		getListView().setOnItemClickListener(new OnItemClickListener() {
//	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//	        	System.err.println(swipeDetector.getAction() );
//	                if (swipeDetector.swipeDetected() && swipeDetector.getAction() == SwipeDetector.Action.RL){
//	                	System.err.println("finsih");
//	                    finish();
//	                } else {
//	                	listViewOnItemClickListener.onItemClick(parent, view, position, id);
//	                }
//	            }
//			});
//		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
//	        @Override
//	        public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
//	            if (swipeDetector.swipeDetected()){
//	            	Log.i(Constants.LOGTAG, "long click swipe");
//	            } else {
//	            	 Log.i(Constants.LOGTAG, "long click");
//	            }
//	            return true;
//	        }
//	    });
	}
    
	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(0, null, this /* Cursor loader callback */);
		subscription.calculateStatistics(application);
		updateSubtitle();	
	}
	
	@Override
	public void onPause() {
		super.onPause();
		lastPosition = ListPosition.getScrollPosition(getListView());
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putLong(Constants.SUBSCRIPTION_ID, subscription._id);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Handle each list item click event. The list item layout and widgets must have clickable=false.
	 */
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		Intent intent = new Intent(this, WebSummaryActivity_.class);
	    intent.putExtra(Constants.FEED_ID, id);
	    intent.putExtra(Constants.SUBSCRIPTION_TITLE, subscription.title);
	    startActivity(intent);
	}
	//--------------------------------------------------------------------------------------------------
	// MENU
	//--------------------------------------------------------------------------------------------------

	/**
	 * Make sure to set setHasOptionsMenu(true);
	 */
	@OptionsItem(R.id.menuitem_refresh)
    void menuRefresh(MenuItem item) {
		AnimatingItem animate = new AnimatingItem(item);
    	animate.rotate(this, R.layout.animate_refresh);
    	subscription.refresh(rss, animate);
    }
	
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		   switch (item.getItemId()) {
//		    case R.id.menuitem_refresh:
//		    	
//		    	AnimatingItem animate = new AnimatingItem(item);
//		    	animate.rotate(this, R.layout.animate_refresh);
//		    	
//		    	Repository repository = Repository.getRepository(this);
//		    	//Subscription subscription = repository.findById(Subscription.class, subscription._id);
//				//subscription.refresh(repository, new FeedRefreshed(repository, this, animate));
//				subscription.refresh(rss, animate, new FeedRefreshed(repository, this, animate));
//		    	return true;
//		    }
//		    return super.onOptionsItemSelected(item);
//	}

	public void refreshFinished() {
//		Menu
//		try {
//		item.getActionView().clearAnimation();
//		item.setActionView(null);				
//	}
//	catch(Exception e) { /* ignore, if stop gets called more than once it throws */ }
	}
	
	//--------------------------------------------------------------------------------------------------
	// Loader
	//--------------------------------------------------------------------------------------------------
	
	/**
	 * This is triggered by Fragment or Activity getLoaderManager().initLoader() or restartLoader(...)
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Uri uri = CatFeedContentProvider.table("webfeed");
		return new CursorLoader(this, uri, 
			   new String[] { "_id", "title", "summary", "link" , "category", "date", "cached" }, "sub_id=?", new String[] { String.valueOf(subscription._id) }, "date desc");
	
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		// Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        adaptor.swapCursor(data);
        
        // The list should now be shown.
//        if (isResumed()) {
//            setListShown(true);
//        } else {
//            setListShownNoAnimation(true);
//        }
        // Restore to last scrolled to position
        if (lastPosition != null) lastPosition.scroll(getListView());
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        adaptor.swapCursor(null);
	}

	public void updateSubtitle() {
		ActionBar ab = getActionBar();
		ab.setSubtitle(subscription.getTotalArticles() + " articles | " + subscription.getUnreadCount() + " unread | " + subscription.getCachedPercentage() + "% cached");		
	}
	
	//--------------------------------------------------------------------------------------------------
	// Observer Pattern
	//--------------------------------------------------------------------------------------------------
    
    /**
     * Observer observing when RSS feeds is received and content is downloaded.
     */
	@Override
	@UiThread
	public void update(Observable observed, Object eventObject) {
		updateSubtitle();		
	}
	
//	public void update(Observable observed, Object eventObject) {
//		if (eventObject instanceof ReceivedFeed) {
//			ReceivedFeed feed = (ReceivedFeed) eventObject;
//			if (subscription.url.equalsIgnoreCase(feed.feedUrl)) {	
//				// We can't correctly figure out the total articles by getting it from syndfeed 
//				// because there could be duplication. So we have to calculate from source.				
//				application.totalArticles = WebFeed.count(Repository.getRepository(this), subscription._id);
//				updateSubtitle();		
//			}
//		}
//		if (eventObject instanceof WebFeed) {
//			WebFeed feed = (WebFeed) eventObject;
//			if (subscription._id.equals(feed.sub_id)) {
//				application.noOfCachedArticles ++;
//				updateSubtitle();		
//			}
//		}
//	}
	

}

