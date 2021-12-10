package ru.bingosoft.teploInspector.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.Dialog.DIALOG_DATE
import ru.bingosoft.teploInspector.util.Const.Dialog.DIALOG_TIME
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.MAX_COUNT_REPLICATE_GROUP
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.NAME_GROUP_REPLICATE
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.NEW_NAME_GROUP_REPLICATE
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.NUMBER_GROUPS_FIELD_MARK
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.NUMBER_GROUPS_QUESTION
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.OTHER
import ru.bingosoft.teploInspector.util.Const.SpecialTypesOrders.ROUTINE_MAINTENANCE
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
//private val rootView: View, val checkup: Checkup, private val photoHelper: PhotoHelper, private val checkupPresenter: CheckupPresenter
@Suppress("unused")
class UICreator(private val parentFragment: CheckupFragment, val checkup: Checkup) {
    lateinit var controlList: MutableList<Models.TemplateControl>
    var listSubtypeView: MutableList<LinearLayout> = mutableListOf()

    private var enabledControls: MutableList<View> = mutableListOf()
    private val dateAndTime: Calendar =Calendar.getInstance()

    private val photoHelper=parentFragment.photoHelper
    private lateinit var rootView:View
    private var enabled: Boolean=true
    private var llGroupNodes: LinearLayout?=null
    private var listllGroupNodes: MutableList<LinearLayout> = mutableListOf()
    private var listllNode: List<LinearLayout> = listOf()
    private var listllArchHour: List<LinearLayout> = listOf()
    private var listllArchDaily: List<LinearLayout> = listOf()
    private var listllGroupOther: MutableList<LinearLayout> = mutableListOf()
    private var listSubtypeName: MutableList<String> = mutableListOf()
    private var mapllNode: MutableMap<String, List<LinearLayout>> = mutableMapOf()


    val otherUtil=parentFragment.otherUtil


    // заглушка от долгого нажатия, иначе ошибка Fatal Exception: java.lang.NullPointerException
    //Attempt to invoke virtual method 'int android.widget.Editor$SelectionModifierCursorController.getMinTouchOffset()' on a null object reference
    //android.widget.Editor.touchPositionIsInSelection
    // подробнее тут
    //https://stackoverflow.com/questions/53435237/nullpointerexception-int-android-widget-editorselectionmodifiercursorcontrolle
    private val emptyLongClickListener= View.OnLongClickListener {
        Timber.d("LongClickListener_${it}")
        otherUtil.writeToFile("Logger_LongClickListener_${it}")
        return@OnLongClickListener true
    }

