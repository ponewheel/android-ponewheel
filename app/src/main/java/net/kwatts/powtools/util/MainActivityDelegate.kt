package net.kwatts.powtools.util

interface MainActivityDelegate {
    fun updateBatteryRemaining(percent: Int)
    fun deviceConnectedTimer(timer: Boolean)
}
