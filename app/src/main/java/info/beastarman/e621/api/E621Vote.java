package info.beastarman.e621.api;

public class E621Vote
{
	public boolean success = false;
	public int score = 0;
	public boolean removed_vote = false;
	
	public E621Vote(){};
	
	public E621Vote(int score, boolean removed_vote)
	{
		this.success = true;
		this.score = score;
		this.removed_vote = removed_vote;
	}
}