package net.kwatts.powtools.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferencesUtil {

    public static final String METRIC_UNITS = "metricUnits";
    private static final String DAY_NIGHT_MODE = "dayNightMode";
    public static final String DARK_NIGHT_MODE = "darkNightMode";
    private static final String EULA_AGREE = "eula_agree";
    private static final String DEBUG_WINDOW = "debugWindow";
    private static final String ONE_WHEEL_PLUS = "oneWheelPlus";
    private static final String LOGGING_FREQUENCY = "loggingFrequency";
    private static final String TRIP_LOGGING = "tripLogging";
    public static final String LOG_LOCATIONS = "logLocations";
    public static final String DEVICE_RECONNECT = "deviceReconnect";
    public static final String OW_MAC_ADDRESS = "ow_mac_address";
    public static final String OW_MAC_NAME = "ow_mac_name";

    private SharedPreferences mSharedPref;


    public SharedPreferencesUtil(Context context) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);


    }

    public boolean isEulaAgreed() {
        return mSharedPref.getBoolean(EULA_AGREE, false);
    }
    public void setEulaAgreed(boolean isAgreed) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(EULA_AGREE, isAgreed);
        editor.commit();
    }

    public int getLoggingFrequency() {
        try {
            return Integer.valueOf(mSharedPref.getString(LOGGING_FREQUENCY, "1000"));

        } catch (ClassCastException e) {
            int logFrequency =  mSharedPref.getInt(LOGGING_FREQUENCY, 1000);

            // TODO remove this at some point in the future when everyone is int (
            // PreferencesActivity doesn't like ints
            mSharedPref.edit()
                    .remove(LOGGING_FREQUENCY)
                    .putString(LOGGING_FREQUENCY, logFrequency+"")
                    .apply();

            return logFrequency;
        }
    }

    public boolean isDayNightMode() {
        return mSharedPref.getBoolean(DAY_NIGHT_MODE, false);
    }

    public boolean isDarkNightMode() {
        return mSharedPref.getBoolean(DARK_NIGHT_MODE, false);
    }

    public boolean isLoggingEnabled() {
        return mSharedPref.getBoolean(TRIP_LOGGING, false);
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public Boolean isDebugging() {
        return mSharedPref.getBoolean(DEBUG_WINDOW, false);
    }

    public Boolean isOneWheelPlus() {
        return mSharedPref.getBoolean(ONE_WHEEL_PLUS, false);
    }

    public void removeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public boolean shouldAutoReconnect() {
        return mSharedPref.getBoolean(DEVICE_RECONNECT, false);
    }

    public void saveMacAddress(String macAdress, String macAddressName) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(OW_MAC_ADDRESS, macAdress);
        editor.putString(OW_MAC_NAME,macAddressName);
        editor.commit();
    }

    public boolean isMetric() {
        return mSharedPref.getBoolean(METRIC_UNITS, false);
    }
}
