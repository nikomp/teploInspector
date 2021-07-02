package ru.bingosoft.teploInspector.ui.order

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.IS_AUTH
import ru.bingosoft.teploInspector.util.Toaster


@RunWith(AndroidJUnit4::class)
@LargeTest
@Ignore("Ошибка в тесте + зависает при формировании Jacoco отчета")
class OrderFragmentInstTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get: Rule
    val permissionsRule = GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /*@Rule
    var fragmentTestRule: FragmentTestRule<MainActivity, OrderFragment> =
        FragmentTestRule.create(MainActivity::class.java,  )*/

    lateinit var sharedPreferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor
    lateinit var mockDb: AppDatabase
    lateinit var mockApiService: ApiService
    lateinit var orderPresenter: OrderPresenter

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferences=targetContext.getSharedPreferences(
            Const.SharedPrefConst.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
        preferencesEditor = sharedPreferences.edit()

        mockDb= Mockito.mock(AppDatabase::class.java)
        mockApiService= Mockito.mock(ApiService::class.java)

        val toaster= Toaster(targetContext)
        orderPresenter=OrderPresenter(mockDb, mockApiService, toaster)

        println("auth_${sharedPreferences.getBoolean(IS_AUTH, false)}")
        Log.d("myLogs", "auth_${sharedPreferences.getBoolean(IS_AUTH, false)}")
        if (!sharedPreferences.getBoolean(IS_AUTH, false)) {
            onView(withId(R.id.btnGo)).perform(click())
        }



    }

    @Test
    fun testOnCreateView() {
        onView(withId(R.id.stMsgAlert)).check(matches(isDisplayed()))
    }

    @Test
    fun testOnCreateViewWithFilteredOrders() {
        activityRule.scenario.onActivity { activity ->
            activity.filteredOrders= listOf(Orders())

            val adapter=activity.findViewById<RecyclerView>(R.id.orders_recycler_view).adapter as OrderListAdapter
            assertNotNull(adapter)
        }

    }
}