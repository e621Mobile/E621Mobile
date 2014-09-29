package info.beastarman.e621.api;

import java.util.ArrayList;
import java.util.Iterator;

public class DText implements Iterable<DTextObject>, DTextObject
{
	ArrayList<DTextObject> objects;
	
	public DText(String text)
	{
		objects = new ArrayList<DTextObject>();
		
		parse(text);
	}

	@Override
	public Iterator<DTextObject> iterator()
	{
		return objects.iterator();
	}

	@Override
	public void parse(String text)
	{
		objects.clear();
		
		DTextString obj = new DTextString();
		obj.parse(text);
		
		objects.add(obj);
	}

	@Override
	public String raw()
	{
		String ret = "";
		
		for(DTextObject obj : objects)
		{
			ret += obj.raw();
		}
		
		return ret;
	}
}
