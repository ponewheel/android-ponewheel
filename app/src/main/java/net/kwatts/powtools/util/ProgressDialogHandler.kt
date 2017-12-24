package net.kwatts.powtools.util

import android.app.ProgressDialog
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context

class ProgressDialogHandler(private val activity: LifecycleOwner) : LifecycleObserver {

    private var progressDialog: ProgressDialog? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    fun show(): ProgressDialog? {
        dismiss()
        progressDialog = ProgressDialog.show(activity as Context, "","Please wait")
        return progressDialog
    }

    fun dismiss() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        dismiss()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        dismiss()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        dismiss()
    }
}