    fun create(rootView: View): MutableList<Models.TemplateControl> {
        this.rootView=rootView
        enabled= !(parentFragment.currentOrder.status==parentFragment.getString(R.string.status_IN_WAY)||
                parentFragment.currentOrder.status==parentFragment.getString(R.string.status_OPEN))

        // Возможно чеклист был ранее сохранен, тогда берем сохраненный и восстанавливаем его
        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type

        controlList = if (checkup.textResult != null) {
            Gson().fromJson(checkup.textResult, listType)
        } else {
            Timber.d("checkup.text=${checkup.text}")
            Gson().fromJson(checkup.text, listType)
        }

        checkSubtypesOrder() // Проверим возможно есть подтипы у заявки

        controlList.forEach controls@ {
            when (it.type) {
                "combobox", "date", "time", "textinput", "numeric", "photo" -> {
                    Timber.d("генерим ${it.type}")

                    createWithSubCheckup(it)

                    //createWithGrouping(it)
                    return@controls
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }

        }

        return controlList

    }

    private fun checkSubtypesOrder() {
        if (parentFragment.currentOrder.typeOrder==ROUTINE_MAINTENANCE) {
            val subtypes= parentFragment.currentOrder.subtypes
            Timber.d("subtypes_$subtypes")
            if (!subtypes.isNullOrEmpty()) {
                val result= subtypes.removePrefix("{").removeSuffix("}")
                val listSubtypeId= result.split(",").toList()

                listSubtypeId.forEach{subtype ->
                    controlList.firstOrNull{it.subtype==subtype}?.subtype_name?.let {
                        listSubtypeName.add(
                            it
                        )
                    }
                }
            }
        }
    }

    private fun createWithSubCheckup(control: Models.TemplateControl) {
        Timber.d("createWithSubCheckup")
        if (!listSubtypeName.isNullOrEmpty() && !control.subtype_name.isNullOrEmpty() && listSubtypeName.contains(control.subtype_name)) {
            val tempRootView=this.rootView
            this.rootView=createGroup(control.subtype_name!!,rootView as LinearLayout)
            Timber.d("rootView_${this.rootView}")
            createWithGrouping(control)
            this.rootView=tempRootView
        } else {
            createWithGrouping(control)
        }
    }

    fun showQuestionCount(layout: LinearLayout) {
        Timber.d("showQuestionCount")
        val llGroup=layout.findViewById<LinearLayout>(R.id.container)
        Timber.d("llGroup_${llGroup.tag}")
        val groupName=llGroup.tag
        val tvCountQuestion=layout.findViewById<TextView>(R.id.countQuestion)
        if (tvCountQuestion!=null && parentFragment.currentOrder.typeOrder==ROUTINE_MAINTENANCE) {
            val allQuestion=controlList.filter { it.subtype_name==groupName }.size
            val answeredQuestion=controlList.filter { it.subtype_name==groupName && it.answered }.size
            tvCountQuestion.text=parentFragment.getString(R.string.question_count,allQuestion,answeredQuestion)
            tvCountQuestion.visibility=View.VISIBLE
        }
    }


    fun saveUI() {
        Timber.d("saveUI")
        val ll=rootView.findViewById<LinearLayout>(R.id.llMain)
        parentFragment.llMainUi.addAll(ll.children)
    }

    private fun createWithGrouping(it: Models.TemplateControl) {
        Timber.d("createWithGrouping")
        if (it.replication_nodes==true) {
            // Создаем группу Узлы
            llGroupNodes=createGroupNodes("Узлы",it.subtype_name)
            // Создаем Узлы в группе
            listllNode=createNodes(llGroupNodes!!)

            if (it.group_checklist==null) {
                // Тиражируем по узлам
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node - 1]
                        when (it.type) {
                            "combobox" -> createCombobox(it, llCurrentNode)
                            "textinput" -> createTextInput(it, llCurrentNode)
                            "numeric" -> createNumeric(it, llCurrentNode)
                            "date" -> createDate(it, llCurrentNode)
                            "time" -> createTime(it, llCurrentNode)
                            "photo" -> createPhoto(it, llCurrentNode)
                        }
                    }
                }
            } else {
                Timber.d("it.group_checklist=${it.group_checklist}")
                // Есть еще и группа
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node - 1]
                        val llGroup=createGroup(
                            it.group_checklist!!,
                            llCurrentNode,
                            it.node.toString()
                        )
                        when (it.type) {
                            "combobox" -> createCombobox(it, llGroup)
                            "textinput" -> createTextInput(it, llGroup)
                            "numeric" -> createNumeric(it, llGroup)
                            "date" -> createDate(it, llGroup)
                            "time" -> createTime(it, llGroup)
                            "photo" -> createPhoto(it, llGroup)
                        }
                    }
                }
            }

        } else {
            if (it.replicating_archival_records==true) {
                Timber.d("Тиражирование_Арх_Зап")
                // Создаем группу Архивные записи
                llGroupNodes=createGroupNodes("Архивные записи",it.subtype_name)
                // Создаем группы Часовые, Суточные по 5 штук
                /*if (listllNode.isEmpty()) {
                    listllArchHour=createArchGroup(llGroupNodes!!,"Часовые")
                    listllArchDaily=createArchGroup(llGroupNodes!!,"Суточные")
                }*/

                if (it.group_checklist==null) {
                    // Тиражируем по узлам
                    /*if (it.archival_records!=null) {
                        if (listllArchHour.isNotEmpty()) {
                            val llCurrentNode=listllArchHour[it.archival_records-1]
                            when (it.type) {
                                "combobox" -> createCombobox(it,llCurrentNode)
                                "textinput" -> createTextInput(it,llCurrentNode)
                                "numeric" -> createNumeric(it,llCurrentNode)
                                "date" -> createDate(it,llCurrentNode)
                                "time" -> createTime(it,llCurrentNode)
                                "photo" -> createPhoto(it,llCurrentNode)
                            }
                        }
                    }*/
                } else {
                    Timber.d("it.group_checklist=${it.group_checklist}")
                    if (listllArchHour.isEmpty() && it.group_checklist=="Часовые") {
                        listllArchHour=createArchGroup(llGroupNodes!!, "Часовые")
                    }
                    if (listllArchDaily.isEmpty() && it.group_checklist=="Суточные") {
                        listllArchDaily=createArchGroup(llGroupNodes!!, "Суточные")
                    }
                    // Есть еще и группа
                    if (it.archival_records!=null) {
                        if (listllArchHour.isNotEmpty()) {
                            val llCurrentNode=listllArchHour[it.archival_records - 1]
                            when (it.type) {
                                "combobox" -> createCombobox(it, llCurrentNode)
                                "textinput" -> createTextInput(it, llCurrentNode)
                                "numeric" -> createNumeric(it, llCurrentNode)
                                "date" -> createDate(it, llCurrentNode)
                                "time" -> createTime(it, llCurrentNode)
                                "photo" -> createPhoto(it, llCurrentNode)
                            }
                        }
                        if (listllArchDaily.isNotEmpty()) {
                            val llCurrentNode=listllArchDaily[it.archival_records - 1]
                            when (it.type) {
                                "combobox" -> createCombobox(it, llCurrentNode)
                                "textinput" -> createTextInput(it, llCurrentNode)
                                "numeric" -> createNumeric(it, llCurrentNode)
                                "date" -> createDate(it, llCurrentNode)
                                "time" -> createTime(it, llCurrentNode)
                                "photo" -> createPhoto(it, llCurrentNode)
                            }
                        }
                    }
                }
            } else {
                // Тиражирования по узлам и по архивным записям нет
                if (it.group_checklist!=null) {
                    //..., но есть Группа
                    // Группа может быть многоуровневой
                    var llGroup=rootView as LinearLayout //Инициализируем корневым layout
                    if (it.group_checklist?.contains("#") == true) {
                        var parentGroup:LinearLayout?=null
                        val groupList= it.group_checklist!!.split("#")
                        Timber.d("groupList_$groupList")
                        groupList.forEach { groupName ->
                            llGroup=if (parentGroup==null) {
                                createGroup(groupName, rootView as LinearLayout)
                            } else {
                                createGroup(groupName, parentGroup!!, parentGroup!!.tag.toString())
                            }
                            parentGroup=llGroup

                            // Если группа тиражная покажем кнопки добавления/удаления
                            if (groupName==NEW_NAME_GROUP_REPLICATE) {
                                val templateStep=llGroup.parent as LinearLayout
                                val ivAddGroup=templateStep.findViewById<ImageView>(R.id.ivAddGroup)
                                ivAddGroup.visibility=View.VISIBLE
                                //addGroupClickListener.templateStep=templateStep
                                ivAddGroup.tag=templateStep
                                ivAddGroup.setOnClickListener(addGroupClickListener)
                            } else {

                                val ivDeleteGroup=(llGroup.parent as LinearLayout).findViewById<ImageView>(R.id.ivDeleteGroup)
                                ivDeleteGroup.visibility=View.VISIBLE

                                ivDeleteGroup.setOnClickListener(deleteGroupClickListener)

                            }

                        }
                    } else {
                        llGroup=createGroup(it.group_checklist!!, rootView as LinearLayout)
                    }


                    //var llGroup=createGroup(it.group_checklist!!, rootView as LinearLayout)
                    // Проверим, возможно эта группа тиражируется по узлам (как ИТП(общий ввод))
                    Timber.d("_it=$it")
                    if (it.replicated_on!=null) {
                        Timber.d("it.replicated_on=${it.replicated_on}")
                        // Тиражирование по ИТП
                        if (it.node_itp!=null) {
                            Timber.d("it.node_itp=${it.node_itp}")
                            llGroup=createGroup(
                                "Узел ${it.group_checklist} ${it.node_itp}",
                                llGroup,
                                it.node_itp
                            )
                        }
                        // Тиражирование по Номеру объекта
                        if (it.number_object!=null) {
                            Timber.d("it.number_object=${it.number_object}")
                            llGroup=createGroup(
                                "Номер объекта ${it.number_object}",
                                llGroup
                            )
                        }
                    }


                    when (it.type) {
                        "combobox" -> createCombobox(it, llGroup)
                        "textinput" -> createTextInput(it, llGroup)
                        "numeric" -> createNumeric(it, llGroup)
                        "date" -> createDate(it, llGroup)
                        "time" -> createTime(it, llGroup)
                        "photo" -> createPhoto(it, llGroup)
                    }

                } else {
                    // Ничего нет, ни тиражирования, ни группы
                    when (it.type) {
                        "combobox" -> createCombobox(it, rootView as LinearLayout)
                        "textinput" -> createTextInput(it, rootView as LinearLayout)
                        "numeric" -> createNumeric(it, rootView as LinearLayout)
                        "date" -> createDate(it, rootView as LinearLayout)
                        "time" -> createTime(it, rootView as LinearLayout)
                        "photo" -> createPhoto(it, rootView as LinearLayout)
                    }
                }
            }


        }
    }

    private fun createCombobox(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_material_spinner, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        //templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it, templateStep)

        val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

        doAssociateParent(templateStep, parent)
        it.parentView=parent // Сохраним данные о родительской вьюхе

        //#Перенос_строк #Combobox
        // Проставим переносы в строку. Длина строки 40 символов
        val listLinesWithBreak= mutableListOf<String>()
        it.value.forEach {
            var item=it
            var newitem=""
            while (item.length > 40) {
                val spacepos = item.lastIndexOf(" ", 40) // ищем первый пробел начиная с 40 символа к началу строки
                val strtemp = item.substring(0, spacepos)
                newitem = newitem + strtemp+"\n"

                // Удалим из item подстроку
                item=item.replace(strtemp, "")
            }
            newitem += item
            listLinesWithBreak.add(newitem)
        }

        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item,
            listLinesWithBreak //it.value
        )
        // Заполним spinner
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread{
                materialSpinner.setAdapter(spinnerArrayAdapter)

                // Если шаг чеклиста был ранее сохранен восстановим значение
                if (!it.resvalue.isNullOrEmpty()){
                    materialSpinner.setText(it.resvalue)
                }
            }
        }


        materialSpinner.isEnabled = enabled
        if (enabled) {
            materialSpinner.dropDownHeight = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            materialSpinner.dropDownHeight = 0
        }

        // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
        materialSpinner.addTextChangedListener(TextWatcherHelper(it, templateStep))
        enabledControls.add(materialSpinner)

    }

    private fun createTextInput(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_textinput, rootView.parent as ViewGroup?, false
        ) as LinearLayout


        templateStep.id=it.results_id
        templateStep.findViewById<TextView>(R.id.question).text=it.question

        //Покажем норматив
        val tvRegulation=templateStep.findViewById<TextView>(R.id.tvRegulation)
        if (!it.regulation.isNullOrEmpty()) {
            tvRegulation.visibility=View.VISIBLE
            tvRegulation.text = parentFragment.getString(R.string.regulation_text,it.question,it.regulation)
        }else {
            tvRegulation.visibility=View.GONE
        }

        setQuestionColor(it, templateStep)


        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled


        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        doAssociateParent(templateStep, parent)
        it.parentView=parent // Сохраним данные о родительской вьюхе

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            if (parentFragment.isAdded) {
                parentFragment.requireActivity().runOnUiThread{
                    textInputEditText.setText(it.resvalue)
                }
            }


        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))
        textInputEditText.setOnLongClickListener(emptyLongClickListener)

        enabledControls.add(textInputLayout)
    }

    private fun createNumeric(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep = LayoutInflater.from(rootView.context).inflate(
            R.layout.template_numeric, rootView.parent as ViewGroup?, false
        ) as LinearLayout


        templateStep.id = it.results_id
        templateStep.findViewById<TextView>(R.id.question).text = it.question

        //Покажем норматив
        val tvRegulation=templateStep.findViewById<TextView>(R.id.tvRegulation)
        if (!it.regulation.isNullOrEmpty()) {
            Timber.d("it_regulation=${it.regulation}")
            tvRegulation.visibility=View.VISIBLE
            tvRegulation.text = parentFragment.getString(R.string.regulation_text,it.question,it.regulation)
        }else {
            tvRegulation.visibility=View.GONE
        }

        setQuestionColor(it, templateStep)

        val textInputLayout = templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint = it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText = templateStep.findViewById<TextInputEditText>(R.id.tiet)

        // Если тип заявки Другое и поле NUMBER_GROUPS_QUESTION, пометим его
        if (parentFragment.currentOrder.typeOrder==OTHER) {
            Timber.d("ДругоеQ_${it.question}")
            if (it.question==NUMBER_GROUPS_QUESTION) {
                Timber.d("Добавили_пометку")
                textInputEditText.tag=NUMBER_GROUPS_FIELD_MARK
            }
        }

        doAssociateParent(templateStep, parent)
        it.parentView=parent // Сохраним данные о родительской вьюхе

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()) {
            if (parentFragment.isAdded) {
                parentFragment.requireActivity().runOnUiThread{
                    textInputEditText.setText(it.resvalue)
                }
            }


        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(
            TextWatcherHelper(
                it,
                templateStep
            )
        )
        textInputEditText.setOnLongClickListener(emptyLongClickListener)

        enabledControls.add(textInputLayout)
    }

    private fun createDate(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_date, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        templateStep.id=it.results_id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it, templateStep)

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_DATE, textInputEditText)
        }

        doAssociateParent(templateStep, parent)
        it.parentView=parent // Сохраним данные о родительской вьюхе

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            if (parentFragment.isAdded) {
                parentFragment.requireActivity().runOnUiThread{
                    textInputEditText.setText(it.resvalue)
                }
            }


        }

        //#Проверка_формата_даты
        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))
        textInputEditText.setOnLongClickListener(emptyLongClickListener)
        textInputEditText.setOnFocusChangeListener { v, hasFocus ->
            Timber.d("фокус_даты=$hasFocus")
            val strDate=(v as TextInputEditText).text.toString()
            if (!hasFocus && strDate.isNotEmpty()) {
                Timber.d("strDate=$strDate")
                if (!strDate.matches("[0-3]\\d.[01]\\d.\\d{4}".toRegex())) {
                    Timber.d("Ошибка_даты")
                    textInputEditText.error=parentFragment.getString(R.string.error_date)
                    parentFragment.errorControls.add(textInputEditText)
                } else {
                    val df=SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
                    df.isLenient=false
                    try {
                        df.parse(strDate)
                        if (parentFragment.errorControls.contains(textInputEditText)) {
                            parentFragment.errorControls.remove(textInputEditText)
                        }
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        textInputEditText.error=parentFragment.getString(R.string.error_date)
                        parentFragment.errorControls.add(textInputEditText)
                    }
                }
            }
        }

        enabledControls.add(textInputLayout)
    }

    private fun createTime(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_time, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        templateStep.id=it.results_id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it, templateStep)

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_TIME, textInputEditText)
        }

        doAssociateParent(templateStep, parent)
        it.parentView=parent // Сохраним данные о родительской вьюхе

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            if (parentFragment.isAdded) {
                parentFragment.requireActivity().runOnUiThread{
                    textInputEditText.setText(it.resvalue)
                }
            }
        }

        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))
        textInputEditText.setOnLongClickListener(emptyLongClickListener)

        enabledControls.add(textInputLayout)
    }


    private fun createPhoto(templateControl: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_photo2, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        templateStep.id=templateControl.results_id
        templateStep.findViewById<TextView>(R.id.question).text=templateControl.question
        setQuestionColor(templateControl, templateStep)

        templateStep.tag=templateControl

        doAssociateParent(templateStep, parent)
        templateControl.parentView=parent // Сохраним данные о родительской вьюхе

        // Обработчик для кнопки "Добавить фото"
        val btnPhoto=templateStep.findViewById<MaterialButton>(R.id.btnPhoto)
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread{
                btnPhoto.isEnabled = enabled
            }
        }

        val stepCheckup=templateControl
        btnPhoto.tag=templateStep
        btnPhoto.setOnClickListener{
            Timber.d("Добавляем фото")
            // Сбрасываем признак Checked
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            if (parentFragment.isAdded) {
                (parentFragment.requireActivity() as MainActivity).photoStep=stepCheckup // Сохраним id контрола для которого делаем фото
                (parentFragment.requireActivity() as MainActivity).photoDir="${curOrder.guid}/${stepCheckup.results_guid}" // Сохраним id контрола для которого делаем фото
            }

            photoHelper.createPhoto(curOrder.guid, stepCheckup)
        }

        val btnDeletePhoto = templateStep.findViewById<MaterialButton>(R.id.btnDeletePhoto)
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread{
                btnDeletePhoto.isEnabled = enabled
            }
        }

        btnDeletePhoto.tag=templateStep
        btnDeletePhoto.setOnClickListener {
            Timber.d("Удалим фото")
            val ts=it.tag
            val tc=((ts as View).tag as Models.TemplateControl)

            val pager = templateStep.findViewById(R.id.pager) as ViewPager

            // Получим текущую страницу
            val indexPhoto=pager.currentItem
            Timber.d("pager.adapter.count=${pager.adapter?.count}")
            if (pager.adapter?.count!! >0) {
                // Получим список фоток из папки
                val imagesPhoto: List<String> = if (!tc.resvalue.isNullOrEmpty()) {
                    otherUtil.getFilesFromDir("${DCIM_DIR}/PhotoForApp/${tc.resvalue}")
                } else {
                    val curOrder=(parentFragment.activity as MainActivity).currentOrder
                    val photoDirectory="${curOrder.guid}/${stepCheckup.results_guid}"
                    otherUtil.getFilesFromDir("${DCIM_DIR}/PhotoForApp/$photoDirectory")
                }
                val photoForDelete=imagesPhoto[indexPhoto]
                Timber.d("photoForDelete=$photoForDelete")
                if (photoHelper.deletePhoto(photoForDelete)) {
                    val imagesNew= mutableListOf<String>()
                    imagesNew.addAll(imagesPhoto)
                    imagesNew.removeAt(indexPhoto)

                    if (parentFragment.isAdded) {
                        parentFragment.requireActivity().runOnUiThread{
                            parentFragment.refreshPhotoViewer(templateStep, imagesNew, rootView.context)
                        }
                    }



                    // Проверим папку, может она пуста
                    val curOrder=(parentFragment.activity as MainActivity).currentOrder
                    if (photoHelper.checkDirAndEmpty("$DCIM_DIR/PhotoForApp/${curOrder.guid}/${stepCheckup.results_guid}")) {
                        Timber.d("Папка есть, она не пуста")
                    } else {
                        Timber.d("Удаляем_папку")
                        // Удалим папку, очистим photoStep?.resvalue
                        val dir=File("$DCIM_DIR/PhotoForApp/${curOrder.guid}/${stepCheckup.results_guid}")
                        dir.delete()
                        tc.answered=false
                        tc.resvalue=null
                        parentFragment.checkupPresenter.saveCheckup(this, send = false)
                        (parentFragment.activity as MainActivity).photoDir=""
                    }
                }
            }
        }

        val images: List<String> = if (!templateControl.resvalue.isNullOrEmpty()) {
            Timber.d("Фото_${templateControl.resvalue}")
            // Обновим список с фото
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            val stDir = "${DCIM_DIR}/PhotoForApp/${curOrder.guid}/${stepCheckup.results_guid}"
            otherUtil.getFilesFromDir(stDir)

        } else {
            listOf()
        }

        Timber.d("images=$images")

        // Проверим, если resvalue пусто, а папка с фото есть и содержит старые фото, тогда удалим папку
        Timber.d("it.resvalue=${templateControl.resvalue}")
        if (templateControl.resvalue.isNullOrEmpty()) {
            Timber.d("it.resvalue.isNullOrEmpty()")
            if (parentFragment.activity!=null) {
                val curOrder=(parentFragment.activity as MainActivity).currentOrder
                val stDir = "${DCIM_DIR}/PhotoForApp/${curOrder.guid}/${stepCheckup.results_guid}"

                val listPhoto=otherUtil.getFilesFromDir(stDir)
                Timber.d("listPhoto=${listPhoto}")
                if (File(stDir).exists() && listPhoto.isNotEmpty()) {
                    Timber.d("папка_есть")
                    otherUtil.deleteDir(stDir)
                }
            }

        }

        val leftBtn = templateStep.findViewById(R.id.left_nav) as ImageButton
        val rightBtn = templateStep.findViewById(R.id.right_nav) as ImageButton

        // Обновим вьювер с фотками
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread{
                parentFragment.refreshPhotoViewer(templateStep, images, rootView.context)
            }
        }


        val pager = templateStep.findViewById(R.id.pager) as ViewPager
        val myList = templateStep.findViewById(R.id.recyclerviewFrag) as RecyclerView

        leftBtn.setOnClickListener {
            var tab = pager.currentItem
            if (tab > 0) {
                tab--
                pager.currentItem = tab
            } else if (tab == 0) {
                pager.currentItem = tab
            }
        }

        rightBtn.setOnClickListener {
            var tab = pager.currentItem
            tab++
            pager.currentItem = tab
        }

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                myList.scrollToPosition(position)
            }

        })

        enabledControls.add(btnPhoto)
    }



    @SuppressLint("UseRequireInsteadOfGet")
    private fun createNodes(llContainer: LinearLayout): List<LinearLayout> {
        Timber.d("генерим узлы")
        return mapllNode.getOrElse(llContainer.tag.toString(),{
            val listNodesView= mutableListOf<LinearLayout>()
            if (parentFragment.currentOrder.countNode!=null) {
                for (i in 1..parentFragment.currentOrder.countNode!!) {
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_group, rootView.parent as ViewGroup?, false
                    ) as LinearLayout

                    Timber.d("Узел $i")
                    templateStep.findViewById<TextView>(R.id.question).text=parentFragment.requireContext().getString(
                        R.string.name_node,
                        i
                    )

                    val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
                    val llNode=templateStep.findViewById<LinearLayout>(R.id.container)
                    val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

                    clTitle.setOnClickListener {
                        if (llNode.visibility==View.VISIBLE) {
                            llNode.visibility=View.GONE
                            ivExpand.setImageDrawable(
                                ContextCompat.getDrawable(
                                    rootView.context,
                                    R.drawable.arrow_up
                                )
                            )
                        } else {
                            llNode.visibility=View.VISIBLE
                            ivExpand.setImageDrawable(
                                ContextCompat.getDrawable(
                                    rootView.context,
                                    R.drawable.arrow_down
                                )
                            )
                        }
                    }

                    listNodesView.add(llNode)

                    doAssociateParent(templateStep, llContainer)
                }
                mapllNode[llContainer.tag.toString()] = listNodesView
            } else {
                Timber.d("parentFragment.currentOrder.countNode==null")
                parentFragment.requireActivity().runOnUiThread {
                    parentFragment.toaster.showErrorToast(R.string.not_count_node)
                }

            }

            return listNodesView
        })

    }

    private fun createArchGroup(llContainer: LinearLayout, name: String): List<LinearLayout> {
        Timber.d("генерим группы $name")
        if (parentFragment.isAdded) {
            // Сообщение отправляем не в UI потоке, requireActivity не используем т.к.isAdded=false
            Handler(Looper.getMainLooper()).post{
                parentFragment.toaster.showErrorToast(R.string.checkup_fragment_not_attached)
            }
            return listOf()
        }

        val listNodesView= mutableListOf<LinearLayout>()
        for (i in 1..5) {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, rootView.parent as ViewGroup?, false
            ) as LinearLayout

            Timber.d("$name $i")
            templateStep.findViewById<TextView>(R.id.question).text=parentFragment.requireContext().getString(
                R.string.question,
                name,
                i
            )

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llNode=templateStep.findViewById<LinearLayout>(R.id.container)
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llNode.visibility==View.VISIBLE) {
                    llNode.visibility=View.GONE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_up
                        )
                    )
                } else {
                    llNode.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_down
                        )
                    )
                }
            }

            listNodesView.add(llNode)

            doAssociateParent(templateStep, llContainer)
        }


        return listNodesView

    }

    private fun createGroupNodes(name: String, subtype: String?="") :LinearLayout {
        Timber.d("генерим группу для Узлов")

        // Проверим, возможно группа уже создана
        val ll=listllGroupNodes.filter { it.tag=="$name$subtype" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, rootView.parent as ViewGroup?, false
            ) as LinearLayout

            templateStep.findViewById<TextView>(R.id.question).text=name//"Узлы"

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)
            llGroup.tag="$name$subtype" // Сохраним имя группы

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_up
                        )
                    )
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_down
                        )
                    )
                }
            }

            if (rootView.findViewById<View>(R.id.llMain)!=null) {
                doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))
            } else {
                doAssociateParent(templateStep, rootView)
            }


            Timber.d("llGroup=$llGroup")
            listllGroupNodes.add(llGroup)

            return llGroup
        }

    }

    private fun createGroup(name: String, parent: LinearLayout, node: String = ""): LinearLayout {
        Timber.d("генерим_группу_$name$node")

        // Проверим, возможно группа уже создана
        val ll=listllGroupOther.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, /*rootView.parent as ViewGroup?*/parent as ViewGroup?, false
            ) as LinearLayout

            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            llGroup.tag="$name$node" // Сохраним имя группы
            Timber.d("tag_${llGroup.tag}")
            if (parentFragment.currentOrder.typeOrder==ROUTINE_MAINTENANCE) {
                showQuestionCount(templateStep)
                listSubtypeView.add(templateStep)
            }

            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_up
                        )
                    )
                } else {

                    // Группа Вопросы, должна быть растиражирована
                    // Чеклист у заявки Другое может ломаться, если в пустую БД, приходит заявки с ранее заполненным чекоситом
                    // Такое может быть если пользователь сохранил чеклист, не перевел заявку в Выполнено, очистил БД и повторил синхронизацию
                    // Группа тиражируется неправильно, т.к. с сервера прилетает уже измененный чеклист, а не дефолтный
                    // Возможные способы исправления: 1) Добавить жестко в код шаблон чеклиста Другое
                    // 2) Либо с сервера всегда присылать чистый шаблон.
                    // Чистые поля как-то пометить в реестре и при формировании чеклиста отбирать только эти поля
                    if (parentFragment.currentOrder.typeOrder== OTHER && name==NAME_GROUP_REPLICATE) {
                        if (rootView.findViewWithTag<TextView>(NUMBER_GROUPS_FIELD_MARK)!=null) {
                            val numberGroups=(rootView.findViewWithTag<TextView>(NUMBER_GROUPS_FIELD_MARK).text.toString()).toIntOrNull()
                            if (numberGroups==null || numberGroups<=0 || numberGroups>MAX_COUNT_REPLICATE_GROUP ) {
                                parentFragment.toaster.showErrorToast(R.string.other_type_order_number_group_empty)
                                return@setOnClickListener
                            } else {

                                llGroup.removeAllViewsInLayout()

                                replicateGroup(templateStep,numberGroups)
                            }
                        } else {
                            parentFragment.toaster.showErrorToast(R.string.other_type_order_number_group_empty)
                            return@setOnClickListener
                        }

                    } else {

                        llGroup.visibility=View.VISIBLE
                        ivExpand.setImageDrawable(
                            ContextCompat.getDrawable(
                                rootView.context,
                                R.drawable.arrow_down
                            )
                        )
                    }



                }
            }

            doAssociateParent(templateStep, parent)


            listllGroupOther.add(llGroup)

            return llGroup
        }

    }


    private fun replicateGroup(templateStep: LinearLayout, numberGroups: Int) {
        val layoutGroup=templateStep.findViewById<LinearLayout>(R.id.container)
        Timber.d(" layoutGroup_childCount=${layoutGroup.childCount}")
        if (layoutGroup.childCount!=MAX_COUNT_REPLICATE_GROUP) {
            val checkup=createCheckupForReplicatedGroup(numberGroups, layoutGroup.childCount)
            addReplicatedCheckupInUI(checkup, templateStep)
        } else {
            parentFragment.toaster.showErrorToast(R.string.max_number_group_limit)
        }

    }

    private fun createCheckupForReplicatedGroup(numberGroups: Int, numberGroupsExists: Int=0) : Checkup {
        // Получим чеклист для группы вопросов
        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type
        val controlList: List<Models.TemplateControl> =Gson().fromJson(checkup.text, listType)
        val controlsInGroup=controlList.filter { it.group_checklist==NAME_GROUP_REPLICATE }

        val newControlsInGroup= mutableListOf<Models.TemplateControl>()
        repeat(numberGroups){index ->
            val list=controlsInGroup.map { it.copy() }
            list.forEach{
                // Для первой группы не сбрасываем results_id
                /*if (index>0) {
                    it.results_id=0
                }*/
                it.results_id=0
                it.group_checklist="Группа ${numberGroupsExists+index+1}"
                it.replicated=true
            }
            newControlsInGroup.addAll(list)
        }

        return Checkup(
            numberOrder = checkup.numberOrder,
            orderGuid = checkup.orderGuid,
            text=Gson().toJsonTree(newControlsInGroup) as JsonArray
        )
    }

    @SuppressLint("SetTextI18n")
    private fun addReplicatedCheckupInUI(checkup: Checkup, templateStep: LinearLayout) {
        Timber.d("addReplicatedCheckupInUI")
        val layoutGroup=templateStep.findViewById<LinearLayout>(R.id.container)
        val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)

        templateStep.findViewById<TextView>(R.id.question).text=NEW_NAME_GROUP_REPLICATE
        val ivAddGroup=templateStep.findViewById<ImageView>(R.id.ivAddGroup)
        ivAddGroup.visibility=View.VISIBLE
        //addGroupClickListener.templateStep=templateStep
        ivAddGroup.tag=templateStep
        ivAddGroup.setOnClickListener(addGroupClickListener)

        val pbStepLoad=templateStep.findViewById<ProgressBar>(R.id.pbStepLoad)
        pbStepLoad.visibility=View.VISIBLE
        val r= Runnable {
            val uiCreator=UICreator(parentFragment, checkup)
            val replicatedControlList=uiCreator.create(layoutGroup)

            updateMainControlList(replicatedControlList)

            //Сменим кол-во вопросов в группе
            val newNumberQuestion=this.controlList.size
            parentFragment.currentOrder.questionCount=newNumberQuestion

            if (parentFragment.isAdded) {
                parentFragment.requireActivity().runOnUiThread {
                    val tvQuestionCount=parentFragment.view?.findViewWithTag<TextView>("countQuestionChecklist")
                    if (tvQuestionCount!=null) {
                        tvQuestionCount.text="${parentFragment.currentOrder.questionCount}/${parentFragment.currentOrder.answeredCount}"
                    }

                    pbStepLoad.visibility=View.INVISIBLE
                    layoutGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_down
                        )
                    )

                    //Для всех потомков Группы добавим кнопки для удаления
                    Timber.d("layoutGroup.children=${layoutGroup.children.count()}")
                    layoutGroup.children.forEach {
                        val ivDeleteGroup=it.findViewById<ImageView>(R.id.ivDeleteGroup)
                        ivDeleteGroup.visibility=View.VISIBLE
                        //deleteGroupClickListener.templateStep=templateStep
                        Timber.d("setOnClickListener_deleteGroupClickListener")
                        ivDeleteGroup.setOnClickListener(deleteGroupClickListener)
                    }

                }
            }

        }
        val t=Thread(r)
        t.start()
    }

    private val addGroupClickListener= View.OnClickListener { v ->

        //lateinit var templateStep: LinearLayout
        Timber.d("Добавим_группу")
        val templateStep= v?.tag as LinearLayout
        replicateGroup(templateStep,1)

        // Сменим значение атрибута Кол-во групп полей
        val tvNumberGroup=rootView.findViewWithTag<TextView>(NUMBER_GROUPS_FIELD_MARK)
        val numberGroups=(tvNumberGroup.text.toString()).toIntOrNull()
        if (numberGroups != null) {
            tvNumberGroup.text=(numberGroups+1).toString()
        }
    }

    private val deleteGroupClickListener= View.OnClickListener { v ->
        val parentView: LinearLayout= if (rootView.findViewWithTag<LinearLayout>(NEW_NAME_GROUP_REPLICATE)!=null) {
            rootView.findViewWithTag(NEW_NAME_GROUP_REPLICATE)
        } else {
            rootView.findViewWithTag(NAME_GROUP_REPLICATE)
        }
        val viewForDelete=v?.parent?.parent?.parent?.parent as View

        val nameGroup=viewForDelete.findViewById<TextView>(R.id.question).text
        parentView.removeView(viewForDelete)
        Timber.d("parentView=${parentView.childCount}")
        // Сменим нумерацию групп
        parentView.children.forEachIndexed { index, view ->
            view.findViewById<TextView>(R.id.question).text=parentFragment.getString(R.string.name_group,index+1)// "Группа ${index+1}"
        }

        Timber.d("Удаляем_текущую_группу_$nameGroup")
        val controlsForDelete=this@UICreator.controlList.filter { it.group_checklist!=null && it.group_checklist!!.contains(nameGroup) }
        controlList.removeAll(controlsForDelete)

        controlList.filter {
            it.replicated
        }.forEach {
            // Получим новое название группы в которой находится контрол
            val newNameGroup= (it.parentView?.parent as View).findViewById<TextView>(R.id.question)?.text
            it.group_checklist="$NEW_NAME_GROUP_REPLICATE#$newNameGroup"
        }


        //Сменим кол-во вопросов в группе
        val newNumberQuestion=controlList.size
        parentFragment.currentOrder.questionCount=newNumberQuestion
        val tvQuestionCount=parentFragment.view?.findViewWithTag<TextView>("countQuestionChecklist")
        if (tvQuestionCount!=null) {
            tvQuestionCount.text=parentFragment.getString(R.string.question_count,parentFragment.currentOrder.questionCount,parentFragment.currentOrder.answeredCount)
        }

        // Сменим значение атрибута Кол-во групп полей
        val tvNumberGroup=rootView.findViewWithTag<TextView>(NUMBER_GROUPS_FIELD_MARK)
        val numberGroups=(tvNumberGroup.text.toString()).toIntOrNull()
        if (numberGroups != null) {
            tvNumberGroup.text=(numberGroups-1).toString()
        }
    }

    private fun updateMainControlList(cl: MutableList<Models.TemplateControl>) {
        // Формируем правильный список контролов, по которому сохраняются данные
        val position=this.controlList.indexOfFirst { it.group_checklist==NAME_GROUP_REPLICATE }
        this.controlList.removeAll{
            it.group_checklist==NAME_GROUP_REPLICATE
        }

        //Сменим название группы, чтоб при восстановлении чеклиста, построилась вложенность
        cl.forEach {
            it.group_checklist= it.group_checklist?.replace("Группа","ВопросыТираж#Группа")
        }

        if (position>0) {
            this.controlList.addAll(position,cl)
        } else {
            this.controlList.addAll(cl)
        }

    }

    private fun setQuestionColor(it: Models.TemplateControl, templateStep: LinearLayout) {
        var questionColor=Color.BLACK
        if (it.question.indexOf("подающий")!=-1) {
            Timber.d("Color.RED")
            questionColor=Color.RED
        }
        if (it.question.indexOf("обратный")!=-1) {
            Timber.d("Color.BLUE")
            questionColor=Color.BLUE
        }
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread {
                templateStep.findViewById<TextView>(R.id.question).setTextColor(questionColor)
            }
        }

    }

    fun refresh() {
        val llMain=rootView as LinearLayout
        if(llMain.childCount>0) {
            llGroupNodes= null
            listllGroupOther.clear()
            listllNode=listOf()
            llMain.removeAllViews()
            create(rootView)
        }

    }

    fun checkEnabled() {
        Timber.d("checkEnabled")
        Timber.d("parentFragment_currentOrder_status=${parentFragment.currentOrder.status}")
        enabled= !(parentFragment.currentOrder.status==parentFragment.getString(R.string.status_IN_WAY)||
                parentFragment.currentOrder.status==parentFragment.getString(R.string.status_OPEN))

        Timber.d("enabledControls_size=${enabledControls.size}")
        Timber.d("enabled=${enabled}")

        try {
            enabledControls.forEach {
                if (it is MaterialBetterSpinner) {
                    if (enabled) {
                        it.dropDownHeight = WindowManager.LayoutParams.WRAP_CONTENT
                    } else {
                        it.dropDownHeight = 0
                    }
                }
                it.isEnabled=enabled
            }
        } catch (e: Exception) {
            Timber.d("Error55")
            e.printStackTrace()
        }

    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View, index: Int? = null){
        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread{
                if (mainView is LinearLayout) {
                    if (index!=null) {
                        mainView.addView(v, index)
                    } else {
                        mainView.addView(v)
                    }
                }
            }
        } else {
            // Сообщение отправляем не в UI потоке, requireActivity не используем т.к.isAdded=false
            Handler(Looper.getMainLooper()).post{
                parentFragment.toaster.showErrorToast(R.string.checkup_fragment_not_attached)
            }

        }
    }


    private fun showDialog(id: Int, textInputEditText: TextInputEditText) {
        //Если потребуется вводить дату вручную, нужно поставить флаг focusableInTouchMode в XML
        if (id== DIALOG_DATE) {
            val dateListener =
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    dateAndTime.set(Calendar.YEAR, year)
                    dateAndTime.set(Calendar.MONTH, month)
                    dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    textInputEditText.setText(
                        SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU")).format(dateAndTime.time)
                    )
                    textInputEditText.error=null
                    Timber.d("errorControls=${parentFragment.errorControls}")
                    Timber.d("textInputEditText=${textInputEditText.id}")
                    if (parentFragment.errorControls.contains(textInputEditText)) {
                        parentFragment.errorControls.remove(textInputEditText)
                    }
                }

            DatePickerDialog(
                parentFragment.requireContext(),
                dateListener,
                dateAndTime.get(Calendar.YEAR),
                dateAndTime.get(Calendar.MONTH),
                dateAndTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        } else {
            val timeListener =
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    dateAndTime.set(Calendar.MINUTE, minute)
                    textInputEditText.setText(
                        SimpleDateFormat("HH:mm", Locale("ru", "RU")).format(dateAndTime.time)
                    )
                }

            TimePickerDialog(
                parentFragment.requireContext(),
                timeListener,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true
            ).show()
        }
    }

}