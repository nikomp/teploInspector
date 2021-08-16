package ru.bingosoft.teploInspector.ui.login

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.anyOrNull
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.AddLoad.AddLoadDao
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Checkup.CheckupDao
import ru.bingosoft.teploInspector.db.HistoryOrderState.HistoryOrderStateDao
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.Orders.OrdersDao
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.db.TechParams.TechParamsDao
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.db.User.TrackingUserLocationDao
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.FinishTime.FINISH_CHECK_INTERVAL
import ru.bingosoft.teploInspector.util.Const.LocationStatus.INTERVAL_SENDING_ROUTE
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.TimeUnit


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class LoginPresenterTest {
    private lateinit var loginPresenter: LoginPresenter
    private lateinit var mockDb: AppDatabase
    private lateinit var mockView: LoginContractView
    private lateinit var mockApiService: ApiService
    private lateinit var mockOrdersDao: OrdersDao
    private lateinit var mockHistoryOrderStateDao: HistoryOrderStateDao
    private lateinit var mockTechParamsDao: TechParamsDao
    private lateinit var mockAddLoadDao: AddLoadDao
    private lateinit var mockCheckupDao: CheckupDao
    private lateinit var mockTrackingUserDao: TrackingUserLocationDao
    private lateinit var mockOtherUtil: OtherUtil
    private lateinit var mockSharedPrefSaver: SharedPrefSaver

    private lateinit var valueCaptor: KArgumentCaptor<String>

    private lateinit var testScheduler: TestScheduler

    @Before
    fun setUp() {
        testScheduler=TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}
        /*testScheduler=TestScheduler()
        RxAndroidPlugins.setMainThreadSchedulerHandler { testScheduler }*/

        mockDb= mock(AppDatabase::class.java)
        mockOrdersDao=mock(OrdersDao::class.java)
        mockHistoryOrderStateDao=mock(HistoryOrderStateDao::class.java)
        mockTechParamsDao=mock(TechParamsDao::class.java)
        mockAddLoadDao=mock(AddLoadDao::class.java)
        mockCheckupDao=mock(CheckupDao::class.java)
        mockTrackingUserDao=mock(TrackingUserLocationDao::class.java)
        mockApiService= mock(ApiService::class.java)
        mockSharedPrefSaver=mock(SharedPrefSaver::class.java)

        mockOtherUtil=mock(OtherUtil::class.java)
        //val context =  ApplicationProvider.getApplicationContext<App>()
        //val toaster= Toaster(context)
        val mockToaster=mock(Toaster::class.java)
        //val sharedPrefSaver=SharedPrefSaver(context)

        loginPresenter= LoginPresenter(mockApiService, mockDb, mockSharedPrefSaver, mockToaster)
        mockView= mock(LoginContractView::class.java)
        loginPresenter.view=mockView
        loginPresenter.otherUtil=mockOtherUtil

        valueCaptor= argumentCaptor()
    }

    @Test
    fun testGetToaster() {
        Assert.assertNotNull(loginPresenter.toaster)
    }

    @Test
    fun testAttachView() {
        loginPresenter.attachView(mockView)
        Assert.assertNotNull(loginPresenter.view)
    }

    @Test
    fun testAuthorizationWithEmptyData() {
        loginPresenter.authorization("", "", "")
        sleep(1000)
        verify(mockOtherUtil).writeToFile("Logger_authorization_from_LoginPresenter")
        verify(mockView).errorReceived(anyOrNull())
        verify(mockOtherUtil).writeToFile("Logger_Не заданы логин или пароль")
    }

    @Test
    fun testAuthorizationWithNullData() {
        loginPresenter.authorization("", null, null)
        sleep(1000)
        verify(mockOtherUtil).writeToFile("Logger_authorization_from_LoginPresenter")
        verify(mockView).errorReceived(anyOrNull())
        verify(mockOtherUtil).writeToFile("Logger_Не заданы логин или пароль")
    }

    @Test
    fun testAuthorization() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)

        verify(mockOtherUtil).writeToFile("Logger_authorization_from_LoginPresenter")
        verify(mockApiService).getAuthentication(anyString(), anyOrNull())
        verify(mockView).saveLoginPasswordToSharedPreference("fakeLogin", "fakePassword")
        verify(mockView).getAllMessage()

    }

    @Test
    fun testSaveTokenFCM() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
    }

    @Test
    fun testSaveTokenFCMWithError() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val t=Throwable("error_in_saveGCMToken")
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.error(t))
        `when`(mockSharedPrefSaver.getTokenGCM()).thenReturn("fakeToken")

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(1000)
        verify(mockView).errorReceived(t)
    }


    @Test
    fun testSendRouteWithEmptyData() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)
        val fakeListTrackingUserLocationEmpty = listOf<TrackingUserLocation>()
        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenReturn(
            fakeListTrackingUserLocationEmpty
        )

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(mockOtherUtil, atLeast(2)).writeToFile(valueCaptor.capture())
        val values= valueCaptor.allValues
        assertTrue(values.any { it.contains("Logger_Нет данных о маршруте") })
    }

    @Test
    fun testSendRoute() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)
        val fakeListTrackingUserLocation = listOf(
            TrackingUserLocation(lat=1.0,lon=1.0),
            TrackingUserLocation(lat=2.0,lon=2.0))

        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenReturn(
            fakeListTrackingUserLocation
        )

        val mockUnit2=mock(Unit::class.java)
        `when`(mockApiService.sendTrackingUserLocation(anyOrNull())).thenReturn(Single.just(mockUnit2))

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(mockApiService).sendTrackingUserLocation(anyOrNull())

    }

    @Test
    fun testSetAutoFinish() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)
        val fakeListTrackingUserLocation = listOf(
            TrackingUserLocation(lat=1.0,lon=1.0),
            TrackingUserLocation(lat=2.0,lon=2.0))

        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenReturn(
            fakeListTrackingUserLocation
        )

        val mockUnit2=mock(Unit::class.java)
        `when`(mockApiService.sendTrackingUserLocation(anyOrNull())).thenReturn(Single.just(mockUnit2))

        `when`(mockSharedPrefSaver.getTokenGCM()).thenReturn("fakeToken")

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(FINISH_CHECK_INTERVAL,TimeUnit.MINUTES)
        verify(mockOtherUtil).writeToFile("Logger_setAutoFinish_trigger_${Date()}")

    }


    @Test
    fun testSendRouteWithException() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)
        val fakeListTrackingUserLocation = listOf(
            TrackingUserLocation(lat=1.0,lon=1.0),
            TrackingUserLocation(lat=2.0,lon=2.0))

        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenReturn(
            fakeListTrackingUserLocation
        )

        val t=Throwable("error_in_sendTrackingUserLocation")
        `when`(mockApiService.sendTrackingUserLocation(anyOrNull())).thenReturn(Single.error(t))
        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(mockView).errorReceived(t)
    }

    @Test
    fun testSendRouteWithExceptionInGetTrackingForCurrentDay() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)


        val exception=RuntimeException("error_in_getTrackingForCurrentDay")
        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenThrow(exception)

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(mockView).errorReceived(exception)
    }

    @Test
    fun testSendRouteWithExceptionAndViewNull() {
        loginPresenter.view=null


        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)
        `when`(mockSharedPrefSaver.getTokenGCM()).thenReturn("fakeToken")


        val exception=RuntimeException("error_in_getTrackingForCurrentDay")
        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenThrow(exception)

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(loginPresenter.toaster).showErrorToast(anyString(), anyInt())

    }

    @Test
    fun testSendRouteWithHttpException500() {
        loginPresenter.view=null


        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)


        val fakeError=Gson().toJson(Models.Error(error = "user_not_found")).toResponseBody("application/json".toMediaTypeOrNull())
        val fakeHTTPExceptionResponse=Response.error<HttpException>(500, fakeError)
        val fakeHTTPException=HttpException(fakeHTTPExceptionResponse)
        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenThrow(fakeHTTPException)

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        verify(loginPresenter.toaster).showErrorToast(anyString(), anyInt())

    }


    @Test
    fun testSendRouteWithHttpException401() {
        loginPresenter.view=null


        val fakeUuid=Models.Uuid(uuid = "fakeUuid")
        val fakeToken=Models.Token(token = "fakeToken", name = "fakeName")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(Single.just(fakeToken))
        val mockUnit=mock(Unit::class.java)
        `when`(mockApiService.saveGCMToken(anyOrNull())).thenReturn(Single.just(mockUnit))
        `when`(mockDb.trackingUserDao()).thenReturn(mockTrackingUserDao)

        val fakeError2=Gson().toJson(Models.Error(error = "user_not_found")).toResponseBody("application/json".toMediaTypeOrNull())
        val fakeHTTPExceptionResponse2=Response.error<HttpException>(401, fakeError2)
        val fakeHTTPException2=HttpException(fakeHTTPExceptionResponse2)
        `when`(mockTrackingUserDao.getTrackingForLastMinutes()).thenThrow(fakeHTTPException2)
        `when`(mockSharedPrefSaver.getLogin()).thenReturn("fakeLogin")
        `when`(mockSharedPrefSaver.getPassword()).thenReturn("fakePassword")
        `when`(mockSharedPrefSaver.getTokenGCM()).thenReturn("fakeToken")

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(2000)
        testScheduler.advanceTimeTo(INTERVAL_SENDING_ROUTE,TimeUnit.MINUTES)
        //verify(mockOtherUtil).writeToFile("Logger_authorization_from_LoginPresenter")
        verify(mockOtherUtil, atLeast(2)).writeToFile(valueCaptor.capture())
        val values= valueCaptor.allValues
        assertTrue(values.filter { it.contains("Logger_authorization_from_LoginPresenter") }.size>=2)
    }



    @Test
    fun testAuthorizationWithException() {
        val fakeUuid=Models.Uuid(uuid = "fakeUuid")

        `when`(mockApiService.getAuthentication(anyString(), anyOrNull())).thenReturn(
            Single.just(
                fakeUuid
            )
        )
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(
            Single.error(
                RuntimeException(
                    "Тестовая ошибка"
                )
            )
        )

        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(1000)
        verify(mockView).showAlertNotInternet()

        val fakeError=Gson().toJson(Models.Error(error = "user_not_found")).toResponseBody("application/json".toMediaTypeOrNull())
        val fakeHTTPExceptionResponse=Response.error<HttpException>(500, fakeError)
        val fakeHTTPException=HttpException(fakeHTTPExceptionResponse)
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(
            Single.error(
                fakeHTTPException
            )
        )
        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(1000)
        verify(mockView).showFailureTextView("Неверный логин")

        val fakeError2=Gson().toJson(Models.Error(error = "user_password_is_invalid")).toResponseBody(
            "application/json".toMediaTypeOrNull()
        )
        val fakeHTTPExceptionResponse2=Response.error<HttpException>(500, fakeError2)
        val fakeHTTPException2=HttpException(fakeHTTPExceptionResponse2)
        `when`(mockApiService.getAuthorization(anyOrNull())).thenReturn(
            Single.error(
                fakeHTTPException2
            )
        )
        loginPresenter.authorization("fakeUrl", "fakeLogin", "fakePassword")
        sleep(1000)
        verify(mockView).showFailureTextView("Неверный пароль")

    }

    @Test
    fun testSyncDB() {
        val fakeListOrders= listOf(
            Orders(id = 1, number = "A-001", status = "Открыта"),
            Orders(id = 2, number = "A-002", status = "Открыта")
        )
        val fakeTechParams= listOf(
            TechParams(id = 1, technical_characteristic = "Параметр1", idOrder = 1),
            TechParams(id = 2, technical_characteristic = "Параметр2", idOrder = 1)
        )
        val fakeAddLoad= listOf(
            AddLoad(id = 1, purpose = "Назначение1"),
            AddLoad(id = 1, purpose = "Назначение2")
        )
        val fakeCheckup= listOf(
            Checkup(idOrder = 1, numberOrder = "A-001", orderGuid = "", text = JsonArray()),
            Checkup(idOrder = 2, numberOrder = "A-002", orderGuid = "", text = JsonArray())
        )
        `when`(mockApiService.getCheckups()).thenReturn(Single.just(fakeCheckup))
        `when`(mockApiService.getAddLoad(anyInt())).thenReturn(Single.just(fakeAddLoad))
        `when`(mockApiService.getTechParams(anyInt())).thenReturn(Single.just(fakeTechParams))
        `when`(mockApiService.getListOrder()).thenReturn(Single.just(fakeListOrders))
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        `when`(mockDb.historyOrderStateDao()).thenReturn(mockHistoryOrderStateDao)
        `when`(mockDb.techParamsDao()).thenReturn(mockTechParamsDao)
        `when`(mockDb.addLoadDao()).thenReturn(mockAddLoadDao)
        `when`(mockDb.checkupDao()).thenReturn(mockCheckupDao)
        loginPresenter.syncDB()
        sleep(1000)
        verify(mockApiService).getListOrder()
        verify(mockApiService).getTechParams(anyInt())
        verify(mockApiService).getAddLoad(anyInt())
        verify(mockApiService).getCheckups()

        verify(mockOrdersDao).deleteOrders(anyList())
        verify(mockHistoryOrderStateDao).clearHistory()
    }


    @Test
    fun testSyncDBWithEmptyOrder() {
        val fakeListOrders= listOf<Orders>()
        val fakeTechParams= listOf(
            TechParams(id = 1, technical_characteristic = "Параметр1", idOrder = 1),
            TechParams(id = 2, technical_characteristic = "Параметр2", idOrder = 1)
        )
        val fakeAddLoad= listOf(
            AddLoad(id = 1, purpose = "Назначение1"),
            AddLoad(id = 1, purpose = "Назначение2")
        )
        val fakeCheckup= listOf(
            Checkup(idOrder = 1, numberOrder = "A-001", orderGuid = ""),
            Checkup(idOrder = 2, numberOrder = "A-002", orderGuid = "")
        )
        `when`(mockApiService.getCheckups()).thenReturn(Single.just(fakeCheckup))
        `when`(mockApiService.getAddLoad(anyInt())).thenReturn(Single.just(fakeAddLoad))
        `when`(mockApiService.getTechParams(anyInt())).thenReturn(Single.just(fakeTechParams))
        `when`(mockApiService.getListOrder()).thenReturn(Single.just(fakeListOrders))
        //`when`(mockApiService.getListOrder()).thenReturn(Single.error(ThrowHelper("Нет заявок12121")))
        `when`(mockDb.ordersDao()).thenReturn(mockOrdersDao)
        doNothing().`when`(mockOrdersDao).clearOrders()
        println(
            "Ошибка отлавливается в имплементации метода, в тесте не получается ее поймать\n " +
                    "в консоль ошибка валится из-за throwable.printStackTrace() в syncDB и syncOrder"
        )
        loginPresenter.syncDB()

        sleep(1000)
        verify(mockOrdersDao).clearOrders()
        verify(mockView).showFailureTextView("Нет заявок")
        verify(mockView).errorReceived(anyOrNull())
    }

    @Test
    fun testOnDestroy() {
        loginPresenter.onDestroy()
        Assert.assertNull(loginPresenter.view)
    }


}