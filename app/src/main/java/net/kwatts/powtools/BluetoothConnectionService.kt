package net.kwatts.powtools

import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.databinding.Observable
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import net.kwatts.powtools.database.entities.Attribute
import net.kwatts.powtools.database.entities.Moment
import net.kwatts.powtools.database.entities.Ride
import net.kwatts.powtools.model.OWDevice
import net.kwatts.powtools.util.BluetoothUtil
import net.kwatts.powtools.util.BluetoothUtilImpl
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class BluetoothConnectionService : Service() {

    lateinit var mOWDevice: OWDevice
        private set
    var ride: Ride? = null
    lateinit var bluetoothUtil: BluetoothUtil
        private set
    private val mLoggingHandler = Handler()

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).i("onCreate")
        setupOWDevice()
        bluetoothUtil = BluetoothUtilImpl(this, mOWDevice)
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.tag(TAG).i("onBind")
        return LocalBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).i("onHandleIntent ($intent)")
        val action = intent?.action
        when (action) {
            ACTION_CONNECT_BT -> {
                bluetoothUtil.startScanning()
                if (App.INSTANCE.sharedPreferences.isLoggingEnabled) {
                    initLogging()
                }
            }
            ACTION_DISCONNECT_BT -> {
                bluetoothUtil.stopScanning()
                mLoggingHandler.removeCallbacksAndMessages(null)
            }
            else -> Timber.e("Unknown service action: [$action]")
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(TAG).i("onDestroy")
    }

    private fun setupOWDevice() {
        mOWDevice = OWDevice()

        mOWDevice.showDebugWindow.set(App.INSTANCE.sharedPreferences.isDebugging)
        mOWDevice.isOneWheelPlus.set(App.INSTANCE.sharedPreferences.isOneWheelPlus)

        mOWDevice.setupCharacteristics()
        mOWDevice.isConnected.set(false)

        mOWDevice.isConnected.addOnPropertyChangedCallback(
            object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(observable: Observable, i: Int) {
                    if (mOWDevice.isConnected.get() && isNewOrNotContinuousRide()) {
                        ride = Ride().also { newRide ->
                            App.dbExecute { database -> newRide.id = database.rideDao().insert(newRide) }
                        }
                    }
                }
            })
    }

    private fun isNewOrNotContinuousRide(): Boolean {
        val currentRide = ride
        if (currentRide == null) {
            return true
        } else if (currentRide.end == null) {
            Timber.e("isNewOrNotContinuousRide: unexpected state, ride.end not set")
            return true
        }

        val millisSinceLastMoment = Date().time - currentRide.end.time
        // Not continuous is defined as 1 min break. Maybe configurable in the future.
        return TimeUnit.MINUTES.toMillis(1) > millisSinceLastMoment
    }

    fun overrideBluetoothUtil(builder: (OWDevice) -> BluetoothUtil) {
        bluetoothUtil = builder(mOWDevice)
        //TODO disconnect previous connection
    }

    private fun initLogging() {
        if (ONEWHEEL_LOGGING) {
            val deviceFileLogger = object : Runnable {
                override fun run() {
                    val mLoggingFrequency = App.INSTANCE.sharedPreferences.loggingFrequency
                    mLoggingHandler.postDelayed(this, mLoggingFrequency.toLong())
                    if (mOWDevice.isConnected.get()) {
                        try {
                            persistMoment()
                        } catch (e: Exception) {
                            Timber.e("unable to write logs", e)
                        }

                    }
                }
            }
            mLoggingHandler.postDelayed(deviceFileLogger, App.INSTANCE.sharedPreferences.loggingFrequency.toLong())
        }
    }

    @Throws(Exception::class)
    private fun persistMoment() {
        App.dbExecute { database ->
            val latestMoment = Date()

            val ride = ride!!

            if (ride.start == null) {
                ride.start = latestMoment
            }
            ride.end = latestMoment
            database.rideDao().updateRide(ride)

            val moment = Moment(ride.id, latestMoment)
            moment.rideId = ride.id
            val momentId = database.momentDao().insert(moment)
            val attributes = ArrayList<Attribute>()
            for (deviceReadCharacteristic in mOWDevice.notifyCharacteristics) {
                val attribute = Attribute()
                attribute.setMomentId(momentId)
                attribute.value = deviceReadCharacteristic.value.get()
                attribute.key = deviceReadCharacteristic.key.get()

                attributes.add(attribute)
            }
            database.attributeDao().insertAll(attributes)
            if (mOWDevice.gpsLocation != null) {
                moment.setGpsLat(mOWDevice.gpsLocation.latitude)
                moment.setGpsLong(mOWDevice.gpsLocation.longitude)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothConnectionService = this@BluetoothConnectionService
    }

    companion object {
        private const val SERVICE_NAME = "bt_connection_service"
        private const val ACTION_CONNECT_BT = "connect"
        private const val ACTION_DISCONNECT_BT = "disconnect"
        private const val ONEWHEEL_LOGGING = true
        private const val TAG = "BluetoothConnectionService"

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
