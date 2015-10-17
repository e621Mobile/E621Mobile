package info.beastarman.e621.backend.errorReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportGetMessagesResponse extends ErrorReportResponse
{
	ArrayList<ErrorReportMessage> messages;

	public ErrorReportGetMessagesResponse(JSONObject jsonObject)
	{
		super(jsonObject);

		if(isSuccessfull())
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("response");
				messages = new ArrayList<ErrorReportMessage>();

				for(int i=0; i<jsonArray.length(); i++)
				{
					messages.add(new ErrorReportMessage(jsonArray.getJSONObject(i)));
				}
			}
			catch(JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
}
