package com.catfeed.adaptor;

import java.util.Date;

import utils.CursorUtils;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.catfeed.R;

public class WebFeedsAdaptor extends CursorAdapter {

	private LayoutInflater inflater;
	
	public WebFeedsAdaptor(Context context, Cursor cursor) {
		super(context, cursor, FLAG_REGISTER_CONTENT_OBSERVER /* not needed if using CursorLoader */);
		inflater = LayoutInflater.from(context);
	}
	
	/**
	 * http://stackoverflow.com/questions/12672749/what-bindview-and-newview-do-in-cursoradapter
	 * Calls to load view data to view.
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(CursorUtils.getString(cursor, "title"));

		// Use a flag instead of body.
//		ImageView arrow = (ImageView) view.findViewById(R.id.image1);
//		if (CursorUtils.getString(cursor, "body") != null) {
//			arrow.setImageResource(R.drawable.navigation_next_itemcached);			
//		}
		TextView cached = (TextView) view.findViewById(R.id.cached);
		if (CursorUtils.getBoolean(cursor, "cached")) {
			cached.setText("Cached");
		}
		else {
			cached.setText("");	
		}
		
		// Show relative hours and days.
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


	
}
