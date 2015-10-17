package info.beastarman.e621.backend;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by beastarman on 10/17/2015.
 */
public class JsonObjectResponseHandler implements ResponseHandler
{
	@Override
	public JSONObject handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException
	{
		try
		{
			return new JSONObject(httpResponse.getEntity().toString());
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
