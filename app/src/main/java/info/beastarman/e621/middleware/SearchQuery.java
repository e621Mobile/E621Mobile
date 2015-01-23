package info.beastarman.e621.middleware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class SearchQuery
{
	public ArrayList<String> ands = new ArrayList<String>();
	public ArrayList<String> ors = new ArrayList<String>();
	public ArrayList<String> nots = new ArrayList<String>();
	
	public SearchQuery(String query)
	{
		String[] tags = query.trim().split("\\s");
		
		for(String s : tags)
		{
			if(s.startsWith("~"))
			{
				ors.add(s.substring(1));
			}
			else if(s.startsWith("-"))
			{
				nots.add(s.substring(1));
			}
			else
			{
				if(s.length() > 0)
				{
					ands.add(s);
				}
			}
		}

		Collections.sort(ands);
		Collections.sort(ors);
		Collections.sort(nots);
	}
	
	public static String normalize(String query)
	{
		return new SearchQuery(query).normalize();
	}
	
	public String normalize()
	{
		String ret = "";
		
		for(String s : ands)
		{
			ret = ret + " " + s;
		}
		
		for(String s : ors)
		{
			ret = ret + " ~" + s;
		}
		
		for(String s : nots)
		{
			ret = ret + " -" + s;
		}
		
		return ret.trim().toLowerCase(new Locale("en_US"));
	}
}
