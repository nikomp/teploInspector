package ru.bingosoft.teploInspector.ui.checkup

import android.os.Build
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.contains
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.LockStateOrder.STATE_OPEN

@RunWith(RobolectricTestRunner::class)
@Config(application= App::class, sdk = [Build.VERSION_CODES.O_MR1])
@Ignore("Долгая отладка других тестов, убрать как будут разработаны остальные тесты")
@LooperMode(LooperMode.Mode.PAUSED)
class StepsAdapterTest {

    private lateinit var testScheduler: TestScheduler
    private lateinit var checkupFragment: CheckupFragment
    private lateinit var activityController: ActivityController<MainActivity>
    var rxJavaError="empty"

    @Before
    fun setUp() {
        testScheduler= TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline()}


        // Переопределяем обработчик RxJava, который гасит ошибки
        RxJavaPlugins.setErrorHandler {t ->
            //println(t.printStackTrace())
            if (t is UndeliverableException) {
                rxJavaError= t.cause.toString()
                //println(rxJavaError)
            }
        }

        checkupFragment=CheckupFragment()
        val bundle = Bundle()
        bundle.putBoolean("checkUpForOrder", true)
        bundle.putLong("idOrder", 0)
        bundle.putString("typeOrder", "fakeTypeOrder")
        checkupFragment.arguments=bundle
        activityController=Robolectric.buildActivity(MainActivity::class.java)
        activityController.create().start().resume()
        activityController.get()
            .supportFragmentManager
            .beginTransaction()
            .add(checkupFragment,"CheckupFragment_tag222")
            .commitNow()


    }

    @Test
    fun testClickGeneralInformation() {
        val positionGI=0

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // только после обновления получаем дочерние View
        val newRvView=recycler.findViewHolderForAdapterPosition(positionGI)?.itemView
        val llMain=newRvView?.findViewById<LinearLayout>(R.id.llMain)
        val rvgi= llMain?.findViewById<RecyclerView>(R.id.rvgi)

        Assert.assertTrue(llMain?.childCount!! >0)
        Assert.assertTrue(llMain.visibility == View.VISIBLE)
        Assert.assertTrue(rvgi?.adapter is GeneralInformationAdapter)

    }

    @Test
    fun testClickTChShowToast() {
        val positionGI=1

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val viewToast=ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.th_is_empty))

    }

    @Test
    fun testClickTCh_llMainUiTX_not_empty() {
        val positionGI=1

        val tvDummy=TextView(activityController.get().applicationContext)
        checkupFragment.llMainUiTX.add(tvDummy)

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val newRvView=recycler.findViewHolderForAdapterPosition(positionGI)?.itemView
        val llMain=newRvView?.findViewById<LinearLayout>(R.id.llMain)

        Assert.assertTrue(llMain?.contains(tvDummy)!!)

    }

    @Test
    fun testClickTCh_techParams_not_empty() {
        val positionGI=1

        checkupFragment.techParams=listOf(TechParams(idOrder = 0))

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        Assert.assertNotNull(checkupFragment.txCreator)


    }


    @Test
    fun testClickTCh_parentFragment_not_attach() {
        val positionGI=1

        checkupFragment.techParams=listOf(TechParams(idOrder = 0))



        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // Отсоединяем фрагмент от активити
        activityController.get()
            .supportFragmentManager
            .beginTransaction()
            .remove(checkupFragment)
            .commitNow()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val viewToast=ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.error_unable_upload_checklist))

    }

    @Test
    fun testAdditionalLoad_addLoads_empty() {
        val positionGI=2

        //checkupFragment.addLoads= listOf(AddLoad(purpose = "fakePurpose"))

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val viewToast=ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.al_is_empty))

    }

    @Test
    fun testAdditionalLoad_addLoads_not_empty() {
        val positionGI=2

        checkupFragment.addLoads= listOf(AddLoad(purpose = "fakePurpose"))


        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val newRvView=recycler.findViewHolderForAdapterPosition(positionGI)?.itemView
        val llMain=newRvView?.findViewById<LinearLayout>(R.id.llMain)

        Assert.assertTrue(llMain?.childCount!! >0)

    }

    @Test
    fun testClickCheckup() {
        val positionGI=3

        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val viewToast=ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        println(textToast.text)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.checklist_is_empty))

    }

    @Test
    fun testClickCheckup_llMainUi_not_empty() {
        val positionGI=3

        val tvDummy=TextView(activityController.get().applicationContext)
        checkupFragment.llMainUi.add(tvDummy)


        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val newRvView=recycler.findViewHolderForAdapterPosition(positionGI)?.itemView
        val llMain=newRvView?.findViewById<LinearLayout>(R.id.llMain)

        Assert.assertTrue(llMain?.contains(tvDummy)!!)

    }

    @Test
    fun testClickCheckup_checkup_is_init() {
        val positionGI=3

        //val fakeCheckup="[{\"results_id\":966045,\"results_guid\":\"c0619459-bcdc-4c7c-9886-2cef770b72f2\",\"id_question\":112,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b38c0ca8-dbbb-448b-a945-405b6f78cdc0\",\"sequence_question\":1,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"СПЭС\",\"type\":\"combobox\",\"value\":[\"Да\",\"Нет\"]}]"
        val fakeCheckup="[{\"results_id\":966045,\"results_guid\":\"c0619459-bcdc-4c7c-9886-2cef770b72f2\",\"id_question\":112,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b38c0ca8-dbbb-448b-a945-405b6f78cdc0\",\"sequence_question\":1,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"СПЭС\",\"type\":\"combobox\",\"value\":[\"Да\",\"Нет\"]},{\"results_id\":966046,\"results_guid\":\"cc67d6e4-7cd0-42dd-b26e-cc55c75030f5\",\"id_question\":113,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"aa8b755b-dbc0-481c-ae54-9b9e27fd4b6e\",\"sequence_question\":2,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Шум от оборудования потребителя\",\"type\":\"combobox\",\"value\":[\"Да\",\"Нет\"]},{\"results_id\":966047,\"results_guid\":\"20a2af8b-ebfd-4010-bdb8-e581a8014c01\",\"id_question\":114,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"2b5ad0ee-1e47-4457-87f9-02828e4f4c1c\",\"sequence_question\":3,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Шум от оборудования  АО \\\"ТЭ\\\".\",\"type\":\"combobox\",\"value\":[\"Да\",\"Нет\"]},{\"results_id\":966048,\"results_guid\":\"885d72ec-16f3-4c39-9e83-5788207f2208\",\"id_question\":115,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"50a51af9-2189-4fc7-8fc1-9a60ae3891fb\",\"sequence_question\":4,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966049,\"results_guid\":\"8941b211-30d8-4d5a-989a-829a42ba0d4d\",\"id_question\":116,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5da23ec0-1072-45e1-8dc2-41e81c55174e\",\"sequence_question\":5,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т2 факт,°C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966050,\"results_guid\":\"7f286b15-bc5d-4a0b-8aab-dce9811451de\",\"id_question\":117,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fd62e3d1-6779-47fa-bc2c-2182ee8a7df9\",\"sequence_question\":6,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р1 факт , кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966051,\"results_guid\":\"5c122eab-184f-46bd-990f-933fa82800e6\",\"id_question\":118,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"66650f53-14a1-4223-b557-2c96a445c3b9\",\"sequence_question\":7,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966052,\"results_guid\":\"09090b1c-d41e-4fc0-9554-ec4733ede4cb\",\"id_question\":119,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"0aef2c6b-7eef-4f5c-a32e-5db4f2e1441c\",\"sequence_question\":8,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G1 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966053,\"results_guid\":\"756654b5-79bc-41d4-944f-74f3eaeb80e6\",\"id_question\":120,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"e9f0c40a-c718-4ca2-b3f8-6939e4cabcc9\",\"sequence_question\":9,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"Источник\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G2 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966054,\"results_guid\":\"4ab47f31-57cb-4fd8-b69c-a3ea0fc0ba6a\",\"id_question\":121,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"0c42eb33-a5f8-4ecf-a4aa-b344a28656cb\",\"sequence_question\":10,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G1 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966055,\"results_guid\":\"bacf94af-b0ae-4f5d-8934-12c3dfe8e0ed\",\"id_question\":122,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"f592c6fb-ec58-45f6-b130-fa76afb3687c\",\"sequence_question\":11,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G2 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966056,\"results_guid\":\"8210bcc0-8804-4df9-b09e-e55a5c050fe4\",\"id_question\":123,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"4be45a54-aed7-4662-8a4b-e6c9d4616209\",\"sequence_question\":12,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966057,\"results_guid\":\"70586527-8bca-4c42-887b-7b7fea3816e6\",\"id_question\":124,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ccf2c658-2147-410e-9e13-9df9a235f329\",\"sequence_question\":13,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966058,\"results_guid\":\"4a857ea2-801c-422b-91b5-4c232f55fa6e\",\"id_question\":125,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"24cdce2e-3acd-4569-ac6d-578da214a781\",\"sequence_question\":14,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966059,\"results_guid\":\"d5c6f5af-aed1-4812-8c50-4cbc00762aec\",\"id_question\":126,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"de91ba4f-8d5b-4908-a6b4-f5ed3a0e333d\",\"sequence_question\":15,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966060,\"results_guid\":\"11ca8f7b-f223-4ee2-b903-6899312a0348\",\"id_question\":127,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"e2d01d2a-0b12-460b-8012-961c005e7672\",\"sequence_question\":16,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G1.1 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966061,\"results_guid\":\"94d6423c-c67f-4ca0-b7b6-85576cd9357c\",\"id_question\":128,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"a3e7da3a-0643-4710-bf9b-c3fd822bdc8d\",\"sequence_question\":17,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G2.1 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966062,\"results_guid\":\"b09d60bc-1496-454d-b5c3-35e1381554a3\",\"id_question\":129,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b8a99b4e-78db-41da-9a3f-81bb93be2544\",\"sequence_question\":18,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р1.1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966063,\"results_guid\":\"ba0697ff-832b-4c82-913a-9eca88a6904a\",\"id_question\":130,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"2c636eed-1112-4f14-8d33-d32d1cddbb6d\",\"sequence_question\":19,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р2.1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966064,\"results_guid\":\"7b9d9093-4ef7-4a9c-af6b-7d15c8a764f2\",\"id_question\":131,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"1ea9239e-f840-41fd-a353-4aa6fd94a6f2\",\"sequence_question\":20,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т1.1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966065,\"results_guid\":\"f8ca8c44-b787-4479-8660-ff728b589437\",\"id_question\":132,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"14aab0a1-6f47-427d-8566-f85b8d4d2065\",\"sequence_question\":21,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ЦТП\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т2.1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966066,\"results_guid\":\"b69e6345-8988-45b7-8d38-6c99fc15a43f\",\"id_question\":133,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b1797b07-88ae-4ad2-83b9-30585f47a98b\",\"sequence_question\":22,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966067,\"results_guid\":\"a5834d4a-9ce1-49d7-82c3-7a513f643f6e\",\"id_question\":133,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b1797b07-88ae-4ad2-83b9-30585f47a98b\",\"sequence_question\":22,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966068,\"results_guid\":\"237aa29b-1095-4816-9906-21ab8c78aafc\",\"id_question\":133,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b1797b07-88ae-4ad2-83b9-30585f47a98b\",\"sequence_question\":22,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966069,\"results_guid\":\"50197004-c2bc-414c-aee9-748404850caa\",\"id_question\":133,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b1797b07-88ae-4ad2-83b9-30585f47a98b\",\"sequence_question\":22,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966070,\"results_guid\":\"8354933d-c310-433c-b79e-5f06f8c72216\",\"id_question\":133,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b1797b07-88ae-4ad2-83b9-30585f47a98b\",\"sequence_question\":22,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966071,\"results_guid\":\"d2637faa-7a1e-4cfb-8db5-6a0c94f3f640\",\"id_question\":134,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"06866243-0d0a-499d-a186-dfec9d2ebc60\",\"sequence_question\":23,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"G1 общ. факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966072,\"results_guid\":\"87c7eb0d-00c6-426a-8ad7-e8035d9bdc5b\",\"id_question\":134,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"06866243-0d0a-499d-a186-dfec9d2ebc60\",\"sequence_question\":23,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"G1 общ. факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966073,\"results_guid\":\"2c00644e-0e4c-43d2-b193-e09a4f7715f8\",\"id_question\":134,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"06866243-0d0a-499d-a186-dfec9d2ebc60\",\"sequence_question\":23,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"G1 общ. факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966074,\"results_guid\":\"bafb1803-19b7-484b-baa4-f1f5168246a1\",\"id_question\":134,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"06866243-0d0a-499d-a186-dfec9d2ebc60\",\"sequence_question\":23,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"G1 общ. факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966075,\"results_guid\":\"ad7d600f-e7fa-4e92-9b14-5675c73c295b\",\"id_question\":134,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"06866243-0d0a-499d-a186-dfec9d2ebc60\",\"sequence_question\":23,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"G1 общ. факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966076,\"results_guid\":\"2d376dbb-c6e3-48eb-82b0-efa9181be03e\",\"id_question\":135,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7e542c91-1902-4736-9099-22c4a931f34f\",\"sequence_question\":24,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"G2 общ. факт,  м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966077,\"results_guid\":\"c6b71a43-8607-4db8-b443-87ee5ae09912\",\"id_question\":135,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7e542c91-1902-4736-9099-22c4a931f34f\",\"sequence_question\":24,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"G2 общ. факт,  м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966078,\"results_guid\":\"7b6ef305-0a78-4851-ba80-bbbeccd57ef4\",\"id_question\":135,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7e542c91-1902-4736-9099-22c4a931f34f\",\"sequence_question\":24,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"G2 общ. факт,  м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966079,\"results_guid\":\"22d1c681-cb58-4b4f-ac11-86a63f616e2e\",\"id_question\":135,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7e542c91-1902-4736-9099-22c4a931f34f\",\"sequence_question\":24,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"G2 общ. факт,  м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966080,\"results_guid\":\"f2d8b4a2-4925-44a9-8756-0ad72dc5f3cd\",\"id_question\":135,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7e542c91-1902-4736-9099-22c4a931f34f\",\"sequence_question\":24,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"G2 общ. факт,  м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966081,\"results_guid\":\"3d7ccdf5-c573-4967-8c62-845f8294e8a8\",\"id_question\":136,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ca7c9fa4-a8a5-48ec-a713-7c1e7a126306\",\"sequence_question\":25,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"G1 отоп факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966082,\"results_guid\":\"c3674e7d-423c-4b17-99b0-1507195206df\",\"id_question\":136,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ca7c9fa4-a8a5-48ec-a713-7c1e7a126306\",\"sequence_question\":25,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"G1 отоп факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966083,\"results_guid\":\"26a4c0e0-b0cc-4d5f-b4c8-b60794b93d05\",\"id_question\":136,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ca7c9fa4-a8a5-48ec-a713-7c1e7a126306\",\"sequence_question\":25,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"G1 отоп факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966084,\"results_guid\":\"a0d55766-d14c-46cd-9c9f-cfd96844d2cd\",\"id_question\":136,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ca7c9fa4-a8a5-48ec-a713-7c1e7a126306\",\"sequence_question\":25,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"G1 отоп факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966085,\"results_guid\":\"93a76bb9-8990-4c20-b676-c2b42de46cea\",\"id_question\":136,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ca7c9fa4-a8a5-48ec-a713-7c1e7a126306\",\"sequence_question\":25,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"G1 отоп факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966086,\"results_guid\":\"0f369f52-efd8-4081-934b-61a6152344b5\",\"id_question\":137,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"28bf315e-b948-4edd-be12-5aafa3441a8f\",\"sequence_question\":26,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"G1 гвс факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966087,\"results_guid\":\"a45648a1-ee69-415f-90ef-a1e8e8920633\",\"id_question\":137,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"28bf315e-b948-4edd-be12-5aafa3441a8f\",\"sequence_question\":26,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"G1 гвс факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966088,\"results_guid\":\"9530a815-8ebe-47e7-b754-0acf2a133e9a\",\"id_question\":137,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"28bf315e-b948-4edd-be12-5aafa3441a8f\",\"sequence_question\":26,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"G1 гвс факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966089,\"results_guid\":\"e1a02732-3f14-4970-89ed-b5d61e899fbf\",\"id_question\":137,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"28bf315e-b948-4edd-be12-5aafa3441a8f\",\"sequence_question\":26,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"G1 гвс факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966090,\"results_guid\":\"cd57301b-899d-47e9-850b-21861e65e414\",\"id_question\":137,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"28bf315e-b948-4edd-be12-5aafa3441a8f\",\"sequence_question\":26,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"G1 гвс факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966091,\"results_guid\":\"366a64d6-cd34-42fc-91ae-ef58f3a98788\",\"id_question\":138,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5fcb6ea0-b933-4ed8-b371-f0963baeca7a\",\"sequence_question\":27,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"G1 вент факт., м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966092,\"results_guid\":\"a3120097-a816-41a9-9781-18ca7eaa9af7\",\"id_question\":138,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5fcb6ea0-b933-4ed8-b371-f0963baeca7a\",\"sequence_question\":27,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"G1 вент факт., м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966093,\"results_guid\":\"28cce465-3d09-4a20-9167-9a4654ea12a5\",\"id_question\":138,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5fcb6ea0-b933-4ed8-b371-f0963baeca7a\",\"sequence_question\":27,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"G1 вент факт., м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966094,\"results_guid\":\"5e8f04ff-a6ae-4d7b-aa7d-a4aa8ed17cb1\",\"id_question\":138,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5fcb6ea0-b933-4ed8-b371-f0963baeca7a\",\"sequence_question\":27,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"G1 вент факт., м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966095,\"results_guid\":\"3a6caf68-3251-4953-bc8d-d42898d91865\",\"id_question\":138,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5fcb6ea0-b933-4ed8-b371-f0963baeca7a\",\"sequence_question\":27,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"G1 вент факт., м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966096,\"results_guid\":\"456ac45e-8a86-4ccb-8d90-d4981799b43a\",\"id_question\":139,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5c1da7f3-29cb-47f3-8087-79cf15f7f4cc\",\"sequence_question\":28,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966097,\"results_guid\":\"1506fdd2-2cf2-41b4-8ce0-fbefd01c7478\",\"id_question\":139,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5c1da7f3-29cb-47f3-8087-79cf15f7f4cc\",\"sequence_question\":28,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966098,\"results_guid\":\"34788a73-062a-4f02-8f94-1a0f15ada324\",\"id_question\":139,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5c1da7f3-29cb-47f3-8087-79cf15f7f4cc\",\"sequence_question\":28,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966099,\"results_guid\":\"a09f6977-3ee8-4f61-91a2-bd3b31b8d0fd\",\"id_question\":139,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5c1da7f3-29cb-47f3-8087-79cf15f7f4cc\",\"sequence_question\":28,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966100,\"results_guid\":\"20ad3eab-4ef3-4a99-9651-38ab3bb79607\",\"id_question\":139,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5c1da7f3-29cb-47f3-8087-79cf15f7f4cc\",\"sequence_question\":28,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966101,\"results_guid\":\"4af7f687-63c0-4cd6-855d-be0694b75b1d\",\"id_question\":140,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"60370687-aa8c-4e0b-abe1-4ab2189b1878\",\"sequence_question\":29,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966102,\"results_guid\":\"1449f18d-b2a9-42eb-8485-c9ffba285ec7\",\"id_question\":140,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"60370687-aa8c-4e0b-abe1-4ab2189b1878\",\"sequence_question\":29,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966103,\"results_guid\":\"e5081e25-96ce-4c53-aedb-89224a39d17d\",\"id_question\":140,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"60370687-aa8c-4e0b-abe1-4ab2189b1878\",\"sequence_question\":29,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966104,\"results_guid\":\"653edc12-d05d-49c0-b6d8-56cc6054508e\",\"id_question\":140,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"60370687-aa8c-4e0b-abe1-4ab2189b1878\",\"sequence_question\":29,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966105,\"results_guid\":\"2e6a91b1-64e6-4c2a-8f98-123123436f4b\",\"id_question\":140,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"60370687-aa8c-4e0b-abe1-4ab2189b1878\",\"sequence_question\":29,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966106,\"results_guid\":\"a93ed987-2704-4132-b4fc-02badf8f2a3b\",\"id_question\":141,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"85169608-0db8-4679-a333-1980ec9b4872\",\"sequence_question\":30,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966107,\"results_guid\":\"82c5e532-285d-41ab-909e-89454bb4a63d\",\"id_question\":141,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"85169608-0db8-4679-a333-1980ec9b4872\",\"sequence_question\":30,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966108,\"results_guid\":\"1bef8cf8-50b1-4e3c-8852-e98822e157e5\",\"id_question\":141,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"85169608-0db8-4679-a333-1980ec9b4872\",\"sequence_question\":30,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966109,\"results_guid\":\"13d00f98-f970-4f47-b090-0389b3fd8d73\",\"id_question\":141,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"85169608-0db8-4679-a333-1980ec9b4872\",\"sequence_question\":30,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966110,\"results_guid\":\"8b76cb23-ad2b-41c4-9d6b-1bb42651fd8d\",\"id_question\":141,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"85169608-0db8-4679-a333-1980ec9b4872\",\"sequence_question\":30,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966111,\"results_guid\":\"c8b03abb-c617-43f4-86ae-e03bb52ad5d9\",\"id_question\":142,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fa5d4ff3-0b88-40af-aff9-51092f2b7ea0\",\"sequence_question\":31,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"1\",\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966112,\"results_guid\":\"2be296c9-32f1-4039-961f-417ec1b2d6ae\",\"id_question\":142,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fa5d4ff3-0b88-40af-aff9-51092f2b7ea0\",\"sequence_question\":31,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"2\",\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966113,\"results_guid\":\"6e349ff3-bd8b-4569-bd3d-030165b6d679\",\"id_question\":142,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fa5d4ff3-0b88-40af-aff9-51092f2b7ea0\",\"sequence_question\":31,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"3\",\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966114,\"results_guid\":\"2e4f5dad-e577-4251-a9fe-3102a9e0e702\",\"id_question\":142,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fa5d4ff3-0b88-40af-aff9-51092f2b7ea0\",\"sequence_question\":31,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"4\",\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966115,\"results_guid\":\"e3e171d6-de5a-4efb-81e3-13d1658df470\",\"id_question\":142,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fa5d4ff3-0b88-40af-aff9-51092f2b7ea0\",\"sequence_question\":31,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":\"ИТП(общий ввод)\",\"replicated_on\":5,\"node_itp\":\"5\",\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966116,\"results_guid\":\"45a777d1-74aa-4f5b-9bed-f8b1c1671d77\",\"id_question\":143,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5b782340-dbcc-43ac-a37b-757841ba61cd\",\"sequence_question\":32,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"№ узла\",\"type\":\"textinput\",\"hint\":\"Введите значение\"},{\"results_id\":966117,\"results_guid\":\"a34cb340-480f-4e10-ab05-5eb3efd51869\",\"id_question\":144,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"bca3fb00-3c4e-48f7-9bc8-b5ff5431ef95\",\"sequence_question\":33,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Темп.график ВСО\",\"type\":\"textinput\",\"hint\":\"Введите значение\"},{\"results_id\":966118,\"results_guid\":\"e5dd2d22-9132-4bfc-8a3d-e6e5fa62bf0e\",\"id_question\":145,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"fabf4972-d4f0-4248-94eb-9ad29291ee16\",\"sequence_question\":34,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G1 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966119,\"results_guid\":\"a818a01c-c78f-47e3-a0f3-8dd87fed39a5\",\"id_question\":146,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"0e856301-7ce6-4f08-adcc-c605d84e3a94\",\"sequence_question\":35,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G2 факт, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966120,\"results_guid\":\"9ae5910e-c1cc-4ca5-95a4-e8c69f87ef2d\",\"id_question\":147,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"8e16bdb5-fbe4-4dd3-9a17-07fa0ce11379\",\"sequence_question\":36,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р1 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966121,\"results_guid\":\"98d548c8-204a-45b1-9e0e-1bf07b837144\",\"id_question\":148,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"d89bc1ba-0544-45bb-8d8f-c168d0a39466\",\"sequence_question\":37,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р2 факт, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966122,\"results_guid\":\"910942c0-c07d-4282-bada-099d3397bda9\",\"id_question\":149,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5338d58a-b8d0-4f1e-905a-242698ff59c6\",\"sequence_question\":38,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т1 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966123,\"results_guid\":\"fb8124d6-4919-462c-8b78-c1f2d44440a8\",\"id_question\":150,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"3cc08620-9edd-48a8-805a-3f5f402a4e8d\",\"sequence_question\":39,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т2 факт, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966124,\"results_guid\":\"b380ce07-0187-4c43-bc91-c68ab5e7303b\",\"id_question\":151,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"da6a9c7d-e793-4cf9-81d3-99d4bd10a687\",\"sequence_question\":40,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"P'1 после ш., кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966125,\"results_guid\":\"a21a9896-db3f-4002-8bc6-1448a5f0eb77\",\"id_question\":152,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"5afa2be2-994e-49ad-b09d-879dc02d8110\",\"sequence_question\":41,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"P'2 до ш., кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966126,\"results_guid\":\"20935a0a-b667-49b5-8919-dabe8b0a1b0b\",\"id_question\":153,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b0b99057-4429-4e7d-b378-2e68292d1dc1\",\"sequence_question\":42,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р1.1, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966127,\"results_guid\":\"05ca975f-23e6-4792-82f3-6989d440026c\",\"id_question\":154,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"8ac75ca7-b0e5-4526-81aa-b9d7db490ba3\",\"sequence_question\":43,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Р2.1, кгс/см2\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966128,\"results_guid\":\"7ce1b0ab-f93f-424e-85fb-1a238f78dbbe\",\"id_question\":155,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"61f5da4f-322b-4eb7-b001-822b47af6e50\",\"sequence_question\":44,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т1.1, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966129,\"results_guid\":\"2a74b905-06a6-4fd9-9a76-386155f1cfec\",\"id_question\":156,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"ac93c8d3-8362-4b67-b12a-004a1abbced2\",\"sequence_question\":45,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Т2.1, °C\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966130,\"results_guid\":\"292e5c7b-ffdd-4c64-a2f0-61bd6333e15a\",\"id_question\":157,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"7435bda1-4ad1-4b21-b9d9-f22afd8cfca0\",\"sequence_question\":46,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G1.1, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966131,\"results_guid\":\"21668cf0-c0ab-46f0-8762-4a649236cdcc\",\"id_question\":158,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"03fb0722-076f-41fc-9f42-ccd586810cb1\",\"sequence_question\":47,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"G2.1, м3/ч\",\"type\":\"numeric\",\"minrange\":null,\"maxrange\":null,\"hint\":\"Введите значение\"},{\"results_id\":966132,\"results_guid\":\"e57d60ec-5f64-430e-abe7-43ac99d959d9\",\"id_question\":159,\"node\":1,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"05f557e0-402a-4b6e-aa9a-453690d770a7\",\"sequence_question\":48,\"replication_nodes\":true,\"replicating_archival_records\":false,\"group_checklist\":\"Узел\",\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Примечание\",\"type\":\"textinput\",\"hint\":\"Введите значение\"},{\"results_id\":966133,\"results_guid\":\"c589254e-bacb-47f4-b37a-f26d83d841dd\",\"id_question\":160,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"507fac07-8240-4601-a779-11da0d31fe2d\",\"sequence_question\":49,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Нарушения\",\"type\":\"combobox\",\"value\":[\"Отсутствует освещение\",\"Завышение обратной сетевой воды и отклонения от договорного расхода\",\"Неукомплектован КИП\",\"РТ в нерабочем состоянии\",\"Отсутствует изоляция\",\"Затоплен подвал\",\"Нет доступа к узлу управления\",\"Нет врезок под термометр\",\"Нет врезок под манометр\",\"нет\",\"Нет освещения, нет врезок под КИП, нет изоляции\",\"запорная арматура ОАО ТЭ (в ТК)\",\"Завышение обратной сетевой воды\",\"Завышение температуры обратной сетевой воды\"]},{\"results_id\":966134,\"results_guid\":\"3390260b-0a25-4e22-b7e4-814d8c53727e\",\"id_question\":1107,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"729e7fe2-230c-4f4a-94c8-aaa6c9150c8c\",\"sequence_question\":50,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"Приложенное фото\",\"type\":\"photo\"}]"
        checkupFragment.checkup= Checkup(idOrder=44731, numberOrder = "тест4",orderGuid = "95c109e2-2456-4141-9fa1-604ba0bb9e20",typeOrder = "Шум", text = Gson().fromJson(fakeCheckup,JsonArray::class.java))


        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)


        shadowOf(getMainLooper()).idle()

        println("updated")
        Assert.assertNotNull(checkupFragment.uiCreator)

    }

    @Test
    fun testClickCheckup_parentFragment_not_attach() {
        val positionGI=3

        val fakeCheckup="[{\"results_id\":966045,\"results_guid\":\"c0619459-bcdc-4c7c-9886-2cef770b72f2\",\"id_question\":112,\"node\":null,\"regulation\":null,\"number_object\":null,\"status\":\"В работе\",\"question_guid\":\"b38c0ca8-dbbb-448b-a945-405b6f78cdc0\",\"sequence_question\":1,\"replication_nodes\":false,\"replicating_archival_records\":false,\"group_checklist\":null,\"replicated_on\":null,\"node_itp\":null,\"archival_records\":null,\"question\":\"СПЭС\",\"type\":\"combobox\",\"value\":[\"Да\",\"Нет\"]}]"
        checkupFragment.checkup= Checkup(numberOrder = "А-001",orderGuid = "fakeGuid",text = Gson().fromJson(fakeCheckup,JsonArray::class.java))


        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // Отсоединяем фрагмент от активити
        activityController.get()
            .supportFragmentManager
            .beginTransaction()
            .remove(checkupFragment)
            .commitNow()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        println("updated")
        val viewToast=ShadowToast.getLatestToast().view
        val textToast=viewToast.findViewById<TextView>(R.id.textToast)

        Assert.assertTrue(textToast.text==activityController.get().applicationContext.getString(R.string.error_unable_upload_checklist))

    }

    @Test
    fun test_with_currentOrder_state_OPEN() {
        val positionGI=3

        checkupFragment.currentOrder.status=STATE_OPEN


        val recycler=checkupFragment.view!!.findViewById<RecyclerView>(R.id.steps_recycler_view)
        // Обновляем RV. Решение проблемы robolectric recyclerView
        // подробнее тут https://stackoverflow.com/questions/27052866/android-robolectric-click-recyclerview-item
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        // клик по Общим характеристикам, RV обновился, произошло notifyItemChanged(position)
        recycler.findViewHolderForAdapterPosition(positionGI)?.itemView?.performClick()

        // нужно опять обновить RV, без этого не попадаем в onBindViewHolder
        recycler.measure(0,0)
        recycler.layout(0,0,100,1000)

        shadowOf(getMainLooper()).idle()

        val isShowedCustomToast=ShadowToast.showedCustomToast(activityController.get().applicationContext.getString(R.string.checklist_is_blocked),R.id.textToast)

        Assert.assertTrue(isShowedCustomToast)

    }

}