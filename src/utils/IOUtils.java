package utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class IOUtils {

	public static String asString(InputStream in) {
		try {
			StringBuilder sb = new StringBuilder();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			String line = r.readLine();
			while (line != null) {
				sb.append(line);
				line = r.readLine();
			}
			return sb.toString();			
		}
		catch(Exception e) {
			Log.e("IOUtils", e.toString());
			return null;
		}
	}
	

	public static boolean isNetworkAvailable(Activity activity) {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
}
