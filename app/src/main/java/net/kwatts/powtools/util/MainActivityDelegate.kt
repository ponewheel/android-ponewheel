package net.kwatts.powtools.util

interface MainActivityDelegate {
    fun invalidateOptionsMenu()
    fun updateBatteryRemaining(percent: Int)
    fun deviceConnectedTimer(timer: Boolean)
}
