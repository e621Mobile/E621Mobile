package info.beastarman.e621.api;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

public class E621
{
	String DOMAIN_NAME = "https://e621.net";
	private static E621 instance = null;
	
	public E621()
	{
	}
	
	public static E621 getInstance()
	{
		if(instance == null)
		{
			instance = new E621();
		}
		return instance;
	}
	
	public ArrayList<E621Image> post__index(String tags, Integer page, Integer limit) throws IOException
	{
		String base = String.format("%s/post/index.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("tags", tags));
		params.add(new BasicNameValuePair("page", String.valueOf(page+1)));
		params.add(new BasicNameValuePair("limit", String.valueOf(limit)));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		HttpClient httpclient = new DefaultHttpClient();
	    HttpResponse response = httpclient.execute(new HttpGet(base));
	    StatusLine statusLine = response.getStatusLine();
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        response.getEntity().writeTo(out);
	        out.close();
	        String responseString = out.toString();
	        
	        try
	        {
	        	ArrayList<E621Image> images = new ArrayList<E621Image>();
	        	
				JSONArray responseJson = new JSONArray(responseString);
				
				for (int i = 0; i < responseJson.length(); i++)
				{
					images.add(E621Image.fromJSON(responseJson.getJSONObject(i)));
				}
				
				return images;
			}
	        catch (JSONException e)
	        {
				return new ArrayList<E621Image>();
			}
	        
	    } else{
	        //Closes the connection.
	        response.getEntity().getContent().close();
	        throw new IOException(statusLine.getReasonPhrase());
	    }
	}
	
	private HttpResponse tryHttpGet(String url, Integer tries) throws ClientProtocolException, IOException
	{
		
		HttpClient httpclient = new DefaultHttpClient();
		
		for(;tries>=0; tries--)
		{
			try {
				return httpclient.execute(new HttpGet(url));
			} catch (ClientProtocolException e1) {
				continue;
			} catch (IOException e1) {
				continue;
			}
		}
		
		return httpclient.execute(new HttpGet(url));
	}

	public InputStream getImage(E621Image img, File cache_path, int size)
	{
		if(size != E621Image.PREVIEW)
		{
		    HttpResponse response = null;
			try {
				if(size == E621Image.FULL)
				{
					response = tryHttpGet(img.file_url,5);
				}
				else
				{
					response = tryHttpGet(img.sample_url,5);
				}
			} catch (ClientProtocolException e1) {
				e1.printStackTrace();
				return null;
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		    StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        try {
		        	response.getEntity().writeTo(out);
					out.close();
					return new ByteArrayInputStream(out.toByteArray());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return null;
				}
		    }
		}
		
		File cache_local = new File(cache_path, img.id + "." + img.file_ext);
		
		try {
			return new FileInputStream(cache_local);
		} catch (FileNotFoundException e) {
		    HttpResponse response = null;
			try {
				response = tryHttpGet(img.preview_url,5);
			} catch (ClientProtocolException e1) {
				e1.printStackTrace();
				return null;
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		    StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        try {
		        	response.getEntity().writeTo(out);
					out.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return null;
				}
		        
		        byte[] raw_file = out.toByteArray();
		        
		        try {
		        	cache_local.getParentFile().mkdirs();
		        	cache_local.createNewFile();
					BufferedOutputStream file;
					file = new BufferedOutputStream(new FileOutputStream(cache_local));
					file.write(raw_file);
					file.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return null;
				}
		        
		        return new ByteArrayInputStream(raw_file);
		    }
		}
		
		return null;
	}
}
