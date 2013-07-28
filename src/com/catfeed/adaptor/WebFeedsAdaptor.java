package com.catfeed.adaptor;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

import java.util.Date;

import utils.CursorUtils;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.catfeed.R;

public class WebFeedsAdaptor extends CursorAdapter {

	private int DEFAULT_HEIGHT = 0;
	
	private static final int SELECTED_BACKGROUND = Color.rgb(240, 240, 240);
	
	/**
	 * Will zoom into the selected arcticle, where the list item webview will not crop
	 * the content height. There can only be one zoomed article. The value represent the feed id.
	 */
	private long selectedArcticle = -1;
	
	private LayoutInflater inflater;
	
	public WebFeedsAdaptor(Context context, Cursor cursor) {
		super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER /* not needed if using CursorLoader */);
		inflater = LayoutInflater.from(context);
		DEFAULT_HEIGHT = (int) applyDimension(COMPLEX_UNIT_DIP, 110, context.getResources().getDisplayMetrics());;
	}
	
	/**
	 * http://stackoverflow.com/questions/12672749/what-bindview-and-newview-do-in-cursoradapter
	 * Calls to load view data to view.
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(CursorUtils.getString(cursor, "title"));

		// Configure the WebView inside list item
		WebView summary = (WebView) view.findViewById(R.id.summary);
		summary.loadData(CursorUtils.getString(cursor, "summary"), "text/html", "utf-8");
		
		// Disable the webview interaction inside the list item
		// For some reason XML setting isn't working out. Set it in code manually.
		summary.setLongClickable(false);
		summary.setFocusable(false);
		summary.setFocusableInTouchMode(false);
		
		// Reduces flickering
		summary.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		
		// Allows WebView to expand if the user "long click" to select the article
		Long id = CursorUtils.getLong(cursor, "_id");
		if (selectedArcticle == id.longValue()) {
			view.setBackgroundColor(SELECTED_BACKGROUND);
			summary.setBackgroundColor(SELECTED_BACKGROUND);
			summary.setClickable(true);
			if (summary.getContentHeight() > DEFAULT_HEIGHT) {
				summary.getLayoutParams().height = LayoutParams.WRAP_CONTENT;				
			}
		}
		else {
			summary.getLayoutParams().height = DEFAULT_HEIGHT;
			summary.setBackgroundColor(Color.TRANSPARENT);
			view.setBackgroundColor(Color.WHITE);
			summary.setClickable(false);
		}
		
		// Footer: cached or not
		TextView cached = (TextView) view.findViewById(R.id.cached);
		if (CursorUtils.getBoolean(cursor, "cached")) {
			cached.setText("Cached");
		}
		else {
			cached.setText("");	
		}
		
		// Footer: Show relative hours and days.
		TextView when = (TextView) view.findViewById(R.id.when);
		long today   = new Date().getTime();
		long pubdate = CursorUtils.getLong(cursor, "date");
		long relativeTime = ((today - pubdate) < 86400000 /* less than 1 day */)? DateUtils.HOUR_IN_MILLIS: DateUtils.DAY_IN_MILLIS;
		when.setText(DateUtils.getRelativeTimeSpanString(pubdate, today, relativeTime));			
		view.setTag(CursorUtils.getString(cursor, "link"));
	}

	/**
	 * http://stackoverflow.com/questions/12672749/what-bindview-and-newview-do-in-cursoradapter
	 * Creates the item view. Calls each time a list item view needs to be displayed in a list.
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = inflater.inflate(R.layout.webfeed_item, parent, false);
		return view;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	public void selectArticle(long feedId) {
		selectedArcticle = feedId;
	}
	
}
