package ru.bingosoft.teploInspector.ui.order

import android.os.Build
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
class OrderFragmentTest {
    private lateinit var orderFragment: OrderFragment
    lateinit var mockToaster: Toaster
    lateinit var mockOrderPresenter: OrderPresenter
    lateinit var mockSharedPrefSaver: SharedPrefSaver

    @Before
    fun setUp() {
        mockToaster= mock(Toaster::class.java)
        mockOrderPresenter=mock(OrderPresenter::class.java)
        mockSharedPrefSaver=mock(SharedPrefSaver::class.java)
        orderFragment= OrderFragment()
        orderFragment.toaster=mockToaster
        orderFragment.orderPresenter=mockOrderPresenter
        orderFragment.sharedPref=mockSharedPrefSaver
    }

    @Test
    fun testGetToaster() {
        Assert.assertNotNull(orderFragment.toaster)
    }

    @Test
    fun testShowMessageLogin() {
        orderFragment.showMessageLogin(1)
        verify(mockToaster).showToast(1)
    }

    @Test
    fun testShowMessageLoginString() {
        orderFragment.showMessageLogin("fakeString")
        verify(mockToaster).showToast("fakeString")
    }

    @Test
    fun testShowOrders() {
        orderFragment.showOrders()
        verify(mockOrderPresenter).attachView(anyOrNull())
        verify(mockOrderPresenter).loadOrders()
    }

    @Test
    fun testSaveToken() {
        orderFragment.saveToken("fakeToken")
        `when`(mockSharedPrefSaver.sptoken).thenReturn("fakeToken")
        assertEquals("fakeToken",mockSharedPrefSaver.sptoken)
        verify(mockSharedPrefSaver).saveToken("fakeToken")
    }

    @Test
    fun testSaveLoginPasswordToSharedPreference() {
        orderFragment.saveLoginPasswordToSharedPreference("fakeLogin","fakePassword")
        verify(mockSharedPrefSaver).saveLogin("fakeLogin")
        verify(mockSharedPrefSaver).savePassword("fakePassword")
        verify(mockSharedPrefSaver).saveAuthFlag()
    }
}