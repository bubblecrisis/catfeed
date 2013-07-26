package com.catfeed.async;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import utils.Notifier;
import android.os.AsyncTask;
import android.util.Log;

import com.catfeed.constants.Constants;
import com.catfeed.db.Repository;
import com.catfeed.model.Subscription;

public class SubscriptionIconDownloader extends AsyncTask<String, Void, Void>  {
	private Repository repository;
	private Long subscriptionId;
	
	public static Notifier progressListeners = new Notifier();
	
	public static Set<String> VALID_CONTENT_TYPE = new HashSet();
	static {
		VALID_CONTENT_TYPE.add("image/gif");
		VALID_CONTENT_TYPE.add("image/jpeg");
		VALID_CONTENT_TYPE.add("image/png");
	}
	
	public SubscriptionIconDownloader(Repository repository, Long subscriptionId) {
		this.repository = repository;
		this.subscriptionId = subscriptionId;
	}

	@Override
	protected Void doInBackground(String... urls) {
		String url = urls[0];
		try {
			Log.i(getClass().getName(), "Downloading image " + url);	
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(url));
			
			HttpEntity entity = response.getEntity();
			Header contentTypeHeader = entity.getContentType();
			String contentType = contentTypeHeader.getValue();
			if (VALID_CONTENT_TYPE.contains(contentType)) {
				InputStream in = entity.getContent();			
				byte[] data = IOUtils.toByteArray(in);
				Subscription.setImage(repository, subscriptionId, data);				
			}
			else {
				Log.w(getClass().getName(), "Invalid content type " + contentType);
			}
			
		} catch (Exception e) {
			Log.e(Constants.LOGTAG, e.toString() + ". Error downloading image " + url, e);
		}
		return null;
	}

//	@Override
//	protected void onPostExecute(WebContent receivedContent) {
//		super.onPostExecute(receivedContent);
//	}

}
