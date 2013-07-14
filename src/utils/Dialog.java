package utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;

public class Dialog {

	
	public static void error(Activity activity, String title, String message, OnClickListener callback) {
		if (title == null) title = "Error";
		new AlertDialog.Builder(activity)
	    .setTitle(title)
	    .setMessage(message)
	    .setNeutralButton("OK", callback)
	    .show();
	}
}
