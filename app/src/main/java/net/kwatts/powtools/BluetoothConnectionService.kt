package net.kwatts.powtools

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import net.kwatts.powtools.util.BluetoothUtil
import net.kwatts.powtools.util.BluetoothUtilMockImpl
import timber.log.Timber

class BluetoothConnectionService : IntentService(SERVICE_NAME) {

    var bluetoothUtil: BluetoothUtil? = BluetoothUtilMockImpl()

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action
        when (action) {
            ACTION_CONNECT_BT -> {
                bluetoothUtil!!.startScanning()
            }
            ACTION_DISCONNECT_BT -> {
                bluetoothUtil!!.stopScanning()
            }
            else -> Timber.e("Unknown service action: [$action]")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothConnectionService = this@BluetoothConnectionService
    }

    companion object {
        private const val SERVICE_NAME = "bt_connection_service"
        private const val ACTION_CONNECT_BT = "connect"
        private const val ACTION_DISCONNECT_BT = "disconnect"

        fun startBtConnection(context: Context) {
            context.startService(Intent(context, BluetoothConnectionService::class.java).apply {
                action = ACTION_CONNECT_BT
            })
        }

        fun stopBtConnection(context: Context) {
            context.startService(Intent(context, BluetoothConnectionService::class.java).apply {
                action = ACTION_DISCONNECT_BT
            })
        }
    }
}
