package net.kwatts.powtools.util


class SpeedAlertResolver(
    private val sharedPreferences: SharedPreferencesUtil
) {

    fun isAlertThresholdExceeded(speedString: String): Boolean {
        val speed = java.lang.Float.parseFloat(speedString)
        val speedAlert = sharedPreferences.speedAlert
        val isEnabled = sharedPreferences.speedAlertEnabled

        return isEnabled && speed >= speedAlert
    }

}
