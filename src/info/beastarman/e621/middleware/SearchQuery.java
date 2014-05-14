package info.beastarman.e621.middleware;

import java.util.ArrayList;

public class SearchQuery
{
	ArrayList<String> ands = new ArrayList<String>();
	ArrayList<String> ors = new ArrayList<String>();
	ArrayList<String> nots = new ArrayList<String>();
	
	public SearchQuery(String query)
	{
		String[] tags = query.trim().replace("\"", "").replace("\'", "").split("\\s");
		
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
		
		return ret.trim();
	}
	
	public String toSql()
	{
		String sql = " 1 ";
		
		for(String s : ands)
		{
			sql = sql + String.format(" AND EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
		}
		
		if(ors.size() > 0)
		{
			sql = sql + " AND ( 0 ";
			
			for(String s : ors)
			{
				sql = sql + String.format(" OR EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
			}
			
			sql = sql + " ) ";
		}
		
		if(nots.size() > 0)
		{
			sql = sql + " AND NOT ( 0 ";
			
			for(String s : nots)
			{
				sql = sql + String.format(" OR EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
			}
			
			sql = sql + " ) ";
		}
		
		return sql;
	}
}
