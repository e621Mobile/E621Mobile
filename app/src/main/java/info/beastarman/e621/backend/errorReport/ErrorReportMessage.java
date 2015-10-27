package info.beastarman.e621.backend.errorReport;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ErrorReportMessage
{
	public String text;
	public String author;
	public int local_id;
	public Date time;
	public String reportHash = null;

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-d HH:mm:ss.SSSSSSZZZZZ", Locale.US);

	public ErrorReportMessage(JSONObject jsonObject)
	{
		text = jsonObject.optString("text","");
		author = jsonObject.optString("author", "");
		local_id = jsonObject.optInt("local_id", 0);

		try
		{
			time = DATE_FORMAT.parse(jsonObject.optString("time",""));
		}
		catch(ParseException e)
		{
			e.printStackTrace();
			time = null;
		}
	}
}
