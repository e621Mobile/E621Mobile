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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	
	public ArrayList<E621Image> post__index(String tags, Integer page, Integer limit) throws IOException
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
				
				return images;
	        } catch (ParserConfigurationException e) {
				e.printStackTrace();
				return new ArrayList<E621Image>();
				
			} catch (SAXException e) {
				e.printStackTrace();
				return new ArrayList<E621Image>();
			}
	        
	    } else{
	        //Closes the connection.
	        response.getEntity().getContent().close();
	        throw new IOException(statusLine.getReasonPhrase());
	    }
	}
	
	protected HttpResponse tryHttpGet(String url, Integer tries) throws ClientProtocolException, IOException
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
}
