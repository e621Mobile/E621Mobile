package info.beastarman.e621.backend;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public interface ImageCacheManagerInterface {

	SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss", Locale.getDefault());

	boolean hasFile(String id);

	InputStream getFile(String id);

	boolean createOrUpdate(String id, InputStream in);

	void removeFile(String id);

	boolean hasSpaceLeft();

	void clean();

	long totalSize();

	String[] fileList();

	void setMaxSize(long maxSize);

	void removeFiles(String[] ids);

	void clear();

}