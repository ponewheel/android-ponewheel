package net.kwatts.powtools.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static net.kwatts.powtools.view.AlertsMvpController.ENGLISH_DEFAULT_SPEED_ALARM;
import static net.kwatts.powtools.view.AlertsMvpController.METRIC_DEFAULT_SPEED_ALARM;

public class SharedPreferencesUtil implements net.kwatts.powtools.util.SharedPreferences{

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
    private static final String CHARGE_ALERT = "CHARGE_ALERT";
    public static final String SPEED_ALERT = "SPEED_ALERT";
    private static final String CHARGE_ALERT_ENABLED = "CHARGE_ALERT_ENABLED";
    private static final String SPEED_ALERT_ENABLED = "SPEED_ALERT_ENABLED";

    private SharedPreferences androidSharedPreferences;


    public SharedPreferencesUtil(Context context) {
        androidSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void putInt(int value, String key) {
        SharedPreferences.Editor editor = androidSharedPreferences.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void putBool(boolean value, String key) {
        SharedPreferences.Editor editor = androidSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public boolean isEulaAgreed() {
        return androidSharedPreferences.getBoolean(EULA_AGREE, false);
    }
    public void setEulaAgreed(boolean isAgreed) {
        SharedPreferences.Editor editor = androidSharedPreferences.edit();
        editor.putBoolean(EULA_AGREE, isAgreed);
        editor.commit();
    }

    public int getLoggingFrequency() {
        try {
            return Integer.valueOf(androidSharedPreferences.getString(LOGGING_FREQUENCY, "1000"));

        } catch (ClassCastException e) {
            int logFrequency =  androidSharedPreferences.getInt(LOGGING_FREQUENCY, 1000);

            // TODO remove this at some point in the future when everyone is int (
            // PreferencesActivity doesn't like ints
            androidSharedPreferences.edit()
                    .remove(LOGGING_FREQUENCY)
                    .putString(LOGGING_FREQUENCY, logFrequency+"")
                    .apply();

            return logFrequency;
        }
    }

    public boolean isDayNightMode() {
        return androidSharedPreferences.getBoolean(DAY_NIGHT_MODE, false);
    }

    public boolean isDarkNightMode() {
        return androidSharedPreferences.getBoolean(DARK_NIGHT_MODE, false);
    }

    public boolean isLoggingEnabled() {
        return androidSharedPreferences.getBoolean(TRIP_LOGGING, false);
    }

    public boolean isLocationsEnabled() {
        return androidSharedPreferences.getBoolean(LOG_LOCATIONS, false);
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        androidSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public Boolean isDebugging() {
        return androidSharedPreferences.getBoolean(DEBUG_WINDOW, false);
    }

    public Boolean isOneWheelPlus() {
        return androidSharedPreferences.getBoolean(ONE_WHEEL_PLUS, false);
    }

    public void removeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        androidSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public boolean shouldAutoReconnect() {
        return androidSharedPreferences.getBoolean(DEVICE_RECONNECT, false);
    }

    public void saveMacAddress(String macAdress, String macAddressName) {
        SharedPreferences.Editor editor = androidSharedPreferences.edit();
        editor.putString(OW_MAC_ADDRESS, macAdress);
        editor.putString(OW_MAC_NAME,macAddressName);
        editor.commit();
    }

    public boolean isMetric() {
        return androidSharedPreferences.getBoolean(METRIC_UNITS, false);
    }

    public float getSpeedAlert() {

        float defaultSpeedAlert = isMetric() ? METRIC_DEFAULT_SPEED_ALARM : ENGLISH_DEFAULT_SPEED_ALARM;

        return androidSharedPreferences.getFloat(SPEED_ALERT, defaultSpeedAlert);
    }

    public void saveSpeedAlert(float speedAlert) {
        SharedPreferences.Editor editor = androidSharedPreferences.edit();
        editor.putFloat(SPEED_ALERT, speedAlert);
        editor.commit();
    }
    public int getChargeAlert() {
        return androidSharedPreferences.getInt(CHARGE_ALERT, 95);
    }

    public void saveChargeAlert(int chargeAlert) {
        putInt(chargeAlert, CHARGE_ALERT);
    }

    public void saveChargeAlertEnabled(boolean isChargeAlertEnabled) {
        putBool(isChargeAlertEnabled, CHARGE_ALERT_ENABLED);
    }

    public boolean getChargeAlertEnabled() {
        return androidSharedPreferences.getBoolean(CHARGE_ALERT_ENABLED, false);
    }

    public void saveSpeedAlertEnabled(boolean isSpeedAlertEnabled) {
        putBool(isSpeedAlertEnabled, SPEED_ALERT_ENABLED);
    }

    public boolean getSpeedAlertEnabled() {
        return androidSharedPreferences.getBoolean(SPEED_ALERT_ENABLED, false);
    }
}
