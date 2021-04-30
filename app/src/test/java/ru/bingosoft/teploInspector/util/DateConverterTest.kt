package ru.bingosoft.teploInspector.util

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

class DateConverterTest {
    lateinit var dateConverter: DateConverter
    @Before
    fun setUp() {
        dateConverter= DateConverter()
    }

    @Test
    fun testFromTimestamp() {
        assertNull(dateConverter.fromTimestamp(null))
        val fakeDateLong=1619161886337
        assertTrue(dateConverter.fromTimestamp(fakeDateLong) is Date)
    }

    @Test
    fun testDateToTimestamp() {
        assertNull(dateConverter.dateToTimestamp(null))
        val fakeDate=Date()
        assertTrue(dateConverter.dateToTimestamp(fakeDate) is Long)
    }
}