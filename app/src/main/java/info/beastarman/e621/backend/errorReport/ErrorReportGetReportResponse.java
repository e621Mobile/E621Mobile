package info.beastarman.e621.backend.errorReport;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by beastarman on 10/19/2015.
 */
public class ErrorReportGetReportResponse extends ErrorReportResponse
{
	ErrorReportReport report = null;

	public ErrorReportGetReportResponse(JSONObject jsonObject)
	{
		super(jsonObject);

		if(isSuccessfull())
		{
			try
			{
				report = new ErrorReportReport(jsonObject.getJSONObject("response"));
			}
			catch(JSONException e)
			{
				e.printStackTrace();
			}
		}
	}
}
