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
				ors.add(s);
			}
			else if(s.startsWith("-"))
			{
				nots.add(s);
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
				sql = sql + String.format(" OR EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s.substring(1));
			}
			
			sql = sql + " ) ";
		}
		
		if(nots.size() > 0)
		{
			sql = sql + " AND NOT ( 0 ";
			
			for(String s : nots)
			{
				sql = sql + String.format(" OR EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s.substring(1));
			}
			
			sql = sql + " ) ";
		}
		
		return sql;
	}
}
