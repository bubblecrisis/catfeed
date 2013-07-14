package com.catfeed;

import static com.catfeed.Constants.LOGTAG;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.SeekBarProgressChange;
import com.googlecode.androidannotations.annotations.ViewById;
import com.catfeed.R;

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
@EActivity(R.layout.subscription_edit)
public class SubscriptionEditActivity extends Activity 
{	
	/** Loaded onCreate */
	private Subscription subscription;

	@ViewById
	public TextView title;

	@ViewById
	public TextView url;

	@ViewById
	public CheckBox downloadContent;

	@ViewById
	public CheckBox downloadImage;
	
	@ViewById
	public SeekBar retainFor;

	@ViewById
	public TextView retainForLabel;		
	
	/**
	 * We have to use Fragment transaction to initially create the first {@link SubscriptionsFragment_}.
	 * Doing this through the layout XML result in the fragment not refreshing when returning from back stack.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = getIntent().getExtras();
		Long subscriptionId = bundle.getLong("id");
		
		Repository repository = Repository.getRepository(getApplicationContext());
		subscription = repository.findById(Subscription.class, subscriptionId);
	}

	@Override
	protected void onStart() {
		super.onStart();
//		((TextView) findViewById(R.id.title)).setText(subscription.title);
//		((TextView) findViewById(R.id.url)).setText(subscription.url);
		title.setText(subscription.title);
		url.setText(subscription.url);
		downloadContent.setChecked(subscription.dlpage > 0);
		downloadImage.setChecked(subscription.dlimages > 0);
		retainFor.setProgress(subscription.retain);
	}

	public void updateModel() {
		// There is no view -> model data binding
		// do this manually by hand
		subscription.title = title.getText().toString();
		subscription.url = url.getText().toString();
		subscription.dlpage = (downloadContent.isChecked())? 1:0;
		subscription.dlimages = (downloadImage.isChecked())? 1:0;	
		subscription.retain = retainFor.getProgress();
	}

	//--------------------------------------------------------------------------------------------------
	// FORM
	//--------------------------------------------------------------------------------------------------
	
	@SeekBarProgressChange(R.id.retainFor)
	void onProgressChangeOnSeekBar(SeekBar seekBar, int progress, boolean fromUser) {
		String days = String.valueOf(progress);
		if (progress < 10) days = " " + days;
		retainForLabel.setText("Retain for " + days + " days");
		subscription.retain = progress;
	}
	
	public void saveSubscription(View view) {
		updateModel();
		
		Repository repository = Repository.getRepository(getApplicationContext());
		repository.update(subscription, subscription._id);
		finish();
	}
	
	public void deleteSubscription(View view) {
		Repository repository = Repository.getRepository(getApplicationContext());
		repository.delete(WebFeed.class, "sub_id=?", String.valueOf(subscription._id));
		Log.d(LOGTAG, "Deleted WebFeed subscription id " + subscription._id);
		
		repository.delete(Subscription.class, subscription._id);
		Log.d(LOGTAG, "Deleted Subscription id " + subscription._id);		
		finish();
	}
}
