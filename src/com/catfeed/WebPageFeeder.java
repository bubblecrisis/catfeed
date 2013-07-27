package com.catfeed;

import static com.catfeed.provider.CatFeedContentProvider.table;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import utils.IOUtils;
import utils.Progress;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import com.catfeed.db.Repository;
import com.catfeed.model.WebFeed;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EBean;
import com.googlecode.androidannotations.annotations.RootContext;

@EBean
public class WebPageFeeder {
	@RootContext
	Context context;

	@RootContext
	// Only injected if the root context is an activity
	Activity activity;
	
	@Bean
	Repository repository;

	@Background
	void download(ContentResolver content, Progress refreshing, String... urls) {
		try {
			fetchPage(content, urls);
		} finally {
			refreshing.countdown();
		}
	}
	
	public void fetchPage(ContentResolver content, String... urls) {
		ConnectivityManager connectivity = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			for (String url : urls) {
				// Only download content if we are in wifi
				if (isWifiConntected(connectivity)) {
					downloadContent(content, url);
				} else {
					Log.d(Constants.LOGTAG,"No wifi connection. Not downloading " + url);
				}
			}
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, e.toString(), e);
		}
		
	}

	private boolean isWifiConntected(ConnectivityManager connectivity) {
		return connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnected();
	}

	public void downloadContent(ContentResolver content, String url) {
		Log.i(Constants.LOGTAG, "Downloading content " + url);
		InputStream in = getInputStreamFromUrl(url);
		String html = IOUtils.asString(in);
		if (html != null) {
			Log.i(Constants.LOGTAG, "Fetched Content " + url + ". Size: "
					+ html.length());

			ContentValues values = new ContentValues(3);
			values.put("body", html);
			values.put("cached", true);

			WebFeed webfeed = WebFeed.findByUrl(repository, url);
			content.update(table("webfeed"), values, "_id=?",
					new String[] { String.valueOf(webfeed._id) });
			//progressListeners.notifyObservers(webfeed);
		} else {
			Log.i(Constants.LOGTAG, "Fetched " + url + ". No content");
		}
	}

	private InputStream getInputStreamFromUrl(String url) {
		InputStream content = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(url));
			content = response.getEntity().getContent();
		} catch (Exception e) {
			Log.e("[GET REQUEST]", "Network exception", e);
		}
		return content;
	}
}
