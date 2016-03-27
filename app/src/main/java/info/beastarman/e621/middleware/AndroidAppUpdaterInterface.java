package info.beastarman.e621.middleware;

/**
 * Created by Douglas on 27/03/2016.
 */
public interface AndroidAppUpdaterInterface {
    void setBeta(boolean beta);

    AndroidAppVersion getLatestVersionInfo();
}
