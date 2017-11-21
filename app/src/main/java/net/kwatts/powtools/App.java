package net.kwatts.powtools;


import android.app.Application;
import android.arch.persistence.room.Room;
import android.os.PowerManager;

import net.kwatts.powtools.database.Database;
import net.kwatts.powtools.util.SharedPreferencesUtil;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class App extends Application {

    public static App INSTANCE = null;
    private SharedPreferencesUtil sharedPreferencesUtil = null;
    PowerManager.WakeLock mWakeLock;
    public Database db;


    public App() {
        INSTANCE = this;
    }

    public SharedPreferencesUtil getSharedPreferences() {
        if (sharedPreferencesUtil == null) {
            sharedPreferencesUtil = new SharedPreferencesUtil(App.this);
        }
        return sharedPreferencesUtil;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initWakeLock();
        initDatabase();
    }

    private void initDatabase() {
        db = Room.databaseBuilder(getApplicationContext(),
                Database.class, "database-name-pow")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pOWToolsWakeLock");
    }

    public void acquireWakeLock() {
        mWakeLock.acquire();
    }

    public void releaseWakeLock() {
        mWakeLock.release();
    }
}