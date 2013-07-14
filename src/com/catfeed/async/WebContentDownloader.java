package com.catfeed.async;

import static com.catfeed.provider.CatFeedContentProvider.table;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import utils.IOUtils;
import utils.Notifier;
import utils.Progress;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

import com.catfeed.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.WebFeed;

public class WebContentDownloader extends AsyncTask<String, Void, Void>  {
	private ContentResolver content;
	private Repository repository;
	private ConnectivityManager connectivity;
	private Progress refreshing;
	
	public static Notifier progressListeners = new Notifier();
	
	public WebContentDownloader(Repository repository, ContentResolver content, ConnectivityManager connectivity, Progress refreshing) {
		this.repository = repository;
		this.content = content;		
		this.connectivity = connectivity;
		this.refreshing = refreshing;
	}

	@Override
	protected Void doInBackground(String... urls) {
		try {
			for (String url: urls) {
				// Only download content if we are in wifi
				if (isWifiConntected()) {
					downloadContent(url);					
				}
				else {
					Log.d(Constants.LOGTAG, "No wifi connection. Not downloading " + url);
				}
			}
			return null;
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, e.toString(), e);
			return null;
		}
		finally {
			refreshing.countdown();
		}
	}
	
	private boolean isWifiConntected() {
		return connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}

//	@Override
//	protected void onPostExecute(WebContent receivedContent) {
//		super.onPostExecute(receivedContent);
//	}
	
	
	public void downloadContent(String url) {
		Log.i(Constants.LOGTAG, "Downloading content " + url);	
		InputStream in = getInputStreamFromUrl(url);
		String html = IOUtils.asString(in);
		if (html != null) {
			Log.i(Constants.LOGTAG, "Fetched Content " +url + ". Size: " + html.length());	
			
			ContentValues values = new ContentValues(3);
			values.put("body", html);
			values.put("cached", true);
			
			WebFeed webfeed = WebFeed.findByUrl(repository, url);			
			content.update(table("webfeed"), values, "_id=?", new String[] { String.valueOf(webfeed._id) });	
			progressListeners.notifyObservers(webfeed);
		}
		else {
			Log.i(Constants.LOGTAG, "Fetched " +url + ". No content");	
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
//	
//	public static class WebContent {
//		public String url;
//		public String html;
//	}
//	
//	public interface WedContentReceived {
//		public void received(WebContent receivedContent);
//	}
}
