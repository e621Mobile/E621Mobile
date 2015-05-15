package info.beastarman.e621.middleware;

import java.io.IOException;
import java.util.ArrayList;

import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;

public class OnlineImageNavigator extends ImageNavigator
{
	private static final long serialVersionUID = 6801245286947782850L;
	
	E621Image img;
	int position;
	String query;
	ArrayList<E621Image> cache;
	int cache_offset;
	int total;
	int results_per_page;
	
	public OnlineImageNavigator(E621Image img, int position, String query, int results_per_page, ArrayList<E621Image> cache, int cache_offset, int total)
	{
		this.img = img;
		this.position = position;
		this.query = query;
		this.cache = cache;
		this.cache_offset = cache_offset;
		this.total = total;
		this.results_per_page = results_per_page;
	}
	
	public OnlineImageNavigator(E621Image img, int position, String query, int results_per_page, E621Search search)
	{
		this.img = img;
		this.position = position;
		this.query = query;
		this.cache = new ArrayList<E621Image>(search.images);
		this.cache_offset = search.offset;
		this.total = search.count;
		this.results_per_page = results_per_page;
	}
	
	public E621Search search(String query, int page, int limit) throws IOException
	{
		return E621Middleware.getInstance().post__index(query, page, limit);
	}

	@Override
	public Integer getPosition()
	{
		return position;
	}

	@Override
	public Integer getCount()
	{
		return total;
	}

	@Override
	public ImageNavigator next()
	{
		int new_position = position+1;
		
		if(new_position >= total)
		{
			return null;
		}

		while(!(new_position < cache_offset + cache.size()))
		{
			int page_append = (int) Math.floor(((double)cache_offset + cache.size())/results_per_page);
			int slice_from = (cache_offset + cache.size()) % results_per_page;
			
			ArrayList<E621Image> results;
			
			try {
				results = search(query, page_append, results_per_page).images;
			} catch (IOException e) {
				return null;
			}
			
			if(results == null)
			{
				return null;
			}
			
			if(results.size() <= slice_from)
			{
				return null;
			}
			
			results.subList(0,slice_from).clear();
			
			cache.addAll(results);
		}
		
		if(new_position < cache_offset + cache.size())
		{
			return new OnlineImageNavigator(cache.get(new_position - cache_offset),new_position,query,results_per_page, cache,cache_offset,total);
		}
		
		return null;
	}
	
	@Override
	public ImageNavigator prev()
	{
		int new_position = position-1;
		
		if(new_position < 0)
		{
			return null;
		}
		
		while(!(new_position >= cache_offset))
		{
			int page_append = (int) Math.floor(((double)cache_offset -1)/results_per_page);
			int slice_to = ((cache_offset - 1) % results_per_page) + 1;
			
			ArrayList<E621Image> results;
			
			try {
				results = search(query, page_append, results_per_page).images;
			} catch (IOException e) {
				return null;
			}
			
			if(results == null)
			{
				return null;
			}
			
			if(results.size() < slice_to)
			{
				return null;
			}
			
			results.subList(slice_to,results.size()).clear();
			
			cache.addAll(0,results);
			
			cache_offset -= results.size();
		}
		
		if(new_position >= cache_offset)
		{
			return new OnlineImageNavigator(cache.get(new_position - cache_offset),new_position,query,results_per_page, cache,cache_offset,total);
		}
		
		return null;
	}

	@Override
	public Integer getId() {
		return img.id;
	}

	public String toString()
	{
		return String.valueOf(getId());
	}
}
