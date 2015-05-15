package info.beastarman.e621.backend;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public interface ImageCacheManagerInterface
{

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat(
																				  "yyyy-MM-dd HH:mm:ss", Locale.getDefault());

	public abstract boolean hasFile(String id);

	public abstract InputStream getFile(String id);

	public abstract boolean createOrUpdate(String id, InputStream in);

	public abstract void removeFile(String id);

	public abstract void clean();

	public abstract long totalSize();

	public abstract String[] fileList();

	public void setMaxSize(long maxSize);

	void removeFiles(String[] ids);

	void clear();

}