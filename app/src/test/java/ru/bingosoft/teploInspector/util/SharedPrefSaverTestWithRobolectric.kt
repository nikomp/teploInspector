package ru.bingosoft.teploInspector.util


import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.APP_PREFERENCES
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.DATESYNC
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ENTER_TYPE
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.FIREBASE_MESSAGE
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.IS_AUTH
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.LOGIN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.PASSWORD
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ROLE_ID
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.TOKEN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_FULLNAME
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_ID
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_PHOTO_URL
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.VERSION_NAME
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class SharedPrefSaverTestWithRobolectric {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var sharedPref: SharedPrefSaver

    @Before
    fun setUp() {
        val context =  ApplicationProvider.getApplicationContext<App>()
        sharedPreferences=context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        sharedPref=SharedPrefSaver(context)
    }

    @Test
    fun testGetCtx() {
        sharedPref.sptoken="fakeSptToken"
        assertEquals("fakeSptToken",sharedPref.sptoken)
    }

    @Test
    fun testSaveVersionName() {
        sharedPref.saveVersionName("fakeVersion")
        assertTrue("Нет атрибута $VERSION_NAME", sharedPreferences.contains(VERSION_NAME))
        assertEquals(
            "Значение атрибута $VERSION_NAME отличается от заданного",
            "fakeVersion",
            sharedPreferences.getString(
                VERSION_NAME,
                ""
            )
        )

    }

    @Test
    fun testSaveUser() {
        val user=Models.User(fullname = "fakeFullName", photoUrl = "fakePhotoUrl")
        sharedPref.saveUser(user)
        assertTrue("Нет атрибута $USER_FULLNAME", sharedPreferences.contains(USER_FULLNAME))
        assertTrue("Нет атрибута $USER_PHOTO_URL", sharedPreferences.contains(USER_PHOTO_URL))
        assertEquals(
            "Значение атрибута $USER_FULLNAME отличается от заданного",
            "fakeFullName",
            sharedPreferences.getString(
                USER_FULLNAME,
                ""
            )
        )
        assertEquals(
            "Значение атрибута $USER_PHOTO_URL отличается от заданного",
            "fakePhotoUrl",
            sharedPreferences.getString(
                USER_PHOTO_URL,
                ""
            )
        )

    }

    @Test
    fun testSavePassword() {
        sharedPref.savePassword("fakePassword")
        assertTrue("Нет атрибута $PASSWORD", sharedPreferences.contains(PASSWORD))
        assertEquals(
            "Значение атрибута $PASSWORD отличается от заданного",
            "fakePassword",
            sharedPreferences.getString(
                PASSWORD,
                ""
            )
        )

    }

    @Test
    fun testGetVersionName() {
        assertEquals(
            "Значение атрибута $VERSION_NAME отличается от пустоты",
            "",
            sharedPref.getVersionName()
        )
        sharedPref.saveVersionName("fakeVersion")
        assertEquals(
            "Значение атрибута $VERSION_NAME отличается от заданного",
            "fakeVersion",
            sharedPref.getVersionName()
        )
    }

    @Test
    fun testGetTokenGCM() {
        assertEquals(
            "Значение атрибута $FIREBASE_MESSAGE отличается от пустоты",
            "",
            sharedPref.getTokenGCM()
        )
        sharedPreferences.edit().putString(FIREBASE_MESSAGE, "fakeTokenGCM").apply()
        assertEquals(
            "Значение атрибута $FIREBASE_MESSAGE отличается от заданного",
            "fakeTokenGCM",
            sharedPref.getTokenGCM()
        )
    }

    @Test
    fun testGetLogin() {
        assertEquals("Значение атрибута $LOGIN отличается от пустоты", "", sharedPref.getLogin())
        sharedPref.saveLogin("fakeLogin")
        assertEquals(
            "Значение атрибута $LOGIN отличается от заданного",
            "fakeLogin",
            sharedPref.getLogin()
        )

    }

    @Test
    fun testGetUser() {
        val fakeUser=Models.User(fullname = "fakeFullName", photoUrl = "fakePhotoUrl")
        sharedPref.saveUser(fakeUser)
        val user=sharedPref.getUser()
        assertThat(user, instanceOf(Models.User::class.java))
        assertEquals(
            "Значение атрибута $USER_FULLNAME отличается от пустоты",
            fakeUser.fullname,
            user.fullname
        )
        assertEquals(
            "Значение атрибута $USER_PHOTO_URL отличается от пустоты",
            fakeUser.photoUrl,
            user.photoUrl
        )

    }

    @Test
    fun testGetPassword() {
        assertEquals(
            "Значение атрибута $PASSWORD отличается от пустоты",
            "",
            sharedPref.getPassword()
        )
        sharedPref.savePassword("fakePassword")
        assertEquals(
            "Значение атрибута $PASSWORD отличается от заданного",
            "fakePassword",
            sharedPref.getPassword()
        )

    }

    @Test
    fun testGetUserId() {
        assertEquals("Значение атрибута $USER_ID отличается от 0", 0, sharedPref.getUserId())
        sharedPreferences.edit().putInt(USER_ID, 1).apply()
        assertEquals(
            "Значение атрибута $USER_ID отличается от заданного",
            1,
            sharedPref.getUserId()
        )

    }

    @Test
    fun testGetToken() {
        assertEquals("Значение атрибута $TOKEN отличается от пустоты", "", sharedPref.getToken())
        val fakeToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJBY2Nlc3NFZGl0b3IiLCJuYmYiOjE2MTkwMDA5NDksImlhdCI6MTYxOTAwMDk0OSwiZXhwIjoxNjE5MDAxMDY5LCJzY29wZXMiOlsiVFlQRV9SRUZFUkVOQ0VfVE9LRU4iXSwidXNlciI6eyJpZCI6MjM1LCJyb2xlX2lkIjoxMTEsInJvb3RfbWVudV9pZCI6NDIwfSwianRpIjoieW5sdGhjcUkrZjB4UTRGR3VkYkorNWFMcHFzcjJ5MjhuVjVSeE5BU0tzTT0ifQ.dmTOctMWrJA0NFDiAsHy_OxFUhIZSJf9gpkSC61G0dbPIL-ydmAQbHKF7rPrUWInZBAzOSshUmaGqShwCT8POQSDNbnB4vf4xx935hzlB7atrmv4GAnC-rOItQ4d_5aVQ2yBIqqo4WcU7nLbo-BFeVC7gjhtbtkFDSD-xRAJ7M4"
        sharedPref.saveToken(fakeToken)
        assertEquals(
            "Значение атрибута $TOKEN отличается от заданного",
            fakeToken,
            sharedPref.getToken()
        )

    }

    @Test
    fun testGetEnterType() {
        assertEquals(
            "Значение атрибута $ENTER_TYPE отличается от пустоты",
            "",
            sharedPref.getEnterType()
        )
        sharedPref.saveEnterType()
        assertEquals(
            "Значение атрибута $ENTER_TYPE отличается от default",
            "default",
            sharedPref.getEnterType()
        )
        sharedPref.saveEnterType("fakeEnterType")
        assertEquals(
            "Значение атрибута $ENTER_TYPE отличается от заданного",
            "fakeEnterType",
            sharedPref.getEnterType()
        )
    }

    @Test
    fun testIsAuth() {
        sharedPref.saveAuthFlag()
        assertTrue(sharedPreferences.contains(IS_AUTH))
        assertTrue(sharedPref.isAuth())
    }

    @Test
    fun testSaveDateSyncDB() {
        val fakeDate=Date()
        var fakeDateFormat =""
        val dateFormat =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru", "RU"))
        try {
            fakeDateFormat = dateFormat.format(fakeDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        assertEquals(
            "Значение атрибута $DATESYNC отличается от пустоты",
            "",
            sharedPref.getDateSyncDB()
        )

        sharedPref.saveDateSyncDB(fakeDate)
        assertEquals(
            "Значение атрибута $DATESYNC отличается от заданного",
            fakeDateFormat,
            sharedPref.getDateSyncDB()
        )

        /*val fakeUnparseDate="32.04.2021"
        val format = SimpleDateFormat("ddMMyyyy")
        sharedPref.saveDateSyncDB(format.parse(fakeUnparseDate))*/

    }

    @Test
    fun testClearAuthData() {
        sharedPreferences.edit()
            .putBoolean(IS_AUTH, true)
            .putString(USER_FULLNAME, "fakeUserName")
            .putString(TOKEN, "fakeToken")
            .putString(USER_ID, "fakeUserId")
            .putString(ROLE_ID, "fakeRoleId")
            .putString(ENTER_TYPE, "fakeEnterType")
            .putString(USER_PHOTO_URL, "fakeUserPhotoUrl")
        sharedPref.clearAuthData()

        assertFalse("Атрибут $IS_AUTH не был удален", sharedPreferences.contains(IS_AUTH))
        assertFalse(
            "Атрибут $USER_FULLNAME не был удален",
            sharedPreferences.contains(USER_FULLNAME)
        )
        assertFalse("Атрибут $TOKEN не был удален", sharedPreferences.contains(TOKEN))
        assertFalse("Атрибут $USER_ID не был удален", sharedPreferences.contains(USER_ID))
        assertFalse("Атрибут $ROLE_ID не был удален", sharedPreferences.contains(ROLE_ID))
        assertFalse("Атрибут $ENTER_TYPE не был удален", sharedPreferences.contains(ENTER_TYPE))
        assertFalse(
            "Атрибут $USER_PHOTO_URL не был удален", sharedPreferences.contains(
                USER_PHOTO_URL
            )
        )

    }
}