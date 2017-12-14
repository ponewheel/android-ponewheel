package net.kwatts.powtools.view;

import android.support.annotation.NonNull;

import net.kwatts.powtools.util.SharedPreferences;

import timber.log.Timber;

public class AlertsPresenter implements AlertsMvpController.Presenter {

    private AlertsMvpController.View view;
    private boolean isChargeAlarmPlaying;
    private boolean isSpeedAlarmPlaying;
    private SharedPreferences sharedPreferences;

    public AlertsPresenter(@NonNull AlertsMvpController.View view, @NonNull SharedPreferences sharedPreferences) {
        this.view = view;
        this.sharedPreferences = sharedPreferences;

        view.setPresenter(this);

        view.setChargeEnabled(this.sharedPreferences.getChargeAlertEnabled());
        view.setSpeedEnabled(this.sharedPreferences.getSpeedAlertEnabled());

        view.setSpeedAlert(this.sharedPreferences.getSpeedAlert());
        view.setChargeAlert(this.sharedPreferences.getChargeAlert());

    }

    @Override
    public void onChargeAlertCheckChanged(boolean isChecked) {
        view.setChargeEnabled(isChecked);
        sharedPreferences.saveChargeAlertEnabled(isChecked);
    }

    @Override
    public void onChargeValueChanged(@NonNull String speed) {
        int chargeAlert;
        try {
            chargeAlert = Integer.parseInt(speed);
            sharedPreferences.saveChargeAlert(chargeAlert);
        }
        catch(NumberFormatException nfe) {
            view.showNumberFormatError();

        }
    }

    @Override
    public void onSpeedAlertCheckChanged(boolean isChecked) {
        view.setSpeedEnabled(isChecked);
        sharedPreferences.saveSpeedAlertEnabled(isChecked);
    }

    @Override
    public void onSpeedAlertValueChanged(@NonNull String speed) {
        float speedAlert;
        try {
            speedAlert = Float.parseFloat(speed);
            sharedPreferences.saveSpeedAlert(speedAlert);
        }
        catch(NumberFormatException nfe) {
            view.showNumberFormatError();
        }
    }

    @Override
    public void handleSpeed(@NonNull String speedString) {
        float speed;
        try {
            speed = Float.parseFloat(speedString);
        } catch (NumberFormatException e) {
            Timber.e(e);
            return;
        }
        float speedAlert = sharedPreferences.getSpeedAlert();
        boolean isEnabled = sharedPreferences.getSpeedAlertEnabled();

        if (isEnabled && !isSpeedAlarmPlaying && speed >= speedAlert) {
            isSpeedAlarmPlaying = true;
            view.playSound(true);
        } else if (speed < speedAlert && isSpeedAlarmPlaying) {
            isSpeedAlarmPlaying = false;
            view.playSound(false);
        }
    }

    @Override
    public void handleChargePercentage(int percent) {
        float chargeAlert = sharedPreferences.getChargeAlert();
        boolean isEnabled = sharedPreferences.getChargeAlertEnabled();

        if (isEnabled && !isChargeAlarmPlaying && percent >= chargeAlert) {
            isChargeAlarmPlaying = true;
            view.playSound(true);
        } else if (percent < chargeAlert && isChargeAlarmPlaying) {
            view.playSound(false);
        }
    }
}
