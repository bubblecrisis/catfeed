package utils;

import static android.widget.CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AdaptorUtils {

	/**
	 * Wrapper to simplify creating simple cursor adaptor with up to 2 text items per row. See {@link SimpleCursorAdapter}.
	 * 
	 * @param context
	 * @param uri
	 * @param fields  eg. "_id", "title"
	 * @return
	 */
	public static CursorAdapter createCursorAdaptor(Context context, Uri uri, String... fields) {
		
		// Compute viewIds array. Also if fields has more than 2 items in its array, it 
		// is bound to 2.
		int viewIds[] = null;
		if (fields.length == 1) {
			viewIds = new int[] { android.R.id.text1 };
		}
		else if (fields.length > 1) {
			viewIds = new int[] { android.R.id.text1, android.R.id.text2 };
			fields = new String[] { fields[0], fields[1] };
		}
		
		Cursor cursor = context.getContentResolver().query(uri, fields, null, null, null);
		CursorAdapter adaptor = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, cursor, 
				fields, 
				viewIds, 
			    FLAG_REGISTER_CONTENT_OBSERVER) {
		
			public View getView(int i, View convertView, ViewGroup parent) {
				View view = super.getView(i, convertView, parent);
				TextView text = (TextView) view.findViewById(android.R.id.text1);
				//text.setTextColor(Color.BLACK);
				return view;
			}
		};
		return adaptor;
	}

}
