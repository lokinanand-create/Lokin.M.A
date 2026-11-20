package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `app name matches MA AI Studio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MA AI Studio", appName)
    }

    @Test
    fun `main activity launches and initializes Compose UI`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertFalse(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test
    fun `provider statuses report firebase disabled and secret names`() {
        val statuses = ProviderConfig.allStatuses()
        assertEquals(4, statuses.size)
        assertFalse(ProviderConfig.isFirebaseAiLogicEnabled)
        assertTrue(statuses.any { it.id == "runway" })
        assertTrue(statuses.any { it.id == "kling" })
        assertTrue(statuses.any { it.id == "openai" })
    }
}
