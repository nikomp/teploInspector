package ru.bingosoft.teploInspector.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.LOGIN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.PASSWORD


@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore("Долго работает")
class LoginActivityInstTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)
    lateinit var sharedPreferences:SharedPreferences
    lateinit var preferencesEditor:SharedPreferences.Editor

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferences=targetContext.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        preferencesEditor = sharedPreferences.edit()
        preferencesEditor.putString(LOGIN,"test_inzhener").commit()
        preferencesEditor.putString(PASSWORD, "TGu(c5F4{q").commit()
    }

    @Test
    fun testOnCreate() {
        Log.d("myLogs","testOnCreate")
        onView(withId(R.id.edUrl))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))
        onView(withId(R.id.edLogin))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))
        onView(withId(R.id.edPassword))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))

        val login=sharedPreferences.getString(LOGIN,"test_inzhener") //test_inzhener

        onView(withId(R.id.edLogin))
            .check(matches(isDisplayed()))
            .check(matches(withText(login)))

        val password=sharedPreferences.getString(PASSWORD,"TGu(c5F4{q") //TGu(c5F4{q

        onView(withId(R.id.edPassword))
            .check(matches(isDisplayed()))
            .check(matches(withText(password)))

        onView(withId(R.id.cbEnter))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun testBackPressed() {
        Espresso.pressBack()
        onView(withId(R.id.edUrl))
            .check(matches(isDisplayed()))
            .check(matches(not(withText(""))))
    }

}