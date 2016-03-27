package info.beastarman.e621.middleware;

/**
 * Created by Douglas on 27/03/2016.
 */
public class AndroidAppVersion {
    public int versionCode;
    public String versionName;
    public String apkURL;
    public String domain;

	public AndroidAppVersion(int versionCode, String versionName, String apkURL, String domain) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.apkURL = apkURL;
        this.domain = domain;
    }

	public String getFullApkURL() {
        return domain + apkURL;
    }
}
