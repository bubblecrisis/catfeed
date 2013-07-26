package utils;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.catfeed.constants.Constants;

import utils.Http.Response;
import android.os.AsyncTask;
import android.util.Log;

public class Http extends AsyncTask<String, Void, Response> {

	private String url;
	private Responded callback;
	
	public static class Response {
		Response(String body) {
			this.body = body;
		}
		public String body;
	}
	
	public static void get(String url, Responded callback) {
		try {
			Http get = new Http();
			get.url = url;
			get.callback = callback;
			get.execute(url); 		
		}
		catch(Exception e) {
			Log.e(Constants.LOGTAG, e.toString());
		}
	}
	

	@Override
	protected Response doInBackground(String... url) {
		InputStream content = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(url[0]));
			content = response.getEntity().getContent();
			return new Response(IOUtils.asString(content));
		} catch (Exception e) {
			Log.e("[GET REQUEST]", "Network exception", e);
		}
		return null;
	}		
	
	@Override
	protected void onPostExecute(Response result) {
		super.onPostExecute(result);
		callback.received(url, result);
	}
	
	public interface Responded {
		public void received(String url, Response response);
	}
}
