package info.beastarman.e621.backend.errorReport;

import org.json.JSONObject;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportResponse
{
	boolean success;

	public ErrorReportResponse(JSONObject jsonObject)
	{
		this.success = jsonObject.optString("status","").equals("success");
	}

	public boolean isSuccessfull()
	{
		return success;
	}
}
