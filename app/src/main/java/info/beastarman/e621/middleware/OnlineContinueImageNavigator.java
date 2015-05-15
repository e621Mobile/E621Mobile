package info.beastarman.e621.middleware;

import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;

import java.io.IOException;

public class OnlineContinueImageNavigator extends OnlineImageNavigator
{
	private static final long serialVersionUID = -4181810174074433903L;

	public OnlineContinueImageNavigator(E621Image img, int position, String query, int results_per_page, E621Search search)
	{
		super(img, position, query, results_per_page, search);
	}
	
	@Override
	public E621Search search(String query, int page, int limit) throws IOException
	{
		return E621Middleware.getInstance().continue_search(query, page, limit);
	}
}
