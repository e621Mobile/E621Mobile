package info.beastarman.e621.middleware;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;

public class E621SearchGeneratorCombo
{
	ArrayList<E621SearchPack> packs = new ArrayList<E621SearchPack>();

	public E621SearchGeneratorCombo(List<String> searches)
	{
		for(String s : searches)
		{
			packs.add(new E621SearchPack(new E621SearchGenerator(s, 0, 100)));
		}
	}

	public int getCount() throws IOException
	{
		int count = 0;

		for(E621SearchPack pack : packs)
		{
			count += pack.getCount();
		}

		return count;
	}

	public List<E621Image> getImages(int page, int limit) throws IOException
	{
		int offset = page * limit;
		limit = offset + limit;

		List<E621Image> images = new ArrayList<E621Image>();

		for(E621SearchPack pack : packs)
		{
			if(limit == 0)
			{
				break;
			}
			if(pack.getCount() >= offset)
			{
				offset -= pack.getCount();
				limit -= pack.getCount();
			}
			else
			{
				List<E621Image> packImages = pack.getImages(offset, limit);
				images.addAll(packImages);

				offset = 0;
				limit -= packImages.size();
			}
		}

		return images;
	}

	private class E621SearchPack
	{
		Integer count = null;
		ArrayList<E621Image> images = new ArrayList<E621Image>();
		E621SearchGenerator generator;

		public E621SearchPack(E621SearchGenerator generator)
		{
			this.generator = generator;
		}

		private void retrieveSome() throws IOException
		{
			E621Search search = generator.generate();
			generator = generator.nextGenerator();

			if(count == null)
			{
				count = search.count;
			}

			images.addAll(search.images);
		}

		public int getCount() throws IOException
		{
			if(count == null)
			{
				retrieveSome();
			}

			return count;
		}

		public List<E621Image> getImages(int offset, int limit) throws IOException
		{
			limit = Math.min(limit, getCount());

			while(images.size() < limit && generator != null)
			{
				retrieveSome();
			}

			return images.subList(offset, limit);
		}
	}
}
