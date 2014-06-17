package info.beastarman.e621.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class E621
{
	String DOMAIN_NAME = "https://e621.net";
	private static E621 instance = null;
	
	protected E621()
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
	
	public E621Image post__show(String id) throws IOException
	{
		String base = String.format("%s/post/show.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("id", id));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		HttpResponse response = tryHttpGet(base,5);
	    StatusLine statusLine = response.getStatusLine();
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        response.getEntity().writeTo(out);
	        out.close();
	        String responseString = out.toString();
	        
	        try {
				return E621Image.fromJSON(new JSONObject(responseString));
			} catch (JSONException e) {
				return null;
			}
	        
	    } else{
	        //Closes the connection.
	        response.getEntity().getContent().close();
	        throw new IOException(statusLine.getReasonPhrase());
	    }
	}
	
	public E621Search post__index(String tags, Integer page, Integer limit) throws IOException
	{
		String base = String.format("%s/post/index.xml?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("tags", tags));
		params.add(new BasicNameValuePair("page", String.valueOf(page+1)));
		params.add(new BasicNameValuePair("limit", String.valueOf(limit)));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		HttpResponse response = tryHttpGet(base,5);
	    StatusLine statusLine = response.getStatusLine();
	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        response.getEntity().writeTo(out);
	        out.close();
	        String responseString = out.toString();
	        
	        try
	        {
	        	ArrayList<E621Image> images = new ArrayList<E621Image>();
	        	
	        	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder = dbf.newDocumentBuilder();
	            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(responseString.getBytes("utf-8"))));
	            doc.getDocumentElement().normalize();
	            
	            Element posts = (Element) doc.getElementsByTagName("posts").item(0);
				NodeList nodes = posts.getElementsByTagName("post");

				for (int i = 0; i < nodes.getLength(); i++)
				{
					images.add(E621Image.fromXML((Element)nodes.item(i)));
				}
				
				return new E621Search(images,Integer.parseInt(posts.getAttribute("offset")),Integer.parseInt(posts.getAttribute("count")),limit);
	        } catch (ParserConfigurationException e) {
				e.printStackTrace();
				return new E621Search();
				
			} catch (SAXException e) {
				e.printStackTrace();
				return new E621Search();
			}
	        
	    } else{
	        //Closes the connection.
	        response.getEntity().getContent().close();
	        throw new IOException(statusLine.getReasonPhrase());
	    }
	}
	
	public String user__login(String name, String password)
	{
		String base = String.format("%s/user/login.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("name", name));
		params.add(new BasicNameValuePair("password", password));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		try
		{
			HttpResponse response = tryHttpGet(base,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jsonResponse = new JSONObject(responseString);
				
				if(jsonResponse.has("password_hash"))
				{
					return jsonResponse.getString("password_hash");
				}
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}
	
	public ArrayList<E621TagAlias> tag_alias__index(Boolean approved, String order, Integer page)
	{
		String base = String.format("%s/tag_alias/index.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		if(approved != null) params.add(new BasicNameValuePair("approved", String.valueOf(approved)));
		if(order != null) params.add(new BasicNameValuePair("order", order));
		if(page != null) params.add(new BasicNameValuePair("page", String.valueOf(page+1)));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		try
		{
			ArrayList<E621TagAlias> aliases = new ArrayList<E621TagAlias>();
			
			HttpResponse response = tryHttpGet(base,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONArray jsonResponse = new JSONArray(responseString);
				
				int i = 0;
				
				for(i=0; i<jsonResponse.length(); i++)
				{
					aliases.add(E621TagAlias.fromJson(jsonResponse.getJSONObject(i)));
				}
				
				return aliases;
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}
	
	public ArrayList<E621Tag> tag__index(Integer limit, Integer page, String order, Integer id, Integer after_id, String name, String name_pattern)
	{
		String base = String.format("%s/tag/index.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		if(limit != null) params.add(new BasicNameValuePair("limit", String.valueOf(limit)));
		if(page != null) params.add(new BasicNameValuePair("page", String.valueOf(page+1)));
		if(order != null) params.add(new BasicNameValuePair("order", order));
		if(id != null) params.add(new BasicNameValuePair("id", String.valueOf(id)));
		if(after_id != null) params.add(new BasicNameValuePair("after_id", String.valueOf(after_id)));
		if(name != null) params.add(new BasicNameValuePair("name", name));
		if(name_pattern != null) params.add(new BasicNameValuePair("name_pattern", name_pattern));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		Log.d("Msg",base);
		
		try
		{
			ArrayList<E621Tag> tags = new ArrayList<E621Tag>();
			
			HttpResponse response = tryHttpGet(base,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONArray jsonResponse = new JSONArray(responseString);
				
				int i = 0;
				
				for(i=0; i<jsonResponse.length(); i++)
				{
					tags.add(E621Tag.fromJson(jsonResponse.getJSONObject(i)));
				}
				
				return tags;
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}
	
	public Boolean favorite__create(int id, String login, String password_hash)
	{
		String base = String.format("%s/favorite/create.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("id", String.valueOf(id)));
		params.add(new BasicNameValuePair("login", login));
		params.add(new BasicNameValuePair("password_hash", password_hash));
		
		try
		{
			HttpResponse response = tryHttpPost(base,params,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jsonResponse = new JSONObject(responseString);
		        
		        return jsonResponse.optBoolean("success",false);
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}

	public Boolean favorite__destroy(int id, String login, String password_hash)
	{
		String base = String.format("%s/favorite/destroy.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("id", String.valueOf(id)));
		params.add(new BasicNameValuePair("login", login));
		params.add(new BasicNameValuePair("password_hash", password_hash));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		try
		{
			HttpResponse response = tryHttpPost(base,params,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jsonResponse = new JSONObject(responseString);
		        
		        return jsonResponse.optBoolean("success",false);
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}
	
	public class E621Vote
	{
		public boolean success = false;
		public int score = 0;
		public boolean removed_vote = false;
		
		public E621Vote(){};
		
		public E621Vote(int score, boolean removed_vote)
		{
			this.success = true;
			this.score = score;
			this.removed_vote = removed_vote;
		}
	}
	
	public E621Vote post__vote(int id, boolean up, String login, String password_hash)
	{
		String base = String.format("%s/post/vote.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("id", String.valueOf(id)));
		params.add(new BasicNameValuePair("score", (up?"1":"-1")));
		params.add(new BasicNameValuePair("login", login));
		params.add(new BasicNameValuePair("password_hash", password_hash));
		
		base += URLEncodedUtils.format(params, "utf-8");
		
		try
		{
			HttpResponse response = tryHttpPost(base,params,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jsonResponse = new JSONObject(responseString);
		        
		        if(jsonResponse.optBoolean("success",false))
		        {
		        	int change = (up?1:-1);
		        	return new E621Vote(jsonResponse.getInt("score"),(jsonResponse.getInt("change")*change) > 0);
		        }
		        else
		        {
		        	return new E621Vote();
		        }
		    }
		}
		catch (ClientProtocolException e)
		{
		}
		catch (IOException e)
		{
		}
		catch (JSONException e)
        {
		}
		
		return null;
	}
	
	public Boolean comment__create(int id, String body, String login, String password_hash)
	{
		String base = String.format("%s/comment/create.json?",DOMAIN_NAME);
		
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		
		params.add(new BasicNameValuePair("comment[post_id]", String.valueOf(id)));
		params.add(new BasicNameValuePair("comment[body]", body));
		params.add(new BasicNameValuePair("login", login));
		params.add(new BasicNameValuePair("password_hash", password_hash));
		
		try
		{
			HttpResponse response = tryHttpPost(base,params,5);
			
			StatusLine statusLine = response.getStatusLine();
			
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jsonResponse;
				
		        try {
					jsonResponse = new JSONObject(responseString);
				} catch (JSONException e) {
					return true;
				}
		        
		        if(jsonResponse.optBoolean("success",false))
		        {
		        	return false;
		        }
		        else
		        {
		        	return true;
		        }
		    }
		    
		    return true;
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	protected HttpResponse tryHttpGet(String url, Integer tries) throws ClientProtocolException, IOException
	{
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		HttpClient httpclient = new DefaultHttpClient(httpParams);
		
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
	
	protected HttpResponse tryHttpPost(String url, List<NameValuePair> pairs, Integer tries) throws ClientProtocolException, IOException
	{
		final HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
		HttpClient httpclient = new DefaultHttpClient(httpParams);
		
		for(;tries>=0; tries--)
		{
			try {
				HttpPost post = new HttpPost(url);
				post.setEntity(new UrlEncodedFormEntity(pairs));
				return httpclient.execute(post);
			} catch (ClientProtocolException e1) {
				continue;
			} catch (IOException e1) {
				continue;
			}
		}
		
		return httpclient.execute(new HttpGet(url));
	}
}
