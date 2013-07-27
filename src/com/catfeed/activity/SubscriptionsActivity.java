package com.catfeed.activity;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;
import static com.catfeed.Constants.FLKR_ID;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import utils.CursorUtils;
import utils.ListPosition;
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

import com.catfeed.CatFeedApp;
import com.catfeed.Constants;
import com.catfeed.R;
import com.catfeed.RssFeeder;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.provider.CatFeedContentProvider;
import com.catfeed.view.CacheReadChartView;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.photos.Photo;
import com.googlecode.flickrjandroid.photos.PhotoList;
import com.googlecode.flickrjandroid.photos.PhotosInterface;
import com.googlecode.flickrjandroid.photos.SearchParameters;

/**
 * Activity for selecting a list of subscriptions
 */
@OptionsMenu(R.menu.subscription)
@EActivity(R.layout.subscriptions_activity)
public class SubscriptionsActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> , Observer
{		
	@App
	CatFeedApp application;
	
	@Bean
	Repository repository;
	
	@Bean
	RssFeeder rss;
	
	/** Current position when moving off views */
	private ListPosition lastPosition;
	
	private boolean editMode = false;
	CursorAdapter adaptor;

	private Menu menu;
	
	private Random random = new Random();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
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
              rss.subscribe(null, rssUrl);
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		this.menu = menu;
		return true;
	}
	
	//--------------------------------------------------------------------------------------------------
	// Menu
	//--------------------------------------------------------------------------------------------------

	@OptionsItem(R.id.menuitem_edit)
    void editMenuItemClicked() {
		editMode = !editMode;
		MenuItem item = menu.findItem(R.id.menuitem_edit);
		item.setIcon(editMode? R.drawable.navigation_accept: R.drawable.content_edit);
		adaptor.notifyDataSetChanged();  // Causes CursorAdaptor.bindView() to be called. Refreshing views to change the ListView icons.
    }
	
	@OptionsItem(R.id.menuitem_refresh)
    void refreshMenuItemClicked() {
		application.refreshAllSubscriptions();
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


	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
		System.err.println("update -> " + arg0);
	}
	
	
	

}

