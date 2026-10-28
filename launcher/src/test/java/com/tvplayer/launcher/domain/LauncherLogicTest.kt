package com.tvplayer.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLogicTest {

    private fun reading(water: Double?, target: Double?, mode: String? = "heat", bubbles: Boolean = false) =
        SpaReading(water, target, mode, bubbles)

    @Test
    fun `water state`() {
        assertEquals(WaterState.AT_TEMPERATURE, reading(104.0, 104.0).waterState)
        assertEquals(WaterState.AT_TEMPERATURE, reading(103.2, 104.0).waterState)
        assertEquals(WaterState.HEATING, reading(98.0, 104.0).waterState)
        assertEquals(WaterState.COOLING, reading(106.0, 104.0).waterState)
        assertEquals(WaterState.OFF, reading(90.0, 104.0, mode = "off").waterState)
        assertEquals(WaterState.UNKNOWN, reading(null, 104.0).waterState)
    }

    @Test
    fun `tile summary`() {
        assertEquals("104° · Ready", reading(104.2, 104.0).summary)
        assertEquals("98° · Heating", reading(98.0, 104.0).summary)
        assertEquals("104° · Bubbles on", reading(104.0, 104.0, bubbles = true).summary)
        assertEquals("--° · Offline", reading(null, null).summary)
    }

    @Test
    fun `weather mapping`() {
        assertEquals(WeatherKind.CLEAR_DAY, weatherKind("sunny"))
        assertEquals(WeatherKind.CLEAR_NIGHT, weatherKind("clear-night"))
        assertEquals(WeatherKind.RAIN, weatherKind("pouring"))
        assertEquals(WeatherKind.STORM, weatherKind("lightning-rainy"))
        assertEquals(WeatherKind.UNKNOWN, weatherKind(null))
    }

    @Test
    fun `base url normalization`() {
        assertEquals("http://homeassistant.local:8123", normalizeBaseUrl(" homeassistant.local:8123/ "))
        assertEquals("https://ha.example.com", normalizeBaseUrl("https://ha.example.com/"))
        assertEquals("HTTP://10.0.0.5:8123", normalizeBaseUrl("HTTP://10.0.0.5:8123"))
        assertEquals("", normalizeBaseUrl("  "))
    }
}
