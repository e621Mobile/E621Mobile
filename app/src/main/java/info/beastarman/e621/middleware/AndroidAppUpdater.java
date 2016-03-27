package info.beastarman.e621.middleware;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import info.beastarman.e621.backend.PersistentHttpClient;

public class AndroidAppUpdater implements AndroidAppUpdaterInterface {
	private URL _url;
	private boolean beta = false;
	
	public AndroidAppUpdater(URL url)
	{
		this._url = url;
	}

	@Override
	public void setBeta(boolean beta)
	{
		this.beta = beta;
	}

	private URL getURL()
	{
		URL url = null;

		try
		{
			url = new URL(_url.toString() + (beta?"?beta=true":"?beta=false"));
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace();
		}

		Log.d(E621Middleware.LOG_TAG,url.toExternalForm());

		return url;
	}
	
	@Override
	public AndroidAppVersion getLatestVersionInfo()
	{
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		HttpClient httpclient = new PersistentHttpClient(new DefaultHttpClient(httpParams),5);
		
		HttpResponse response = null;
		
		try
		{
			response = httpclient.execute(new HttpGet(getURL().toString()));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		
		StatusLine statusLine = response.getStatusLine();
		
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
	    {
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        
	        try
	        {
				response.getEntity().writeTo(out);
				out.close();
			}
	        catch (IOException e)
	        {
				e.printStackTrace();
				return null;
			}
	        
	        String responseString = out.toString();
	        JSONObject obj;
	        
	        try
	        {
				obj = new JSONObject(responseString);
				
				return new AndroidAppVersion(obj.getInt("versionCode"),obj.getString("versionName"),obj.getString("apkFile"),
													getURL().getProtocol() + "://" + getURL().getAuthority());
			}
	        catch (JSONException e)
	        {
				e.printStackTrace();
				return null;
			}
	    }
	    else
	    {
	    	return null;
	    }
	}
}
