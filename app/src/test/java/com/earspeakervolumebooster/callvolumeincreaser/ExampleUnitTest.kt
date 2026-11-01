package com.earspeakervolumebooster.callvolumeincreaser

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun notificationPreferenceIsClampedToSupportedValues() {
        assertEquals(0, Options.parseNotify("-1", false))
        assertEquals(2, Options.parseNotify("8", false))
    }

    @Test
    fun invalidNotificationPreferenceUsesDeviceDefault() {
        assertEquals(1, Options.parseNotify("invalid", false))
        assertEquals(2, Options.parseNotify("invalid", true))
    }
}