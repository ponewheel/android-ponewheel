package net.kwatts.powtools.util

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*

class SpeedAlertResolverTest {

    val sharedPreferences: SharedPreferencesUtil = mock(SharedPreferencesUtil::class.java)
    val resolver = SpeedAlertResolver(sharedPreferences)

    @Test
    fun alertEnabledAndSpeedExceedsLimit_returnsTrue() {
        `when`(sharedPreferences.speedAlert).thenReturn(10f)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(true)

        val result = resolver.isAlertThresholdExceeded("11")

        assertTrue("Speed limit alert is required", result)
    }

    @Test
    fun alertEnabledAndSpeedLowSpeed_returnsFalse() {
        `when`(sharedPreferences.speedAlert).thenReturn(10f)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(true)

        val result = resolver.isAlertThresholdExceeded("5")

        assertFalse("Speed limit alert is not required", result)
    }

    @Test
    fun alertDisabledAndSpeedExceedsLimit_returnsFalse() {
        `when`(sharedPreferences.speedAlert).thenReturn(10f)
        `when`(sharedPreferences.speedAlertEnabled).thenReturn(false)

        val result = resolver.isAlertThresholdExceeded("12")

        assertFalse("Speed limit alert is not required", result)
    }
}
