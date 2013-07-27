package com.catfeed.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.catfeed.CatFeedApp;
import com.catfeed.R;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;
import com.googlecode.androidannotations.annotations.AfterTextChange;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.App;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.SeekBarProgressChange;
import com.googlecode.androidannotations.annotations.ViewById;

/**
 * Activity to edit subscriptions
 */
@EActivity(R.layout.subscription_edit)
public class SubscriptionEditActivity extends Activity {
	/** Loaded onCreate */
	private Subscription subscription;

	@App
	CatFeedApp application;
	
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
	 * We have to use Fragment transaction to initially create the first
	 * {@link SubscriptionsFragment_}. Doing this through the layout XML result
	 * in the fragment not refreshing when returning from back stack.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		Long subscriptionId = bundle.getLong("id");

		Repository repository = Repository
				.getRepository(getApplicationContext());
		subscription = repository.findById(Subscription.class, subscriptionId);
	}
	
	// --------------------------------------------------------------------------------------------------
	// BINDING EVENTS
	// --------------------------------------------------------------------------------------------------
	/**
	 * Set form
	 */
	@AfterViews
	void prepopulate() {
		title.setText(subscription.title);
		url.setText(subscription.url);
		downloadContent.setChecked(subscription.dlpage > 0);
		downloadImage.setChecked(subscription.dlimages > 0);
		retainFor.setProgress(subscription.retain);
	}

	@AfterTextChange
	void titleAfterTextChanged(TextView title) {
		subscription.title = title.getText().toString();
	}
	
	@AfterTextChange
	void urlAfterTextChanged(TextView url) {
		subscription.url = url.getText().toString();
	}
	
	@Click
	void downloadContent() {
		subscription.dlpage = (downloadContent.isChecked()) ? 1 : 0;
	}
	
	@Click
	void downloadImage() {
		subscription.dlimages = (downloadImage.isChecked()) ? 1 : 0;
	}
	
	@SeekBarProgressChange(R.id.retainFor)
	void onProgressChangeOnSeekBar(SeekBar seekBar, int progress,
			boolean fromUser) {
		String days = String.valueOf(progress);
		if (progress < 10)
			days = " " + days;
		retainForLabel.setText("Retain for " + days + " days");
		subscription.retain = progress;
	}

	// --------------------------------------------------------------------------------------------------
	// BIND IN /res/layout/subscription_edit.xml
	// --------------------------------------------------------------------------------------------------

	public void saveSubscription(View view) {
		Repository repository = Repository
				.getRepository(getApplicationContext());
		repository.update(subscription, subscription._id);
		finish();
	}

	public void deleteSubscription(View view) {
		application.deleteSubscription(subscription);
		finish();
	}
}
