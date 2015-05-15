package info.beastarman.e621.middleware;

import info.beastarman.e621.backend.PersistentHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

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

public class AndroidAppUpdater
{
	private URL url;
	
	public AndroidAppUpdater(URL url)
	{
		this.url = url;
	}
	
	public AndroidAppVersion getLatestVersionInfo()
	{
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		HttpClient httpclient = new PersistentHttpClient(new DefaultHttpClient(httpParams), 5);

		HttpResponse response = null;

		try
		{
			response = httpclient.execute(new HttpGet(url.toString()));
		}
		catch(IOException e)
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
			catch(IOException e)
			{
				e.printStackTrace();
				return null;
			}

			String responseString = out.toString();
			JSONObject obj;

			try
			{
				obj = new JSONObject(responseString);

				return new AndroidAppVersion(obj.getInt("versionCode"), obj.getString("versionName"), obj.getString("apkFile"),
													url.getProtocol() + "://" + url.getAuthority());
			}
			catch(JSONException e)
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
	
	public static class AndroidAppVersion
	{
		public int versionCode;
		public String versionName;
		public String apkURL;
		public String domain;

		public AndroidAppVersion(int versionCode, String versionName, String apkURL, String domain)
		{
			this.versionCode = versionCode;
			this.versionName = versionName;
			this.apkURL = apkURL;
			this.domain = domain;
		}

		public String getFullApkURL()
		{
			return domain + apkURL;
		}
	}
}
