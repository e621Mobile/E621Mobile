package info.beastarman.e621.backend;

public class Pair<T,U>
{
	public final T left;
	public final U right;
	
	public Pair(T left, U right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public String toString()
	{
		return "<" + this.left.toString() + "," + this.right.toString() + ">";
	}
}
