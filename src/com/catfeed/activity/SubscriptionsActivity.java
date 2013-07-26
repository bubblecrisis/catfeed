package com.catfeed.activity;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;
import static com.catfeed.constants.Constants.FLKR_ID;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import utils.CursorUtils;
import utils.F;
import utils.FragmentUtils;
import utils.ListPosition;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.catfeed.R;
import com.catfeed.async.RssAtomFeedRetriever;
import com.catfeed.async.WebContentDownloader;
import com.catfeed.async.postevent.FeedRefreshed;
import com.catfeed.async.postevent.Subscribed;
import com.catfeed.constants.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.provider.CatFeedContentProvider;
import com.catfeed.view.CacheReadChartView;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.SearchParameters;

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
@EActivity(R.layout.subscriptions_activity)
public class SubscriptionsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> , Observer
{		
	/** Current position when moving off views */
	private ListPosition lastPosition;
	
	private boolean editMode = false;
	CursorAdapter adaptor;

	private Random random = new Random();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WebContentDownloader.progressListeners.addObserver(this);
		RssAtomFeedRetriever.progressListeners.addObserver(this);
		
		adaptor = new SimpleCursorAdapter(this, R.layout.subscription_item, null /* cursor loads in Loader */, 
			    new String[] { "title" }, 
			    new int[] { R.id.title }, 
			    FLAG_REGISTER_CONTENT_OBSERVER) {
		
			/* trigger by notifyDataSetChanged() to refresh the view */
			public void bindView(View view, Context context, Cursor cursor) {
				super.bindView(view, context, cursor);
				ImageView arrow = (ImageView) view.findViewById(R.id.image1);				
				arrow.setImageResource(editMode? R.drawable.content_edit_item: R.drawable.navigation_next_item);	
				
				// Get a search keyword from title
				//byte[] image = CursorUtils.getBlob(cursor, "icon");
				CacheReadChartView cacheReadView = (CacheReadChartView) view.findViewById(R.id.background);		
				
				// Be creative and set colour based on title's hash
				String title = CursorUtils.getString(cursor, "title");
				cacheReadView.randomizeColourScheme(title.hashCode());				
//				if (image == null) {
//					String title = CursorUtils.getString(cursor, "title");
////					int r = title.substring(0, 1).hashCode() % 250;
////					int b = title.substring(1, 2).hashCode() % 250;
////					int g = title.substring(3, 3).hashCode() % 250;
////					
////					Bitmap bitmap = Bitmap.createBitmap(100, 100, Config.RGB_565);
////					Canvas canvas = new Canvas(bitmap);
////					Paint paint = new Paint(); 
////					paint.setColor(Color.WHITE);
////					canvas.drawPaint(paint);
////					paint.setTextSize(100); 
////					paint.setColor(Color.rgb(r, g, b));
////					paint.setAlpha(120);
////					paint.setTypeface(Typeface.DEFAULT_BOLD);
////					paint.setAntiAlias(true);						
////					canvas.drawText(title, 20, 120, paint); 
////					background.setImageBitmap(bitmap);
//					
//					Long id = CursorUtils.getLong(cursor, "_id");
//					fetchFlickrImage(context, id, flickerSearchKeywordFromTitle(title));
//				}
//				if (image != null) {					
//					Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
//					int square = (int) ScreenUtils.convertDpToPixel(55, context);
//					int width = (bitmap.getWidth() > square)? square: bitmap.getWidth();
//					int height = (bitmap.getHeight() > square)? square: bitmap.getHeight();
//					int x = (bitmap.getWidth() - width) /2;
//					int y = (bitmap.getHeight() - height) /2;
//					System.err.println("size -> " + height);
//					bitmap = Bitmap.createBitmap(bitmap, x , y, width, height);
//					background.setImageBitmap(bitmap);						
//				}
				
			}
			
			@Override
			public boolean hasStableIds() {
				return true;
			}
		};
		setListAdapter(adaptor); // Setting adaptor on UI-thread can block?
		checkForNewSubscription();
	}
	
	/**
	 * Restart loader so that any data changes get refreshed.
	 */
	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		lastPosition = ListPosition.getScrollPosition(getListView());
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		if (editMode) {
			Intent intent = new Intent(this, SubscriptionEditActivity_.class);
			intent.putExtra("id", id);
			startActivity(intent);
		}
		else {
			Intent intent = new Intent(this, WebFeedsActivity_.class);
		    intent.putExtra(Constants.SUBSCRIPTION_ID, id);
		    startActivity(intent);
		}
	}

	/**
	 * Generic code to replace old fragment with new fragment to go forward.
	 * @param newFragment
	 * @param oldFragment
	 */
	public void replaceFragment(Fragment newFragment, Fragment oldFragment) {
		FragmentUtils.replace(getFragmentManager(), R.id.subscriptions_frame, newFragment, oldFragment);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WebContentDownloader.progressListeners.deleteObserver(this);
		RssAtomFeedRetriever.progressListeners.deleteObserver(this);
	}

	
	//--------------------------------------------------------------------------------------------------
	// LOAD FLICKR IMAGE
	//--------------------------------------------------------------------------------------------------
	 @Background
	 void fetchFlickrImage(Context context, Long id, String search) {
		 System.err.println("fetchFlickrImage(): " + id + " " + search);
			 Flickr f = new Flickr(FLKR_ID);
			 PhotosInterface photoInterface = f.getPhotosInterface();
			 try {
				 SearchParameters params = new SearchParameters();
				 params.setText("cat");
				 params.setSafeSearch("1");
				 params.setMedia("photos");
				 params.setLicense("4,7");  // http://www.flickr.com/services/api/flickr.photos.licenses.getInfo.html
				 params.setTags(new String[] { "cat" } );
				 PhotoList photos = photoInterface.search(params, 100, 1);
				 int i = random.nextInt(photos.size());
				 Photo photo = photos.get(i);
				 
				 byte[] image = IOUtils.toByteArray(photo.getSmallAsInputStream());
				 System.err.println("read image size " + image.length);
				 Repository repository = new Repository(context);
				 Subscription.setImage(repository, id, image);
			 }
			 catch(Exception e) {
				 Log.d(Constants.LOGTAG, "fetchFlickrImage() Error: " + e.toString());
			 }
	 }
	 
	 @UiThread
	 void updateSubscriptionImage(Photo photo) {
		 
	 }
	 
	 /**
	  * Takes the first 2 words from the title as flickr search key words. Remove
	  * any 'the' in the title.
	  * @param title
	  * @return
	  */
	 private String flickerSearchKeywordFromTitle(String title) {
		title = title.toLowerCase();
		title = title.replace("the", "").trim(); // Not interested in 'The'
		String[] words = title.split(" ");
		if (words.length == 1) {
			return words[0];
		} else {
			return words[0] + " " + words[1];
		}
	 }
	//--------------------------------------------------------------------------------------------------
	// SUBSCRIBE
	//--------------------------------------------------------------------------------------------------
	public void checkForNewSubscription() {

		// Detect RSS URL intent and subscribe to that URL.
		if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
              String rssUrl = getIntent().getData().toString();
              Log.d(Constants.LOGTAG, "Adding feed " + rssUrl);
              Repository repository = Repository.getRepository(this);
              Subscription.subscribe(rssUrl, new Subscribed(repository, this /* Activity */));
              return;
		}
		
		// Check clipboard
