package net.kwatts.powtools.view;

import android.app.Activity;

import com.github.anastr.speedviewlib.ProgressiveGauge;

import net.kwatts.powtools.App;

public class AlertsMvpController {
    public static final float METRIC_DEFAULT_SPEED_ALARM = 27f;
    public static final float ENGLISH_DEFAULT_SPEED_ALARM = 17f;

    private final View view;
    private final Presenter presenter;

    public interface View {
        void setPresenter(Presenter alertsPresenter);
        void showNumberFormatError();
        void setChargeEnabled(boolean isChecked);
        void setSpeedEnabled(boolean isChecked);
        void setSpeedAlert(float speedAlert);
        void setChargeAlert(int chargeAlert);
        void releaseMedia();
        void playSound(boolean shouldPlay);
        void recaptureMedia(Activity activity);
    }
    public interface Presenter {

        void onChargeAlertCheckChanged(boolean isChecked);
        void onChargeValueChanged(String charge);
        void onSpeedAlertCheckChanged(boolean isChecked);
        void onSpeedAlertValueChanged(String speed);
        void handleSpeed(ProgressiveGauge gauge, String speedString);
        void handleChargePercentage(int percent);
    }

    public AlertsMvpController(Activity activity) {

        view = new AlertsView(activity);
        presenter = new AlertsPresenter(view, App.INSTANCE.getSharedPreferences());
    }

    public void handleSpeed(ProgressiveGauge gauge,String speedString) {
        presenter.handleSpeed(gauge,speedString);
    }

    public void releaseMedia() {
        view.releaseMedia();
    }

    public void recaptureMedia(Activity activity) {
        view.recaptureMedia(activity);
    }


    public void handleChargePercentage(int percent) {
        presenter.handleChargePercentage(percent);
    }

}
