package info.beastarman.e621.middleware;

import java.io.IOException;

import info.beastarman.e621.api.E621Search;

public class E621SearchGenerator
{
	final String tags;
	final int page;
	final int limit;
	E621Search searchCache = null;

	public E621SearchGenerator(String tags, int page, int limit)
	{
		this.tags = tags;
		this.limit = limit;
		this.page = page;
	}

	public E621Search generate() throws IOException
	{
		if(searchCache == null)
		{
			searchCache = E621Middleware.getInstance().post__index(tags, page, limit);
		}

		return searchCache;
	}

	public E621SearchGenerator nextGenerator() throws IOException
	{
		E621Search search = generate();

		if(!search.has_next_page())
		{
			return null;
		}

		return new E621SearchGenerator(tags, page + 1, limit);
	}
}
