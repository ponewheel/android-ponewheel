package net.kwatts.powtools.util;

public interface SharedPreferences {

    boolean getChargeAlertEnabled();

    boolean getSpeedAlertEnabled();

    float getSpeedAlert();

    int getChargeAlert();

    void saveChargeAlertEnabled(boolean isChecked);

    void saveChargeAlert(int chargeAlert);

    void saveSpeedAlertEnabled(boolean isChecked);

    void saveSpeedAlert(float speedAlert);
}
