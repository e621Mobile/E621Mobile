package info.beastarman.e621.backend.errorReport;

import org.json.JSONObject;

/**
 * Created by beastarman on 10/17/2015.
 */
public class ErrorReportReportResponse extends ErrorReportResponse
{
	private String hash = null;

	public ErrorReportReportResponse(JSONObject jsonObject)
	{
		super(jsonObject);

		if(isSuccessfull())
		{
			hash = jsonObject.optString("response", "");
		}
	}

	public String getHash()
	{
		return hash;
	}
}
