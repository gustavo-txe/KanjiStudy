package com.app.kanjistudy.data.ocr

import com.app.kanjistudy.core.time.MonotonicClock
import com.google.mlkit.vision.common.InputImage
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CameraKanjiScannerTest {
    private val recognizer = mockk<TextRecognizer>()
    private val image = mockk<InputImage>()
    private val clock = mockk<MonotonicClock>()

    @Test fun `first frame is accepted at clock zero and OCR keeps first occurrence order without duplicates`() = runTest {
        every { clock.nowMillis() } returns 0
        coEvery { recognizer.recognize(image) } returns "abc 月月ほん日! 月123日火火"
        assertEquals("月日火", CameraKanjiScanner(recognizer, clock).analyze(image, null))
    }

    @Test fun `changes in repetition count do not replace camera result`() = runTest {
        var now = 0L
        every { clock.nowMillis() } answers { now }
        coEvery { recognizer.recognize(image) } returnsMany listOf("日日月日", "日月月月")
        val useCase = CameraKanjiScanner(recognizer, clock)
        val firstResult = useCase.analyze(image, null)
        assertEquals("日月", firstResult)
        now = 2500
        assertNull(useCase.analyze(image, firstResult))
        coVerify(exactly = 2) { recognizer.recognize(image) }
    }

    @Test fun `throttled frames skip OCR until exact interval boundary`() = runTest {
        var now = 0L
        every { clock.nowMillis() } answers { now }
        coEvery { recognizer.recognize(image) } returns "日"
        val useCase = CameraKanjiScanner(recognizer, clock)
        assertEquals("日", useCase.analyze(image, null))
        now = 2499
        assertNull(useCase.analyze(image, null))
        coVerify(exactly = 1) { recognizer.recognize(image) }
        now = 2500
        assertEquals("日", useCase.analyze(image, null))
        coVerify(exactly = 2) { recognizer.recognize(image) }
    }

    @Test fun `empty or unchanged recognition does not replace camera result`() = runTest {
        every { clock.nowMillis() } returns 0
        for (text in listOf("hello ひらがな 123", "日")) {
            coEvery { recognizer.recognize(image) } returns text
            assertNull(CameraKanjiScanner(recognizer, clock).analyze(image, "日"))
        }
    }

    @Test fun `gallery filters non-kanji without throttling`() = runTest {
        coEvery { recognizer.recognize(image) } returnsMany listOf("日a月日", "abc123")
        val useCase = ImageKanjiScanner(recognizer)
        assertEquals("日月日", useCase(image))
        assertEquals("", useCase(image))
    }

    @Test fun `recognition cancellation propagates`() = runTest {
        every { clock.nowMillis() } returns 0
        val cancellation = CancellationException("cancelled")
        coEvery { recognizer.recognize(image) } throws cancellation
        try {
            CameraKanjiScanner(recognizer, clock).analyze(image, null)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
