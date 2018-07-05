package net.kwatts.powtools.view;

import android.support.annotation.NonNull;

import net.kwatts.powtools.util.SharedPreferences;

import net.kwatts.powtools.util.SpeedAlertResolver;
import timber.log.Timber;

public class AlertsPresenter implements AlertsMvpController.Presenter {

    private AlertsMvpController.View view;
    private boolean isChargeAlarmPlaying;
    private boolean isSpeedAlarmPlaying;
    private SharedPreferences sharedPreferences;
    private final SpeedAlertResolver speedAlertResolver;

    public AlertsPresenter(
            @NonNull AlertsMvpController.View view,
            @NonNull SharedPreferences sharedPreferences,
            SpeedAlertResolver speedAlertResolver
    ) {
        this.view = view;
        this.sharedPreferences = sharedPreferences;
        this.speedAlertResolver = speedAlertResolver;

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
        if (!isChecked && isChargeAlarmPlaying) {
            isChargeAlarmPlaying = false;
            view.playSound(false);
        }
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
        if (!isChecked && isSpeedAlarmPlaying) {
            isSpeedAlarmPlaying = false;
            view.playSound(false);
        }
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
        if(speedAlertResolver.isAlertThresholdExceeded(speedString)) {
            if(!isSpeedAlarmPlaying) {
                isSpeedAlarmPlaying = true;
                view.playSound(true);
            }
        } else  {
            if(isSpeedAlarmPlaying) {
                isSpeedAlarmPlaying = false;
                view.playSound(false);
            }
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
            isChargeAlarmPlaying = false;
            view.playSound(false);
        }
    }
}
