package ru.bingosoft.teploInspector.util

import com.google.gson.JsonObject
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class JsonConverterTest {
    lateinit var jsonConverter: JsonConverter
    @Before
    fun setUp() {
        jsonConverter= JsonConverter()
    }

    @Test
    fun testFromString() {
        Assert.assertNull(jsonConverter.fromString(null))
        val fakeJsonString="{\"results_id\":70738}"
        Assert.assertTrue(jsonConverter.fromString(fakeJsonString) is JsonObject)
    }

    @Test
    fun testJsonToString() {
        Assert.assertNull(jsonConverter.jsonToString(null))
        val fakeJsonObject= JsonObject()
        fakeJsonObject.addProperty("property","value")
        Assert.assertTrue(jsonConverter.jsonToString(fakeJsonObject) is String)
    }
}