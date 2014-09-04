package info.beastarman.e621.backend;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class PersistentHttpClient implements HttpClient
{
	private HttpClient client;
	private int tries;
	
	public PersistentHttpClient(HttpClient client, int tries)
	{
		this.client = client;
		this.tries = tries;
	}
	
	@Override
	public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(request);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(request);
	}

	@Override
	public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(request,context);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(request,context);
	}

	@Override
	public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(target, request);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(target, request);
	}

	@Override
	public <T> T execute(HttpUriRequest arg0, ResponseHandler<? extends T> arg1) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(arg0, arg1);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(arg0, arg1);
	}

	@Override
	public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(target, request, context);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(target, request, context);
	}

	@Override
	public <T> T execute(HttpUriRequest arg0, ResponseHandler<? extends T> arg1, HttpContext arg2) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(arg0, arg1, arg2);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(arg0, arg1, arg2);
	}

	@Override
	public <T> T execute(HttpHost arg0, HttpRequest arg1, ResponseHandler<? extends T> arg2) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(arg0, arg1, arg2);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(arg0, arg1, arg2);
	}

	@Override
	public <T> T execute(HttpHost arg0, HttpRequest arg1, ResponseHandler<? extends T> arg2, HttpContext arg3) throws IOException, ClientProtocolException
	{
		for(int i=0; i<tries-1; i++)
		{
			try
			{
				return client.execute(arg0, arg1, arg2, arg3);
			}
			catch(IOException e)
			{
			}
		}
		
		return client.execute(arg0, arg1, arg2, arg3);
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams()
	{
		return client.getParams();
	}
}
