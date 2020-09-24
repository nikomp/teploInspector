package ru.bingosoft.teploInspector.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
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
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
//private val rootView: View, val checkup: Checkup, private val photoHelper: PhotoHelper, private val checkupPresenter: CheckupPresenter
@Suppress("unused")
class UICreator(val parentFragment: CheckupFragment, val checkup: Checkup) {
    lateinit var controlList: List<Models.TemplateControl>
    private val dateAndTime: Calendar =Calendar.getInstance()

    private val photoHelper=parentFragment.photoHelper
    private lateinit var rootView:View
    private var enabled: Boolean=true
    private var llGroupNodes: LinearLayout?=null
    private var listllNode: List<LinearLayout> = listOf()
    private var listllGroupOther: MutableList<LinearLayout> = mutableListOf()

    fun create(rootView: View) {
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

        controlList.forEach controls@ {
            when (it.type) {
                "combobox","date","time","textinput","numeric","photo" -> {
                    Timber.d("генерим ${it.type}")
                    createWithGrouping(it)
                    return@controls
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }

        }

    }

    private fun createWithGrouping(it: Models.TemplateControl) {
        if (it.replication_nodes==true || it.replicating_archival_records==true) {
            // Создаем группу Узлы
            if (llGroupNodes==null) {
                llGroupNodes=createGroupNodes()
            }
            // Создаем Узлы в группе
            if (listllNode.isEmpty()) {
                listllNode=createNodes(llGroupNodes!!)
            }

            if (it.group_checklist==null) {
                // Тиражируем по узлам
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node-1]
                        when (it.type) {
                            "combobox" -> createCombobox(it,llCurrentNode)
                            "textinput" -> createTextInput(it,llCurrentNode)
                            "numeric" -> createNumeric(it,llCurrentNode)
                            "date" -> createDate(it,llCurrentNode)
                            "time" -> createTime(it,llCurrentNode)
                            "photo" -> createPhoto(it,llCurrentNode)
                        }
                    }
                }
            } else {
                Timber.d("it.group_checklist=${it.group_checklist}")
                // Есть еще и группа
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node-1]
                        val llGroup=createGroup(it.group_checklist,llCurrentNode,it.node.toString())
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
            // Тиражирования по узлам нет
            if (it.group_checklist!=null) {
                //..., но есть Группа
                var llGroup=createGroup(it.group_checklist,rootView.findViewById(R.id.llMain))
                // Проверим, возможно эта группа тиражируется по узлам (как ИТП(общий ввод))
                Timber.d("_it=$it")
                if (it.replicated_on!=null) {
                    Timber.d("it.replicated_on=${it.replicated_on}")
                    if (it.node_itp!=null) {
                        Timber.d("it.node_itp=${it.node_itp}")
                        llGroup=createGroup("Узел ${it.group_checklist} ${it.node_itp}",llGroup,it.node_itp)
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
                    "combobox" -> createCombobox(it, rootView.findViewById(R.id.llMain))
                    "textinput" -> createTextInput(it, rootView.findViewById(R.id.llMain))
                    "numeric" -> createNumeric(it, rootView.findViewById(R.id.llMain))
                    "date" -> createDate(it, rootView.findViewById(R.id.llMain))
                    "time" -> createTime(it, rootView.findViewById(R.id.llMain))
                    "photo" -> createPhoto(it, rootView.findViewById(R.id.llMain))
                }
            }

        }
    }

    private fun createCombobox(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

        //templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question

        val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

        //doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))
        doAssociateParent(templateStep, parent)


        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item,
            it.value
        )

        materialSpinner.isEnabled = enabled
        if (enabled) {
            materialSpinner.dropDownHeight = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            materialSpinner.dropDownHeight = 0
        }

        // Заполним spinner
        materialSpinner.setAdapter(spinnerArrayAdapter)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            materialSpinner.setText(it.resvalue)
        }
        // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
        materialSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

    }

    private fun createTextInput(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_textinput, rootView.parent as ViewGroup?, false) as LinearLayout


        //attachListenerToFab(templateStep,it)

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled


        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)


        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))
    }

    private fun createNumeric(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep = LayoutInflater.from(rootView.context).inflate(
            R.layout.template_numeric, rootView.parent as ViewGroup?, false
        ) as LinearLayout


        templateStep.id = it.id
        templateStep.findViewById<TextView>(R.id.question).text = it.question

        val textInputLayout = templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint = it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText = templateStep.findViewById<TextInputEditText>(R.id.tiet)

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()) {
            textInputEditText.setText(it.resvalue)
        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(
            TextWatcherHelper(
                it,
                this,
                templateStep
            )
        )
    }

    private fun createDate(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_date, rootView.parent as ViewGroup?, false) as LinearLayout

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_DATE, textInputEditText)
        }

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }

        textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))
    }

    private fun createTime(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_time, rootView.parent as ViewGroup?, false) as LinearLayout

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_TIME, textInputEditText)
        }

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }

        textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))
    }

    private fun createPhoto(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_photo2, rootView.parent as ViewGroup?, false) as LinearLayout

        //attachListenerToFab(templateStep,it)

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        templateStep.tag=it

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        Timber.d("Фото11 ${it.resvalue}")
        /*if (it.resvalue.isNotEmpty()){
            Timber.d("Фото ${it.resvalue}")
            templateStep.findViewById<TextView>(R.id.photoResult).text=parentFragment.getString(R.string.photoResult,"DCIM\\PhotoForApp\\${it.resvalue}")
        }*/

        // Обработчик для кнопки "Добавить фото"
        val btnPhoto=templateStep.findViewById<MaterialButton>(R.id.btnPhoto)
        btnPhoto.isEnabled = enabled
        val stepCheckup=it
        btnPhoto.tag=templateStep
        btnPhoto.setOnClickListener{
            Timber.d("Добавляем фото")
            //val ts=it.tag


            // Сбрасываем признак Checked
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            (parentFragment.requireActivity() as MainActivity).photoStep=stepCheckup // Сохраним id контрола для которого делаем фото
            (parentFragment.requireActivity() as MainActivity).photoDir="${curOrder.guid}/${stepCheckup.guid}" // Сохраним id контрола для которого делаем фото
            photoHelper.createPhoto(curOrder.guid, stepCheckup)
        }

        val btnClearAll =
            templateStep.findViewById<MaterialButton>(R.id.btnPhotoDeleteAll)
        btnClearAll.isEnabled = enabled
        btnClearAll.setOnClickListener {
            Timber.d("Удалим все фото")
        }

        val images: List<String>
        images = if (!it.resvalue.isNullOrEmpty()) {
            Timber.d("Фото ${it.resvalue}")
            // Обновим список с фото
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            val stDir = "PhotoForApp/${curOrder.guid}/${stepCheckup.guid}"
            val storageDir =
                File(
                    //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    DCIM_DIR,
                    stDir
                )

            OtherUtil().getFilesFromDir("$storageDir")

        } else {
            listOf()
        }

        val leftBtn = templateStep.findViewById(R.id.left_nav) as ImageButton
        val rightBtn = templateStep.findViewById(R.id.right_nav) as ImageButton

        // Обновим вьювер с фотками
        parentFragment.refreshPhotoViewer(templateStep, images, rootView.context)

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
    }

    private fun createNodes(llContainer: LinearLayout): List<LinearLayout> {
        Timber.d("генерим узлы")
        val listNodesView= mutableListOf<LinearLayout>()
        if (parentFragment.currentOrder.countNode!=null) {
            for (i in 1..parentFragment.currentOrder.countNode!!) {
                val templateStep=LayoutInflater.from(rootView.context).inflate(
                    R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

                Timber.d("Узел $i")
                templateStep.findViewById<TextView>(R.id.question).text="Узел $i"

                val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
                val llNode=templateStep.findViewById<LinearLayout>(R.id.container)
                val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

                clTitle.setOnClickListener {
                    if (llNode.visibility==View.VISIBLE) {
                        llNode.visibility=View.GONE
                        ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
                    } else {
                        llNode.visibility=View.VISIBLE
                        ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
                    }
                }

                listNodesView.add(llNode)

                doAssociateParent(templateStep, llContainer)
            }
        } else {
            Timber.d("parentFragment.currentOrder.countNode==null")
            parentFragment.toaster.showToast(R.string.not_count_node)
        }

        return listNodesView

    }

    private fun createGroupNodes() :LinearLayout {
        Timber.d("генерим группу для Узлов")

        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

        //attachListenerToFab(templateStep,it)

        //templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text="Узлы"

        val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
        val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
        val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

        clTitle.setOnClickListener {
            if (llGroup.visibility==View.VISIBLE) {
                llGroup.visibility=View.GONE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
            } else {
                llGroup.visibility=View.VISIBLE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
            }
        }

        doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))
        Timber.d("llGroup=$llGroup")

        return llGroup
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String=""): LinearLayout {
        Timber.d("генерим группу $name$node")

        // Проверим, возможно группа уже создана
        val ll=listllGroupOther.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
                }
            }

            doAssociateParent(templateStep, parent)
            Timber.d("llGroup=$llGroup")

            listllGroupOther.add(llGroup)

            return llGroup
        }


    }

    fun refresh() {
        val llMain= rootView.findViewById(R.id.llMain) as LinearLayout
        if(llMain.childCount>0) {
            llGroupNodes= null
            listllGroupOther.clear()
            listllNode=listOf()
            llMain.removeAllViews()
            create(rootView)
        }

    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View, index: Int?=null){
        if (mainView is LinearLayout) {
            if (index!=null) {
                mainView.addView(v,index)
            } else {
                mainView.addView(v)
            }

        }
    }


    private fun showDialog(id: Int, textInputEditText: TextInputEditText) {
        if (id== DIALOG_DATE) {
            val dateListener =
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    dateAndTime.set(Calendar.YEAR, year)
                    dateAndTime.set(Calendar.MONTH, month)
                    dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    textInputEditText.setText(
                        SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(dateAndTime.time)
                    )
                }

            DatePickerDialog(parentFragment.requireContext(),
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
                        SimpleDateFormat("HH:mm", Locale("ru","RU")).format(dateAndTime.time)
                    )
                }

            TimePickerDialog(parentFragment.requireContext(),
                timeListener,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true
            ).show()
        }
    }



}