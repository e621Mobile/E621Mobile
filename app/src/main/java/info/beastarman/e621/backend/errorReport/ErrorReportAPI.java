package info.beastarman.e621.backend.errorReport;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import info.beastarman.e621.backend.JsonObjectResponseHandler;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportAPI
{
	private HttpClient client;
	private String base_url;

	public ErrorReportAPI(HttpClient client, String base_url)
	{
		this.client = client;
		this.base_url = base_url;
	}

	public ErrorReportReportResponse report(String app, String text, String log, List<String> tags) throws IOException
	{
		URI uri = URI.create(String.format("%s%s/",base_url,app));

		List<NameValuePair> params = new LinkedList<NameValuePair>();

		params.add(new BasicNameValuePair("text", text));
		params.add(new BasicNameValuePair("log", log));

		for(String tag : tags)
		{
			params.add(new BasicNameValuePair("tag", tag));
		}

		HttpPost post = new HttpPost(uri);
		post.setEntity(new UrlEncodedFormEntity(params));

		return new ErrorReportReportResponse(client.<JSONObject>execute(post, new JsonObjectResponseHandler()));
	}

	public ErrorReportGetMessagesResponse getMessages(String report) throws IOException
	{
		URI uri = URI.create(String.format("%sreports/%s/messages/",base_url,report));

		HttpGet get = new HttpGet(uri);

		return new ErrorReportGetMessagesResponse(client.<JSONObject>execute(get, new JsonObjectResponseHandler()), report);
	}

	public ErrorReportResponse addMessage(String report, String message, String author) throws IOException
	{
		URI uri = URI.create(String.format("%sreports/%s/messages/add/",base_url,report));

		List<NameValuePair> params = new LinkedList<NameValuePair>();

		params.add(new BasicNameValuePair("message", message));
		params.add(new BasicNameValuePair("author", author));

		HttpPost post = new HttpPost(uri);
		post.setEntity(new UrlEncodedFormEntity(params));

		return new ErrorReportResponse(client.<JSONObject>execute(post, new JsonObjectResponseHandler()));
	}
}
