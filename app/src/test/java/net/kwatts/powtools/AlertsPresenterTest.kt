package net.kwatts.powtools


import net.kwatts.powtools.util.SharedPreferences
import net.kwatts.powtools.view.AlertsMvpController
import net.kwatts.powtools.view.AlertsPresenter
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AlertsPresenterTest {

    val sharedPreferences: SharedPreferences = mock(SharedPreferences::class.java)
    val view: AlertsMvpController.View = mock(AlertsMvpController.View::class.java)

    private val boolCapture = ArgumentCaptor.forClass(Boolean::class.java)

    @Test
    fun testValidation() {
        val alertsPresenter = AlertsPresenter(view, sharedPreferences)

        var errorShowTimes = 0

        errorShowTimes++// show error
        alertsPresenter.onSpeedAlertValueChanged("a")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        errorShowTimes++// show error
        alertsPresenter.onChargeValueChanged("a")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        // don't show error
        alertsPresenter.handleSpeed("a")
        verify(view, times(errorShowTimes)).showNumberFormatError()


        // don't show error
        alertsPresenter.handleSpeed("a")
        verify(view, times(errorShowTimes)).showNumberFormatError()


        // don't show error
        alertsPresenter.onSpeedAlertValueChanged("1.1")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        errorShowTimes++// show error
        alertsPresenter.onChargeValueChanged("1.1")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        // don't show error
        alertsPresenter.handleSpeed("1.1")
        verify(view, times(errorShowTimes)).showNumberFormatError()


        // don't show error
        alertsPresenter.onSpeedAlertValueChanged("1")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        // don't show error
        alertsPresenter.onChargeValueChanged("1")
        verify(view, times(errorShowTimes)).showNumberFormatError()

        // don't show error
        alertsPresenter.handleSpeed("1")
        verify(view, times(errorShowTimes)).showNumberFormatError()


        // TODO negative numbers and 0
        println("errorShowTimes = " + errorShowTimes)
    }

    @Test
    fun testSetupClassInvisibleStart() {
        `when`(sharedPreferences.chargeAlertEnabled).thenReturn(false)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(false)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)

        verify(view, times(1)).setSpeedEnabled(false)
        verify(view, times(1)).setChargeEnabled(false)
    }

    @Test
    fun testSetupClassVisibleStart() {
        `when`(sharedPreferences.chargeAlertEnabled).thenReturn(true)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(true)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)

        verify(view, times(1)).setSpeedEnabled(true)
        verify(view, times(1)).setChargeEnabled(true)
    }

    @Test
    fun testSoundAlarm() {

        `when`(sharedPreferences.speedAlertEnabled).thenReturn(true)
        `when`(sharedPreferences.speedAlert).thenReturn(17f)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)


        alertsPresenter.handleSpeed("15")
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleSpeed("17")
        verify(view, times(1)).playSound(boolCapture.capture())
        Assert.assertEquals(java.lang.Boolean.TRUE, boolCapture.value)

        alertsPresenter.handleSpeed("18")
        verify(view, times(1)).playSound(anyBoolean())

        alertsPresenter.handleSpeed("15")
        verify(view, times(2)).playSound(boolCapture.capture())
        Assert.assertEquals(java.lang.Boolean.FALSE, boolCapture.value)
    }

    @Test
    fun testSoundAlarmDisabled() {

        `when`(sharedPreferences.speedAlertEnabled).thenReturn(false)
        `when`(sharedPreferences.speedAlert).thenReturn(17f)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)


        alertsPresenter.handleSpeed("15")
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleSpeed("17")
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleSpeed("18")
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleSpeed("15")
        verify(view, times(0)).playSound(anyBoolean())
    }


    @Test
    fun testChargeSoundAlarm() {

        `when`(sharedPreferences.chargeAlertEnabled).thenReturn(true)
        `when`(sharedPreferences.chargeAlert).thenReturn(17)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)


        alertsPresenter.handleChargePercentage(15)
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleChargePercentage(17)
        verify(view, times(1)).playSound(boolCapture.capture())
        Assert.assertEquals(java.lang.Boolean.TRUE, boolCapture.value)

        alertsPresenter.handleChargePercentage(18)
        verify(view, times(1)).playSound(anyBoolean())

        alertsPresenter.handleChargePercentage(15)
        verify(view, times(2)).playSound(boolCapture.capture())
        Assert.assertEquals(java.lang.Boolean.FALSE, boolCapture.value)
    }

    @Test
    fun testChargeSoundAlarmDisabled() {

        `when`(sharedPreferences.chargeAlertEnabled).thenReturn(false)
        `when`(sharedPreferences.chargeAlert).thenReturn(17)

        val alertsPresenter = AlertsPresenter(view, sharedPreferences)


        alertsPresenter.handleChargePercentage(15)
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleChargePercentage(17)
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleChargePercentage(18)
        verify(view, times(0)).playSound(anyBoolean())

        alertsPresenter.handleChargePercentage(15)
        verify(view, times(0)).playSound(anyBoolean())
    }

    @Test
    fun testPlayWithVisibility() {
        `when`(sharedPreferences.chargeAlertEnabled).thenReturn(true)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(true)

        AlertsPresenter(view, sharedPreferences).apply {
            onChargeAlertCheckChanged(false)
            onChargeAlertCheckChanged(true)
            onChargeAlertCheckChanged(false)

            onSpeedAlertCheckChanged(false)
            onSpeedAlertCheckChanged(true)
            onSpeedAlertCheckChanged(false)
        }
        verify(view, times(4)).setChargeEnabled(boolCapture.capture())
        Assert.assertEquals(listOf(true, false, true, false), boolCapture.allValues)
    }


}
