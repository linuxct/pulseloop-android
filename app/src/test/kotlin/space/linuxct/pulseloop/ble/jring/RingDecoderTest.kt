package space.linuxct.pulseloop.ble.jring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import space.linuxct.pulseloop.ble.RingDecodedEvent
import space.linuxct.pulseloop.domain.model.MeasurementKind

/**
 * Verifies the Jring (56ff) combined-measurement decode (0x24), which fans one 20-byte packet out
 * into HR + blood pressure (systolic/diastolic) + SpO2 + fatigue + stress + blood sugar.
 *
 * Layout: [0]=0x24 [1]=HR [2]=systolic [3]=diastolic [4]=SpO2 [5]=fatigue [6]=stress
 *         [7]=bloodSugar(mmol/L×10). Blood sugar is stored as mg/dL = (raw/10) × 18.016.
 */
class RingDecoderTest {

    private val decoder = RingDecoder()

    private fun packet(vararg head: Int): ByteArray =
        ByteArray(20) { i -> if (i < head.size) head[i].toByte() else 0 }

    @Test
    fun `combined 0x24 fans out all metrics`() {
        // HR 72, sys 118, dia 78, SpO2 98, fatigue 30, stress 45, glucose 51 (=5.1 mmol/L)
        val events = decoder.decode(packet(0x24, 72, 118, 78, 98, 30, 45, 51))

        val hr = events.filterIsInstance<RingDecodedEvent.HeartRateSample>().firstOrNull()
        assertNotNull("expected a HeartRateSample", hr)
        assertEquals(72, hr!!.bpm)

        val history = events.filterIsInstance<RingDecodedEvent.HistoryMeasurement>()
        val sys = history.firstOrNull { it.kind == MeasurementKind.BLOOD_PRESSURE_SYSTOLIC }
        val dia = history.firstOrNull { it.kind == MeasurementKind.BLOOD_PRESSURE_DIASTOLIC }
        assertEquals(118.0, sys!!.value, 0.001)
        assertEquals(78.0, dia!!.value, 0.001)

        val spo2 = events.filterIsInstance<RingDecodedEvent.Spo2Result>().firstOrNull()
        assertEquals(98, spo2!!.value)

        val fatigue = history.firstOrNull { it.kind == MeasurementKind.FATIGUE }
        assertEquals(30.0, fatigue!!.value, 0.001)

        val stress = events.filterIsInstance<RingDecodedEvent.StressSample>().firstOrNull()
        assertEquals(45, stress!!.value)

        // Blood sugar: 51 → 5.1 mmol/L → 5.1 × 18.016 = 91.8816 mg/dL
        val glucose = history.firstOrNull { it.kind == MeasurementKind.BLOOD_SUGAR }
        assertEquals(91.8816, glucose!!.value, 0.001)
    }

    @Test
    fun `zero-valued fields are not emitted`() {
        // Only HR + SpO2 present; BP, fatigue, stress, glucose all zero.
        val events = decoder.decode(packet(0x24, 60, 0, 0, 96, 0, 0, 0))
        val history = events.filterIsInstance<RingDecodedEvent.HistoryMeasurement>()
        assertTrue("no BP/fatigue/glucose history when fields are 0", history.isEmpty())
        assertTrue(events.any { it is RingDecodedEvent.HeartRateSample })
        assertEquals(96, events.filterIsInstance<RingDecodedEvent.Spo2Result>().first().value)
    }

    @Test
    fun `invalid spo2 falls back to progress`() {
        // SpO2 byte (4) below the 80..100 window → Spo2Progress(null), preserving prior behaviour.
        val events = decoder.decode(packet(0x24, 60, 0, 0, 50, 0, 0, 0))
        val progress = events.filterIsInstance<RingDecodedEvent.Spo2Progress>().firstOrNull()
        assertNotNull(progress)
        assertNull(progress!!.percent)
        assertTrue(events.none { it is RingDecodedEvent.Spo2Result })
    }

    @Test
    fun `wrong-length packet decodes to a single Unknown`() {
        val events = decoder.decode(byteArrayOf(0x24, 0x01))
        assertEquals(1, events.size)
        assertTrue(events.first() is RingDecodedEvent.Unknown)
    }
}
