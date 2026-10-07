package com.dzaky.anitrack.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test fun `profile accepts Indonesian names and normalizes handles`() {
        val input = ProfileInput("  Dzaky Putra  ", "  DZAKY_01  ", " Anime fan ")
        assertNull(input.validationError())
        assertEquals(ProfileInput("Dzaky Putra", "dzaky_01", "Anime fan"), input.normalized())
    }

    @Test fun `invalid handles and oversized bios are rejected`() {
        assertNotNull(ProfileInput("Dzaky", "ab").validationError())
        assertNotNull(ProfileInput("Dzaky", "with space").validationError())
        assertNotNull(ProfileInput("Dzaky", "dzaky", "x".repeat(121)).validationError())
    }

    @Test fun `sepuh requires both finished anime and recorded episodes`() {
        assertFalse(AnimeBadge.SepuhAnime.qualifies(ProfileStats(499, 25)))
        assertFalse(AnimeBadge.SepuhAnime.qualifies(ProfileStats(500, 24)))
        assertTrue(AnimeBadge.SepuhAnime.qualifies(ProfileStats(500, 25)))
        assertEquals(0.5f, AnimeBadge.SepuhAnime.progress(ProfileStats(250, 25)), 0f)
    }

    @Test fun `large episode totals do not overflow badge progress`() {
        assertEquals(1f, AnimeBadge.Maraton.progress(ProfileStats(Int.MAX_VALUE.toLong() * 100)), 0f)
        assertFalse(AnimeBadge.Kolektor.qualifies(ProfileStats(favorites = 4)))
        assertTrue(AnimeBadge.Kolektor.qualifies(ProfileStats(favorites = 5)))
    }
}
