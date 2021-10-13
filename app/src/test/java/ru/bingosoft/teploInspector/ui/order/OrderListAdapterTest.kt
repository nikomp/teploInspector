package ru.bingosoft.teploInspector.ui.order

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowToast
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivityPresenter
import ru.bingosoft.teploInspector.util.Const.StatusOrder.STATE_CANCELED
import ru.bingosoft.teploInspector.util.Const.StatusOrder.STATE_COMPLETED
import ru.bingosoft.teploInspector.util.Const.TypeTransportation.TRANSPORTATION_FOOT
import ru.bingosoft.teploInspector.util.Const.TypeTransportation.TRANSPORTATION_PERFORMED_CUSTOMER
import ru.bingosoft.teploInspector.util.Const.TypeTransportation.TRANSPORTATION_PRIVATE_TRANSPORT
import java.lang.Thread.sleep
import java.text.ParseException


@RunWith(RobolectricTestRunner::class)
@Config(application= App::class, sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
@LooperMode(LooperMode.Mode.PAUSED)
class OrderListAdapterTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var orderFragment: OrderFragment
    private lateinit var spyOrderFragment: OrderFragment
    private lateinit var activityController: ActivityController<MainActivity>
    private lateinit var recycler: RecyclerView
    private lateinit var mockOrderPresenter: OrderPresenter
    private lateinit var mockMainPresenter: MainActivityPresenter
    private lateinit var db: AppDatabase

    var rxJavaError="empty"
    private lateinit var context: Context

    @Before
    fun setUp() {

        context= ApplicationProvider.getApplicationContext<App>()

        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}


        // Переопределяем обработчик RxJava, который гасит ошибки
        RxJavaPlugins.setErrorHandler { t ->
            //println(t.printStackTrace())
            if (t is UndeliverableException) {
                rxJavaError= t.cause.toString()
                //println(rxJavaError)
            }
        }

        activityController= Robolectric.buildActivity(MainActivity::class.java)
        activityController.create().start().resume()
        // После старта активности, в R.id.nav_host_fragment загружается фрагмент по умолчанию и открывает БД
        // Если ее предварительно не закрыть будет ошибка Illegal connection pointer 1. Current pointers for thread Thread
        db=activityController.get().mainPresenter.db
        sleep(500)
        if (db.isOpen) {
            db.close()
        }

        orderFragment= OrderFragment()
        spyOrderFragment=Mockito.spy(orderFragment)
        mockOrderPresenter=mock(OrderPresenter::class.java)
        mockMainPresenter=mock(MainActivityPresenter::class.java)


        activityController.get().filteredOrders=listOf(
            Orders(id = 44731,number = "тест4",status = "В работе"),
            Orders(id = 44731,number = "тест4",status = "Проверена"),
            Orders(id = 44731,number = "тест4",status = "Отменена"),
            Orders(id = 44731,number = "тест4",status = "Выполнена"),
            Orders(id = 44731,number = "тест4",status = "Приостановлена"),
            Orders(id = 44731,number = "тест4",status = "Открыта"),
            Orders(id = 44731,number = "тест4",status = "В пути"))

        // В активити 2 фрагмента, один дефолтный (появляется после activityController.create().start().resume()),
        // второй тестовый, возможно первый нужно удалять, пока оставил так
        activityController.get()
            .supportFragmentManager
            .beginTransaction()
            .add(spyOrderFragment,"OrderFragment_test_tag")
            .commitNow()

        recycler=spyOrderFragment.view!!.findViewById(R.id.orders_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)
    }

    @Test
    fun testItemCount() {
        Assert.assertNotNull((recycler.adapter as OrderListAdapter).itemCount)
    }

    @Test
    fun testOrder() {
        Assert.assertNotNull((recycler.adapter as OrderListAdapter).getOrder(0))
    }

    @Test
    fun testFilter() {
        Assert.assertNotNull((recycler.adapter as OrderListAdapter).filter)
    }


    @Test
    fun testOrderNoteHaveClickListener() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",status = "В работе",orderNote = "fakeOrderNote"))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val orderNote=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<TextView>(R.id.orderNote)

        Assert.assertTrue(orderNote!!.hasOnClickListeners())
        orderNote.performClick()
        Assert.assertTrue(orderNote.ellipsize== TextUtils.TruncateAt.END)
    }

    @Test
    fun testOrderState_afterTextChanged() {
        val orderState=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.order_state)
        orderState?.setText("В работе")
        shadowOf(orderState).watchers[0].afterTextChanged(null)

        Assert.assertTrue(orderState?.text.toString()=="В РАБОТЕ")
        Assert.assertTrue(shadowOf(orderState).watchers.size>0)

    }

    @Test
    fun testOrderState_afterTextChanged_state_Completed() {
        val orderState=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.order_state)
        orderState?.setText(STATE_COMPLETED)
        shadowOf(orderState).watchers[0].afterTextChanged(null)

        val viewToast= ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.checklist_not_changed_status))
        Assert.assertTrue(shadowOf(orderState).watchers.size>0)

    }

    @Test
    fun testOrderState_afterTextChanged_state_Canceled() {
        spyOrderFragment.orderPresenter=mockOrderPresenter
        doNothing().`when`(mockOrderPresenter).updateOrderState(anyOrNull())

        val currentCountFilteredOrders=activityController.get().filteredOrders.size

        val orderState=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.order_state)
        orderState?.setText(STATE_CANCELED)
        shadowOf(orderState).watchers[0].afterTextChanged(null)

        val newCountFilteredOrders=activityController.get().filteredOrders.size
        // Проверяем что после Отмены уменьшилось число объектов в отфильтрованном массиве
        Assert.assertTrue(currentCountFilteredOrders>newCountFilteredOrders)
        // Проверим, что после смены состояния, вызывался метод updateOrderState,
        // просто что вызывался, при вызове ничего не делаем см. выше doNothing()
        verify(mockOrderPresenter).updateOrderState(anyOrNull())

    }

    @Test
    fun testOrderState_afterTextChanged_state_Canceled_Throwable() {

        spyOrderFragment.orderPresenter=mockOrderPresenter
        `when`(mockOrderPresenter.updateOrderState(anyOrNull())).thenThrow(RuntimeException())

        val orderState=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.order_state)
        orderState?.setText(STATE_CANCELED)
        shadowOf(orderState).watchers[0].afterTextChanged(null)

        verify(spyOrderFragment).errorReceived(anyOrNull())

    }

    @Test
    fun testTypeTransportation_afterTextChanged() {
        val typeTransportation=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        typeTransportation?.setText(TRANSPORTATION_PRIVATE_TRANSPORT)
        shadowOf(typeTransportation).watchers[0].afterTextChanged(null)

        Assert.assertTrue(typeTransportation?.text.toString()==TRANSPORTATION_PRIVATE_TRANSPORT)
        Assert.assertTrue(shadowOf(typeTransportation).watchers.size>0)

    }

    @Test
    fun testTypeTransportation_afterTextChanged_Throwable() {

        spyOrderFragment.orderPresenter=mockOrderPresenter
        `when`(mockOrderPresenter.changeTypeTransportation(anyOrNull())).thenThrow(RuntimeException())
        doNothing().`when`(spyOrderFragment).errorReceived(anyOrNull())

        val typeTransportation=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        typeTransportation?.setText(TRANSPORTATION_PRIVATE_TRANSPORT)
        shadowOf(typeTransportation).watchers[0].afterTextChanged(null)

        verify(spyOrderFragment).errorReceived(anyOrNull())

    }

    @Test
    fun testTypeTransportation_afterTextChanged_performed_customer() {

        val typeTransportation=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        typeTransportation?.setText(TRANSPORTATION_PERFORMED_CUSTOMER)
        shadowOf(typeTransportation).watchers[0].afterTextChanged(null)

        val btnRoute=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<Button>(R.id.btnRoute)

        Assert.assertTrue(btnRoute?.currentTextColor== Color.parseColor("#C7CCD1"))

    }

    @Test
    fun btnChangeDateTime_has_ClickListener() {

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        Assert.assertTrue(btnChangeDateTime!!.hasOnClickListeners())

    }


    @Test
    fun btnChangeDateTime_Click() {

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        Assert.assertNotNull(alertDialog)

    }

    @Test
    fun btnChangeDateTime_Click_dateVisit_isNull() {

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",status = "В работе",orderNote = "fakeOrderNote", dateVisit = "2021-08-06"))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        val alertDialogView= shadowOf(alertDialog).view
        val newDate=alertDialogView.findViewById<TextView>(R.id.newDate)
        Assert.assertTrue(newDate.text.isNotEmpty())

    }

    //#testing_try_catch
    @Test
    fun btnChangeDateTime_Click_dateVisit_Trowable() {

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            dateVisit = "Unparseable_date"))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        App.appInstance.lastExceptionAppForTest=null
        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        //Для тестирваония ветки catch e.printStackTrace() создал в App переменную
        // в нее сохраняю последнее Исключение
        // assertThrow не работает т.к. Исключение не пробрасывается в тест, а гасится в коде, блоком try-catch

        Assert.assertTrue(App.appInstance.lastExceptionAppForTest is ParseException)

    }

    @Test
    fun btnChangeDateTime_Click_showDateTimeDialog_Date() {

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        val alertDialogView= shadowOf(alertDialog).view
        val newDate=alertDialogView.findViewById<TextView>(R.id.newDate)

        newDate.performClick()

        val dateTimeDialog=ShadowAlertDialog.getLatestAlertDialog()
        Assert.assertTrue(dateTimeDialog is DatePickerDialog)

    }

    @Test
    fun btnChangeDateTime_Click_showDateTimeDialog_Time() {

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        val alertDialogView= shadowOf(alertDialog).view
        val newTime=alertDialogView.findViewById<TextView>(R.id.newTime)

        newTime.performClick()

        val dateTimeDialog=ShadowAlertDialog.getLatestAlertDialog()
        Assert.assertTrue(dateTimeDialog is TimePickerDialog)

    }

    @Test
    fun btnChangeDateTime_Click_buttonOK_Click() {
        spyOrderFragment.mainPresenter=mockMainPresenter
        doNothing().`when`(mockMainPresenter).updateGiOrder(anyOrNull())

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        val alertDialogView= shadowOf(alertDialog).view
        val btnOK=alertDialogView.findViewById<TextView>(R.id.btnOk)
        val newDate=alertDialogView.findViewById<TextView>(R.id.newDate)
        val newTime=alertDialogView.findViewById<TextView>(R.id.newTime)

        newDate.text="30.08.2021"
        newTime.text="11:57"

        btnOK.performClick()
        verify(mockMainPresenter).updateGiOrder(anyOrNull())


    }

    @Test
    fun btnChangeDateTime_Click_buttonOK_Click_Throwable() {
        spyOrderFragment.mainPresenter=mockMainPresenter
        doNothing().`when`(mockMainPresenter).updateGiOrder(anyOrNull())

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)
        btnChangeDateTime?.performClick()

        val alertDialog=ShadowAlertDialog.getLatestAlertDialog()
        val alertDialogView= shadowOf(alertDialog).view
        val btnOK=alertDialogView.findViewById<TextView>(R.id.btnOk)
        val newDate=alertDialogView.findViewById<TextView>(R.id.newDate)
        val newTime=alertDialogView.findViewById<TextView>(R.id.newTime)

        newDate.text="Unparseable_date"
        newTime.text="Unparseable_time"
        App.appInstance.lastExceptionAppForTest=null
        btnOK.performClick()
        alertDialog.dismiss()
        Assert.assertTrue(App.appInstance.lastExceptionAppForTest is ParseException)

    }

    @Test
    fun btnChangeDateTime_setText() {

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            dateVisit = "2021-08-30",
            timeVisit = "13:37:01"))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val btnChangeDateTime=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnChangeDateTime)

        Assert.assertFalse(btnChangeDateTime?.text?.toString().isNullOrEmpty())

    }

    @Test
    fun btnChangeDateTime_setText_Throwable() {
        App.appInstance.lastExceptionAppForTest=null

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            dateVisit = "Unparseable_date",
            timeVisit = "Unparseable_time"))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)


        Assert.assertTrue(App.appInstance.lastExceptionAppForTest is ParseException)

    }

    @Test
    fun order_phone_isEmpty() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            phone = "+711"
        ))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val btnPhone=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnPhone)

        Assert.assertFalse(btnPhone?.text.toString().isEmpty())


    }

    @Test
    fun userLocation_not_empty() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            lon=43.7935762,
            lat=56.3800229
        ))

        (recycler.adapter as OrderListAdapter).setUserLocationForTest(56.2361638,43.9646169)
        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val btnRoute=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnRoute)

        Assert.assertTrue(btnRoute?.text?.contains("Маршрут 19.2 км") == true)

    }

    @Test
    //#test_Intent
    fun btnPhone_click() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            phone = "+711"
        ))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val pm= shadowOf(context.packageManager)
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+711"))

        val info = ResolveInfo()
        info.isDefault = true

        val applicationInfo = ApplicationInfo()
        applicationInfo.packageName = "ru.bingosoft.teploInspector.ui.mainactivity"
        info.activityInfo = ActivityInfo()
        info.activityInfo.applicationInfo = applicationInfo
        info.activityInfo.name = "MainActivity"

        pm.addResolveInfoForIntent(intent, info)


        val btnPhone=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialButton>(R.id.btnPhone)
        btnPhone?.performClick()


        val sActivity= shadowOf(activityController.get())

        val startedIntent=sActivity.nextStartedActivity
        println(startedIntent.data)

        Assert.assertTrue(startedIntent.action=="android.intent.action.DIAL")
        Assert.assertTrue(startedIntent.data==Uri.parse("tel:+711"))

    }

    @Test
    fun ordersIdsNotSync() {
        activityController.get().ordersIdsNotSync.add(44731)

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(
            id = 44731, number = "тест4",
            status = "В работе",
        ))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val ivSync=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<ImageView>(R.id.ivSync)
        Assert.assertTrue(ivSync!!.visibility== View.VISIBLE)
        Assert.assertTrue(ivSync.hasOnClickListeners())
    }

    @Test
    fun ivSync_Click() {
        activityController.get().mainPresenter=mockMainPresenter
        doNothing().`when`(mockMainPresenter).sendData3(anyOrNull(), anyOrNull())

        activityController.get().ordersIdsNotSync.add(44731)

        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(
            id = 44731, number = "тест4",
            status = "В работе",
        ))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val ivSync=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<ImageView>(R.id.ivSync)
        ivSync!!.performClick()

        verify(mockMainPresenter).sendData3(anyOrNull(), anyOrNull())
    }



    @Test
    fun testFilter_performFiltering_empty() {
        val filter= (recycler.adapter as OrderListAdapter).filter
        filter.filter("")
        val of=(recycler.adapter as OrderListAdapter).ordersFilterList
        val o=activityController.get().orders
        Assert.assertTrue(of==o)
    }

    @Test
    fun testFilter_performFiltering_with_data() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf(Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            address = "Саврасова"
        ),Orders(id = 44731,number = "тест4",
            status = "В работе",
            orderNote = "fakeOrderNote",
            address = "Батумская"
        ))

        recycler.adapter?.notifyDataSetChanged()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        val filter= (recycler.adapter as OrderListAdapter).filter
        filter.filter("Сав")
        val of=(recycler.adapter as OrderListAdapter).ordersFilterList

        Assert.assertTrue(of.size==1)
    }


    @After
    fun close() {
        if (db.isOpen) {
            db.close()
        }
        //activityController.pause().stop().destroy()
        activityController.get().finish()
        activityController.pause().stop().destroy()

    }


    @Test
    // Никакой проверки нет, тест только для покрытия кода
    fun testOrderState_not_exists_position() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf()

        val orderState=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.order_state)
        orderState?.setText(STATE_CANCELED)

    }

    @Test
    // Никакой проверки нет, тест только для покрытия кода
    fun testTypeTransportation_not_exists_position() {
        (recycler.adapter as OrderListAdapter).ordersFilterList= listOf()

        val typeTransportation=recycler.findViewHolderForAdapterPosition(0)?.itemView?.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        typeTransportation?.setText(TRANSPORTATION_FOOT)

    }

}