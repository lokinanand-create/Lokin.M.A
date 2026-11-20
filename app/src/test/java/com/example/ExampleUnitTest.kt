package com.example

import com.example.domain.model.CreditCalculator
import com.example.domain.model.ImageStudioMode
import com.example.domain.model.SubscriptionPlan
import com.example.domain.model.VideoDuration
import com.example.domain.model.VideoFrameRate
import com.example.domain.model.VideoProvider
import com.example.domain.model.VideoResolution
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun creditCalculator_computesVideoAndImageCostsCorrectly() {
        val baseCost = CreditCalculator.videoCost(
            duration = VideoDuration.SEC_5,
            resolution = VideoResolution.HD_720P,
            fps = VideoFrameRate.FPS_24
        )
        assertEquals(10, baseCost)

        val uhd60Cost = CreditCalculator.videoCost(
            duration = VideoDuration.SEC_10,
            resolution = VideoResolution.UHD_4K,
            fps = VideoFrameRate.FPS_60
        )
        // (18 * 3.0) + 8 = 62
        assertEquals(62, uhd60Cost)

        assertEquals(4, CreditCalculator.imageCost(ImageStudioMode.TEXT_TO_IMAGE, export4K = false))
        assertEquals(7, CreditCalculator.imageCost(ImageStudioMode.TEXT_TO_IMAGE, export4K = true))
    }

    @Test
    fun videoProvider_detectsMedia3EnhancementRequirement() {
        assertFalse(
            VideoProvider.RUNWAY_GEN4.needsMedia3Enhancement(
                VideoResolution.FHD_1080P,
                VideoFrameRate.FPS_24
            )
        )
        assertTrue(
            VideoProvider.RUNWAY_GEN4.needsMedia3Enhancement(
                VideoResolution.UHD_4K,
                VideoFrameRate.FPS_24
            )
        )
        assertTrue(
            VideoProvider.KLING_AI.needsMedia3Enhancement(
                VideoResolution.FHD_1080P,
                VideoFrameRate.FPS_60
            )
        )
    }

    @Test
    fun multilingualStrings_provideNonBlankTranslationsForAllSixLanguages() {
        assertEquals(6, AppLanguage.entries.size)
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            assertEquals("MA AI Studio", s.appTitle)
            assertTrue(s.navHome.isNotBlank())
            assertTrue(s.generateVideo.isNotBlank())
            assertTrue(s.saveToDevice.isNotBlank())
        }
    }

    @Test
    fun subscriptionPlan_lookupFallsBackToFree() {
        assertEquals(SubscriptionPlan.PRO, SubscriptionPlan.fromId("pro"))
        assertEquals(SubscriptionPlan.FREE, SubscriptionPlan.fromId("unknown_plan"))
    }
}