//		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
//		ClipData clip = clipboard.getPrimaryClip();
//		if (clip.getItemCount() > 0) {
//			clip.getItemAt(0).getText()
//		}
	}
	
	//--------------------------------------------------------------------------------------------------
	// MENU
	//--------------------------------------------------------------------------------------------------
	
	/**
	 * Make sure to set setHasOptionsMenu(true);
	 * If the screen rotates, this gets call so we should only inflate the menu items if there
	 * is nothing in the menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	    
		if (menu.size() == 0 /* prevent multiple menu items created */) {	
			menu.add(Menu.NONE, R.id.menuitem_edit, Menu.NONE, "Edit")
        	.setIcon(editMode? R.drawable.navigation_accept: R.drawable.content_edit)
        	.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			
			menu.add(Menu.NONE, R.id.menuitem_refresh, Menu.NONE, "Refresh")
	        	.setIcon(R.drawable.navigation_refresh)
	        	.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

//			MenuInflater menuInflator = getActivity().getMenuInflater();
//			menuInflator.inflate(R.menu.subscription, menu);			
		
			// Ensure screen reload correctly read the editMode setting
//			menu.getItem(0).setIcon(editMode? R.drawable.navigation_accept: R.drawable.content_edit);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		   switch (item.getItemId()) {
		    case R.id.menuitem_edit:
		    	editMode = !editMode;
		    	item.setIcon(editMode? R.drawable.navigation_accept: R.drawable.content_edit);
		    	adaptor.notifyDataSetChanged();  // Causes CursorAdaptor.bindView() to be called. Refreshing views to change the ListView icons.
		    	return true;
		    case R.id.menuitem_refresh:
		    	refreshAllSubscriptions(item);
		    	return true;
		    }
		    return super.onOptionsItemSelected(item);
	}
	
	//--------------------------------------------------------------------------------------------------
	// Loader
	//--------------------------------------------------------------------------------------------------

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Uri uri = CatFeedContentProvider.table("subscription");
		return new CursorLoader(this /* Activity context */, uri, new String[] { "_id", "title", "icon" }, null, null, null);
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
	
	//--------------------------------------------------------------------------------------------------
	// Subscription Logic
	//--------------------------------------------------------------------------------------------------

	/**
	 * Refreshes ALL subscriptions.
	 * PROBLEM: Because we are doing multiple concurrent async job, we cannot properly reset
	 *          the spinning refresh button. Need to rethink this one.
	 * @param item
	 */
	private void refreshAllSubscriptions(MenuItem item) {
    	
		Repository repository = Repository.getRepository(this);
		List<Subscription> subscriptions = repository.all(Subscription.class);	
		
		Collection<String> urls = F.each(subscriptions, new F.Function<Subscription, String>() {
			public String apply(Subscription subscription) {
				return subscription.url;
			}			
		});
		
		FeedRefreshed refreshed = new FeedRefreshed(repository, this, null); 
		RssAtomFeedRetriever feedRetriever = new RssAtomFeedRetriever(refreshed);
	    feedRetriever.execute(urls.toArray(new String[urls.size()]));
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		System.err.println("update -> " + arg0);
	}
	
	
	

}

