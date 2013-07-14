package com.catfeed;



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
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.catfeed.adaptor.WebFeedsAdaptor;
import com.catfeed.async.RssAtomFeedRetriever;
import com.catfeed.async.RssAtomFeedRetriever.ReceivedFeed;
import com.catfeed.async.WebContentDownloader;
import com.catfeed.async.postevent.FeedRefreshed;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.catfeed.provider.CatFeedContentProvider;
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
@EActivity(R.layout.webfeed_activity)
public class WebFeedsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, Observer
{	
	private ListPosition lastPosition;
	
	//public Long subscriptionId;
	public Subscription subscription;
	private boolean editMode = false;
	protected CursorAdapter adaptor;
	protected OnItemClickListener listViewOnItemClickListener;

	private int noOfCachedArticles = 0;
	private int totalArticles = 0;
	private int unreadCount = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WebContentDownloader.progressListeners.addObserver(this);
		RssAtomFeedRetriever.progressListeners.addObserver(this);
		
		Intent intent = getIntent();
		
		Long subscriptionId = null;
		if (subscription == null && intent.getExtras() != null) {
			subscriptionId = intent.getExtras().getLong(Constants.SUBSCRIPTION_ID);		
		}
		else if (savedInstanceState != null) {
			subscriptionId = savedInstanceState.getLong(Constants.SUBSCRIPTION_ID);
		}

		// Load subscription information
		Repository repository = new Repository(this);
		subscription = Subscription.findById(repository, subscriptionId);
		Log.d(Constants.LOGTAG, "Loaded subscription " + subscriptionId + " into view");
		
		// Set title
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
		Repository repository = new Repository(this);
		updateTitleCount(repository, subscription._id);	
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
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		WebContentDownloader.progressListeners.deleteObserver(this);
		RssAtomFeedRetriever.progressListeners.deleteObserver(this);
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
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (menu.size() == 0 /* prevent multiple menu items created */) {			
//			MenuInflater menuInflator = getActivity().getMenuInflater();
//			menuInflator.inflate(R.menu.webfeeds, menu);
			menu.add(Menu.NONE, R.id.menuitem_refresh, Menu.NONE, "Refresh")
        	.setIcon(R.drawable.navigation_refresh)
        	.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		   switch (item.getItemId()) {
		    case R.id.menuitem_refresh:
		    	
		    	AnimatingItem animate = new AnimatingItem(item);
		    	animate.rotate(this, R.layout.animate_refresh);
		    	
		    	Repository repository = Repository.getRepository(this);
		    	//Subscription subscription = repository.findById(Subscription.class, subscription._id);
				subscription.refresh(repository, new FeedRefreshed(repository, this, animate));
		    	
		    	return true;
		    }
		    return super.onOptionsItemSelected(item);
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

	public void updateTitleCount(Repository repository, Long sub_id) {
		this.totalArticles = WebFeed.count(repository, sub_id);
		this.noOfCachedArticles = WebFeed.countCached(repository, sub_id);
		this.unreadCount = WebFeed.countUnread(repository, sub_id);
		updateSubtitle();
	}

	private void updateSubtitle() {
		int cachedPercentage = (noOfCachedArticles * 100)/ totalArticles;
		ActionBar ab = getActionBar();
		ab.setSubtitle(totalArticles + " articles | " + unreadCount + " unread | " + cachedPercentage + "% cached");		
	}
	
	//--------------------------------------------------------------------------------------------------
	// Pregression Observer
	//--------------------------------------------------------------------------------------------------

	/**
	 * Contents Download
	 */
   final Runnable updateCacheArticles = new Runnable() {
        public void run() {
			updateSubtitle();	
        }
    };

	/**
	 * Total Articles Download
	 */
   final Runnable updateTotalArticles = new Runnable() {
        public void run() {
        	updateSubtitle();	
        }
    };    
    
    private final Handler ui = new Handler();
    
    /**
     * Observer observing when RSS feeds is received and content is downloaded.
     */
	@Override
	public void update(Observable arg0, Object eventObject) {
		if (eventObject instanceof ReceivedFeed) {
			ReceivedFeed feed = (ReceivedFeed) eventObject;
			if (subscription.url.equalsIgnoreCase(feed.feedUrl)) {	
				// We can't correctly figure out the total articles by getting it from syndfeed 
				// because there could be duplication. So we have to calculate from source.				
				totalArticles = WebFeed.count(Repository.getRepository(this), subscription._id);
				ui.post(updateTotalArticles);				
			}
		}
		if (eventObject instanceof WebFeed) {
			WebFeed feed = (WebFeed) eventObject;
			if (subscription._id.equals(feed.sub_id)) {
				noOfCachedArticles ++;
				ui.post(updateCacheArticles);				
			}
		}
	}
	

}

