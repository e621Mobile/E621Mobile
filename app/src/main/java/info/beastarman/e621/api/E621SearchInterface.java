package info.beastarman.e621.api;

import java.io.Serializable;

/**
 * Created by douglas on 27/01/15.
 */
public interface E621SearchInterface extends Serializable
{
	boolean has_prev_page();

	boolean has_next_page();

	int current_page();

	int current_page(int diff);

	int total_pages();
}
