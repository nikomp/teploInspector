package ru.bingosoft.teploInspector.util

import android.content.Context
import android.os.Build
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ToasterTest {
    lateinit var context: Context
    lateinit var toaster: Toaster

    @Before
    fun setUp() {
        context =  ApplicationProvider.getApplicationContext<App>()
        toaster= Toaster(context)
    }
    @Test
    fun testGetCtx() {
        assertEquals(context,toaster.ctx)
    }

    @Test
    fun testShowToastWithResId() {
        toaster.showToast(R.string.for_testing)
        val latestToast=ShadowToast.getLatestToast()
        val view=latestToast.view
        val textToast=view.findViewById<TextView>(R.id.textToast)
        assertEquals(context.getString(R.string.for_testing),textToast.text)
    }

    @Test
    fun testShowToast() {
        toaster.showToast("fakeMessage")
        val latestToast=ShadowToast.getLatestToast()
        val view=latestToast.view
        val textToast=view.findViewById<TextView>(R.id.textToast)
        assertEquals("fakeMessage",textToast.text)
    }

    @Test
    fun testShowErrorToastWithResId() {
        toaster.showErrorToast(R.string.for_testing)
        val latestToast=ShadowToast.getLatestToast()
        val view=latestToast.view
        val textToast=view.findViewById<TextView>(R.id.textToast)
        assertEquals(context.getString(R.string.for_testing),textToast.text)
        assertEquals(ContextCompat.getColor(context, R.color.error),textToast.currentTextColor)

    }

    @Test
    fun testShowErrorToast() {
        toaster.showErrorToast("fakeErrorMessage")
        val latestToast=ShadowToast.getLatestToast()
        val view=latestToast.view
        val textToast=view.findViewById<TextView>(R.id.textToast)
        assertEquals("fakeErrorMessage",textToast.text)
        assertEquals(ContextCompat.getColor(context, R.color.error),textToast.currentTextColor)

    }


}