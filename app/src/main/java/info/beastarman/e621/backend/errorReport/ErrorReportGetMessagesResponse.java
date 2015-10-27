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
	int maxId = 0;

	public ErrorReportGetMessagesResponse(JSONObject jsonObject)
	{
		this(jsonObject,null);
	}

	public ErrorReportGetMessagesResponse(JSONObject jsonObject,String report)
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
					ErrorReportMessage m = new ErrorReportMessage(jsonArray.getJSONObject(i));
					m.reportHash = report;
					messages.add(m);
					maxId = Math.max(maxId,m.local_id);
				}
			}
			catch(JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public ArrayList<ErrorReportMessage> getMessages()
	{
		return messages;
	}
}
