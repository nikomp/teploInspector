package ru.bingosoft.teploInspector.ui.mainactivity

import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.models.Models

class MainActivityPresenterTest {
    lateinit var mainActivityPresenter: MainActivityPresenter
    private var mockView=Mockito.mock(MainActivityContractView::class.java)

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    lateinit var mockApiService: ApiService

    object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }
        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T =  null as T
    }

    @Before
    fun setUp() {
        val mockDb= Mockito.mock(AppDatabase::class.java)
        //val mockApiService=Mockito.mock(ApiService::class.java)

        mainActivityPresenter=MainActivityPresenter(mockDb)
        mainActivityPresenter.attachView(mockView)
        mainActivityPresenter.apiService=mockApiService
    }

    @Test
    fun `апи getAuthentication выполняется без ошибок`() {
        val url="https://mi.teploenergo-nn.ru/ldapauthentication/auth/login"
        val stLogin="test_inzhener"
        val stPassword="TGu(c5F4{q"
        val jsonBody = Gson().toJson(Models.LP(login = stLogin, password = stPassword))
            .toRequestBody("application/json".toMediaType())

        // Когда будет вызван метод с параметрами верунуть объект
        `when`(mockApiService.getAuthentication(url, jsonBody))
            .thenReturn(Single.just(Models.Uuid("fakeUuid")))

        val testObserver: TestObserver<Models.Uuid> = mockApiService.getAuthentication(url, jsonBody).test()
        testObserver.awaitTerminalEvent()
        testObserver
            .assertNoErrors()
            .assertValue(Models.Uuid("fakeUuid"))
            .isDisposed

        `when`(mockApiService.getAuthorization(MockitoHelper.anyObject()))
            .thenReturn(Single.just(Models.Token()))

        val testObserver2: TestObserver<Models.Token> = mockApiService.getAuthorization(MockitoHelper.anyObject()).test()
        testObserver2.awaitTerminalEvent()
        testObserver2
            .assertNoErrors()
            .isDisposed()


    }

}