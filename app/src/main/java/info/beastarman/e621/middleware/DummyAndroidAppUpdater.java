package info.beastarman.e621.middleware;

/**
 * Created by Douglas on 27/03/2016.
 */
public class DummyAndroidAppUpdater implements AndroidAppUpdaterInterface {
    @Override
    public void setBeta(boolean beta) {

    }

    @Override
    public AndroidAppVersion getLatestVersionInfo() {
        return null;
    }
}
