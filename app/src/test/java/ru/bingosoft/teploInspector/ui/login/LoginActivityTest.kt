package ru.bingosoft.teploInspector.ui.login

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.APP_PREFERENCES
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ENTER_TYPE
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class LoginActivityTest {
    lateinit var loginActivity: LoginActivity
    lateinit var mockSharedPrefSaver: SharedPrefSaver

    @Before
    fun setUp() {
        loginActivity= LoginActivity()
        val mockToaster= Mockito.mock(Toaster::class.java)
        mockSharedPrefSaver= Mockito.mock(SharedPrefSaver::class.java)
        loginActivity.toaster=mockToaster
        loginActivity.sharedPref=mockSharedPrefSaver
    }

    @Test
    fun testGetToaster() {
        Assert.assertNotNull(loginActivity.toaster)
    }

    @Test
    fun testGetSharedPref() {
        Assert.assertNotNull(loginActivity.sharedPref)
    }

    @Test
    fun testOnClick() {
        println("testOnClick")
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity: LoginActivity ->
                val btnGo=activity.findViewById<Button>(R.id.btnGo)
                //#spy #Подмена_объекта_spy
                //подробнее тут https://stackoverflow.com/questions/47081174/espresso-mockito-verify-that-method-was-called/48275563
                val stySP=spy(activity.sharedPref)
                activity.runOnUiThread{
                    activity.sharedPref=stySP
                }
                btnGo.performClick()

                verify(stySP).saveEnterType(anyString())

                val enterType=activity.sharedPref.getEnterType()
                val sp=activity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                assertTrue(sp.contains(ENTER_TYPE))
                assertEquals(enterType, sp.getString(ENTER_TYPE,""))
                assertEquals(Activity.RESULT_OK, scenario.result.resultCode)

            }
        }
    }

    @Test
    fun testOnClick2() {
        println("testOnClick")
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity: LoginActivity ->
                val btnGo=activity.findViewById<Button>(R.id.btnGo)
                val edLogin=activity.findViewById<TextView>(R.id.edLogin)
                edLogin.text=""
                val styToaster=spy(activity.toaster)
                activity.runOnUiThread{
                    activity.toaster=styToaster
                }
                btnGo.performClick()

                verify(styToaster).showErrorToast(activity.getString(R.string.fill_all_fields))

            }
        }
    }

 }