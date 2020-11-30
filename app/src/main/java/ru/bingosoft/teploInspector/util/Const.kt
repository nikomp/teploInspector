package ru.bingosoft.teploInspector.util

import android.os.Environment
import com.yandex.mapkit.geometry.Point

class Const {

    object RequestCodes {
        const val PHOTO = 1
        const val AUTH = 2
        const val PERMISSION = 123
    }

    object SharedPrefConst {
        const val APP_PREFERENCES = "AppSettings"
        const val LOGIN = "login"
        const val ENTER_TYPE = "enter_type"
        const val TOKEN = "token"
        const val USER_ID = "user_id"
        const val ROLE_ID = "role_id"
        const val PASSWORD = "password"
        const val DATESYNC = "last_sync_date"
        const val USER_FULLNAME = "fullname"
        const val USER_PHOTO_URL = "photo_url"
        const val FIREBASE_MESSAGE = "message_token"
        const val LOCATION_TRACKING= "location_tracking"
    }

    object TypeTransportation {
        var list =
            arrayOf("Самостоятельно на общественном транспорте",
                "Самостоятельно на личном транспорте",
                "Самостоятельно пешком",
                "Транспортировка выполняется заказчиком")
    }

    object StatusOrder {
        var list =
        arrayOf("Отменена",
                "Проверена",
                "Выполнена",
                "Приостановлена",
                "В пути",
                "В работе",
                "Открыта")
    }

    object Photo{
        val DCIM_DIR= "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}"
    }


    object LocationStatus {
        const val PROVIDER_DISABLED="PROVIDER_DISABLED"
        const val PROVIDER_ENABLED="PROVIDER_ENABLED"
        const val NOT_AVAILABLE="NOT_AVAILABLE"
        const val LOCATION_UPDATED="LOCATION_UPDATED"

        const val INTERVAL_SENDING_ROUTE=3 // в минутах, 0.5 - 30 секунд
    }

    object MessageCode {
        const val REFUSED_PERMISSION=1 //пользователь отказался выдать разрешение на Геолокацию
        const val REPEATEDLY_REFUSED=2 //пользователь повторно отказался включить GPS
        const val DISABLE_LOCATION=3 //GPS сигнал потерян или выключен
        const val ENABLE_LOCATION=4 //GPS сигнал восстановлен
    }

    object Location {
        val TARGET_POINT=Point(56.3287,44.002) //Нижний Новгород

        const val ZOOM_LEVEL=12.0f
        const val DESIRED_ACCURACY = 0.0
        const val MINIMAL_TIME=0L
        const val MINIMAL_DISTANCE=50.0
        const val USE_IN_BACKGROUND=false

    }

    object GeneralInformation {
        val list=listOf("Номер договора",
            "Дата договора",
            "Юридический адрес",
            "Телефон",
            "Электронная почта",
            "Почтовый адрес",
            "Контрагент",
            "Ответственный за ТХ",
            "Руководитель",
            "Телефон ответственного гор.",
            "Телефон ответственного моб.",
            "Телефон руководителя гор.",
            "Телефон руководителя моб.",
            "Принадлежность (ОР)",
            "Принадлежность (УЭН)",
            "Управл. организация (УЭН)",
            "Подрядная организация",
            "Принадлежность трассы",
            "Принадлежность узла",
            "РТС",
            "Мастер РТС"
        )
    }

    object Dialog {
        const val DIALOG_DATE=1
        const val DIALOG_TIME=2
    }

    object WebSocketConst{
        const val NORMAL_CLOSURE_STATUS = 1000
        const val NOTIFICATION_CHANNEL_ID = "TeploInspector"
    }

}