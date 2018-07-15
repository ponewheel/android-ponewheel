package net.kwatts.powtools.view;

import android.app.Activity;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import net.kwatts.powtools.R;

import java.util.Locale;

public class AlertsView implements AlertsMvpController.View{

    private final CheckBox alertChargeCheckView;
    private final EditText alertChargeEntryView;
    private final TextView alertChargeTextView;

    private final CheckBox alertSpeedCheckView;
    private final EditText alertSpeedEntryView;
    private final TextView alertSpeedTextView;
    private MediaPlayer mediaPlayer;
    AlertsMvpController.Presenter alertsPresenter;
    private Snackbar numberFormatErrorSnackbar;

    public AlertsView(Activity activity) {
        alertChargeCheckView = activity.findViewById(R.id.alerts_charge_alert_check);
        alertChargeEntryView = activity.findViewById(R.id.alerts_charge_alert_entry);
        alertChargeTextView = activity.findViewById(R.id.alerts_charge_alert_text);
        alertSpeedCheckView = activity.findViewById(R.id.alerts_speed_alert_check);
        alertSpeedEntryView = activity.findViewById(R.id.alerts_speed_alert_entry);
        alertSpeedTextView = activity.findViewById(R.id.alerts_speed_alert_text);
        numberFormatErrorSnackbar = Snackbar.make(alertSpeedTextView, "Please enter a numeric value", Snackbar.LENGTH_SHORT);

        alertChargeCheckView.setOnCheckedChangeListener((view, isChecked) -> {
            alertsPresenter.onChargeAlertCheckChanged(isChecked);
        });
        alertChargeEntryView.addTextChangedListener(
                new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        alertsPresenter.onChargeValueChanged(s.toString());
                    }
                }
        );

        alertSpeedCheckView.setOnCheckedChangeListener((view, isChecked) -> {
            alertsPresenter.onSpeedAlertCheckChanged(isChecked);
        });
        alertSpeedEntryView.addTextChangedListener(
                new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        alertsPresenter.onSpeedAlertValueChanged(s.toString());
                    }
                }
        );

        mediaPlayer = MediaPlayer.create(activity, R.raw.siren);
    }

    public void setSpeedAlert(float speedAlert) {
        alertSpeedEntryView.setText(String.format(Locale.ENGLISH,"%3.1f",speedAlert));
    }

    @Override
    public void setChargeAlert(int chargeAlert) {
        alertChargeEntryView.setText(String.format(Locale.ENGLISH, "%d", chargeAlert));
    }

    @Override
    public void releaseMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void playSound(boolean shouldPlay) {
        if (mediaPlayer == null) {
            return;
        }

        if (shouldPlay) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        } else {
            mediaPlayer.pause();
        }
    }

    @Override
    public void recaptureMedia(Activity activity) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(activity, R.raw.siren);
        }
    }

    @Override
    public void setPresenter(AlertsMvpController.Presenter alertsPresenter) {
        this.alertsPresenter = alertsPresenter;
    }

    @Override
    public void setChargeEnabled(boolean isEnabled) {
        alertChargeEntryView.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        alertChargeCheckView.setChecked(isEnabled);

    }

    @Override
    public void setSpeedEnabled(boolean isEnabled) {
        alertSpeedEntryView.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        alertSpeedCheckView.setChecked(isEnabled);
    }

    @Override
    public void showNumberFormatError() {
        numberFormatErrorSnackbar.show();
    }
}
