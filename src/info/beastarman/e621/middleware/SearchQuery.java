package info.beastarman.e621.middleware;

import info.beastarman.e621.api.E621Image;

import java.util.ArrayList;
import java.util.Collections;

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

		Collections.sort(ands);
		Collections.sort(ors);
		Collections.sort(nots);
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
			if(s.contains(":"))
			{
				String meta = s.split(":")[0];
				String value= s.split(":")[1];
				
				if(meta.equals("rating"))
				{
					value = value.substring(0, 1);
					
					if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
					{
						sql = sql + " AND";
						sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
					}
				}
			}
			else
			{
				sql = sql + " AND";
				sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
			}
		}
		
		if(ors.size() > 0)
		{
			sql = sql + " AND ( 0 ";
			
			for(String s : ors)
			{
				if(s.contains(":"))
				{
					String meta = s.split(":")[0];
					String value= s.split(":")[1];
					
					if(meta.equals("rating"))					{
						value = value.substring(0, 1);
						
						if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
						{
							sql = sql + " OR";
							sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
						}
					}
				}
				else
				{
					sql = sql + " OR";
					sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
				}
			}
			
			sql = sql + " ) ";
		}
		
		if(nots.size() > 0)
		{
			sql = sql + " AND NOT ( 0 ";
			
			for(String s : nots)
			{
				if(s.contains(":"))
				{
					String meta = s.split(":")[0];
					String value= s.split(":")[1];
					
					if(meta.equals("rating"))
					{
						value = value.substring(0, 1);
						
						if(value.equals(E621Image.SAFE) || value.equals(E621Image.QUESTIONABLE) || value.equals(E621Image.EXPLICIT))
						{
							sql = sql + " OR";
							sql = sql + String.format(" EXISTS(SELECT 1 FROM e621image WHERE image=id AND rating=\"%1$s\") ", value);
						}
					}
				}
				else
				{
					sql = sql + " OR";
					sql = sql + String.format(" EXISTS(SELECT 1 FROM image_tags WHERE image=id AND tag=\"%1$s\") ", s);
				}
			}
			
			sql = sql + " ) ";
		}
		
		return sql;
	}
}
