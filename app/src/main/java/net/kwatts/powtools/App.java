package net.kwatts.powtools;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.os.PowerManager;

import com.facebook.stetho.Stetho;

import net.kwatts.powtools.database.DBExecutor;
import net.kwatts.powtools.database.Database;
import net.kwatts.powtools.util.SharedPreferencesUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * This is a subclass of {@link Application} used to provide shared objects for this app.
 */
public class App extends Application {

    public static App INSTANCE = null;
    private SharedPreferencesUtil sharedPreferencesUtil = null;
    PowerManager.WakeLock wakeLock;
    public Database db;
    public ExecutorService dbExecutor;


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
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
            Timber.plant(new Timber.DebugTree());
        }
        initWakeLock();
        initDatabase();
    }

    private void initDatabase() {
        db = Room.databaseBuilder(getApplicationContext(),
                Database.class, "database-name-pow")
                .fallbackToDestructiveMigration()
                .build();
        dbExecutor = Executors.newSingleThreadExecutor();
    }

    public static void dbExecute(DBExecutor dbExecutor) {
        INSTANCE.dbExecutor.execute( () -> dbExecutor.run(INSTANCE.db));
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pOWToolsWakeLock");
    }

    public void acquireWakeLock() {
        wakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}