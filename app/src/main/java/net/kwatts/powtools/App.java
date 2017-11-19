package net.kwatts.powtools;


import android.app.Application;
import android.content.SharedPreferences;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class App extends Application {

    public static App INSTANCE = null;
    private SharedPreferencesUtil sharedPreferencesUtil = null;

    public App() {
        INSTANCE = this;
    }

    public SharedPreferencesUtil getSharedPreferences() {
        if (sharedPreferencesUtil == null) {
            sharedPreferencesUtil = new SharedPreferencesUtil();
        }
        return sharedPreferencesUtil;
    }
}