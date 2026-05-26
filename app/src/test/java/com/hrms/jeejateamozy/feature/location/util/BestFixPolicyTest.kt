package com.hrms.jeejateamozy.feature.location.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BestFixPolicyTest {

    @Test
    fun `takes first valid fix when none held`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = null, heldAgeMs = 0L, newAccuracyM = 30f))
    }

    @Test
    fun `rejects non-positive accuracy even when none held`() {
        assertFalse(BestFixPolicy.shouldReplace(null, 0L, 0f))
        assertFalse(BestFixPolicy.shouldReplace(null, 0L, -1f))
    }

    @Test
    fun `replaces held fix with a more accurate one`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 20f, heldAgeMs = 1_000L, newAccuracyM = 8f))
    }

    @Test
    fun `keeps fresh held fix when new one is less accurate`() {
        assertFalse(BestFixPolicy.shouldReplace(heldAccuracyM = 8f, heldAgeMs = 1_000L, newAccuracyM = 20f))
    }

    @Test
    fun `equal accuracy replaces to prefer newer`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 10f, heldAgeMs = 1_000L, newAccuracyM = 10f))
    }

    @Test
    fun `replaces a stale held fix even with a less accurate new one`() {
        assertTrue(BestFixPolicy.shouldReplace(heldAccuracyM = 5f, heldAgeMs = 10_001L, newAccuracyM = 40f))
    }

    @Test
    fun `does not replace held fix at exactly the stale boundary`() {
        assertFalse(BestFixPolicy.shouldReplace(heldAccuracyM = 5f, heldAgeMs = 10_000L, newAccuracyM = 40f))
    }
}
