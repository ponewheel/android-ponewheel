package net.kwatts.powtools.util

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent

interface MainActivityC {
    fun invalidateOptionsMenu()
    fun updateBatteryRemaining(percent: Int)
    fun updateLog(msg: String)
    fun getSystemService(serviceName: String): BluetoothManager
    fun deviceConnectedTimer(timer: Boolean)
    fun getContext(): Context
    fun startActivityForResult(enableBtIntent: Intent, request: Int)
}
