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
        const val IS_AUTH = "is_auth"
        const val IS_INTERVAL_ROUTE = "is_interval_route"
        const val ENTER_TYPE = "enter_type"
        const val TOKEN = "accent_token"
        const val USER_ID = "user_id"
        const val ROLE_ID = "role_id"
        const val PASSWORD = "password"
        const val DATESYNC = "last_sync_date"
        const val USER_FULLNAME = "fullname"
        const val VERSION_NAME = "version_name"
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

        const val TRANSPORTATION_PUBLIC_TRANSPORT="Самостоятельно на общественном транспорте"
        const val TRANSPORTATION_PRIVATE_TRANSPORT="Самостоятельно на личном транспорте"
        const val TRANSPORTATION_FOOT="Самостоятельно пешком"
        const val TRANSPORTATION_PERFORMED_CUSTOMER="Транспортировка выполняется заказчиком"
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
        const val STATE_COMPLETED="Выполнена"
        const val STATE_CANCELED="Отменена"
    }

    object Photo{
        val DCIM_DIR= "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}"
    }


    object LocationStatus {
        const val PROVIDER_DISABLED="PROVIDER_DISABLED"
        const val PROVIDER_ENABLED="PROVIDER_ENABLED"
        const val NOT_AVAILABLE="NOT_AVAILABLE"
        const val LOCATION_UPDATED="LOCATION_UPDATED"

        const val INTERVAL_SENDING_ROUTE=3L // в минутах, 0.5 - 30 секунд
        const val INTERVAL_SAVE_LOCATION=1L // в минутах, 0.5 - 30 секунд

    }

    object MessageCode {
        const val REFUSED_PERMISSION=1 //пользователь отказался выдать разрешение на Геолокацию
        const val REPEATEDLY_REFUSED=2 //пользователь повторно отказался включить GPS
        const val DISABLE_LOCATION=3 //GPS сигнал потерян или выключен
        const val ENABLE_LOCATION=4 //GPS сигнал восстановлен
        const val USER_LOGOUT=5
        const val USER_LOGIN=6
    }

    object Location {
        val TARGET_POINT=Point(56.3287,44.002) //Нижний Новгород

        const val ZOOM_LEVEL=12.0f
        const val DESIRED_ACCURACY = 0.0
        const val MINIMAL_TIME=0L
        const val MINIMAL_DISTANCE=50.0
        const val MAX_ACCURACY=100.0
        const val USE_IN_BACKGROUND=false

    }

    object OrderTypeForDateChangeAvailable{
        val types= listOf(
            "Распломбировка ПУ",
            "Приемка ПУ из монтажа",
            "Приемка ПУ повторная",
            "Приемка ИПУ ОТ",
            "Приемка ИПУ ГВС"
        )
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

    object FinishTime {
        const val FINISH_HOURS=18
        const val FINISH_MINUTES=0
        const val FINISH_HOURS_DOUBLER=17 // Если ставить время больше, то приложение уходит в Doze т.к. будящее сообщение уже не приходят
        const val FINISH_MINUTES_DOUBLER=15
        const val FINISH_CHECK_INTERVAL=15L
    }

    object WebSocketConst{
        const val NORMAL_CLOSURE_STATUS = 1000
        const val NOTIFICATION_CHANNEL_ID_SERVICES = "TeploInspectorServices"
        const val NOTIFICATION_CHANNEL_ID_GPS_SERVICES = "TeploInspectorGPSServices"
        const val NOTIFICATION_CHANNEL_ID = "TeploInspector"
        const val NOTIFICATION_SERVICE_NOTIFICATION_ID = 521
        const val LOCATION_SERVICE_NOTIFICATION_ID = 522
        const val GPS_SERVICE_NOTIFICATION_ID = 523
        const val NOTIFICATION_CHANGE_DATE="Изменилась согласованная дата визита"
    }

    object SpecialTypesOrders{
        const val OTHER="Другое"
        const val NUMBER_GROUPS_FIELD_MARK="number_groups"
        const val NUMBER_GROUPS_QUESTION="Количество группы полей"
        const val NAME_GROUP_REPLICATE="Вопросы"
        const val NEW_NAME_GROUP_REPLICATE="ВопросыТираж"
        const val MAX_COUNT_REPLICATE_GROUP=100
        const val ROUTINE_MAINTENANCE="Регламентные работы"
    }

    object PositionSteps{
        const val GENERAL_INFORMATION_POSITION=0
        const val TECHNICAL_CHARACTERISTICS_POSITION=1
        const val ADDITIONAL_LOAD_POSITION=2
        const val CHECKUP_POSITION=3
    }

    object LockStateOrder{
        const val STATE_OPEN="Открыта"
        const val STATE_IN_WAY="В пути"
    }


    object Network{
        const val TIMEOUT=120L
    }

}
