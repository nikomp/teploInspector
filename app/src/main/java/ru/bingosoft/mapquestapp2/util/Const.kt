package ru.bingosoft.mapquestapp2.util

import com.mapbox.mapboxsdk.geometry.LatLng

class Const {
    object LogTags {
        const val LOGTAG = "myLogs"
        const val CODER = "coder"
        const val SPS = "sharedPrefSaver"
        const val PH = "photoHelper"
    }

    object RequestCodes {
        const val PHOTO = 1
        const val AUTH = 2
        const val PERMISSION = 123
    }

    object SharedPrefConst {
        const val APP_PREFERENCES = "AppSettings"
        const val LOGIN = "login"
        const val IVPASS = "ivpassword"
        const val PASSWORD = "password"
        const val SESSION = "session_id"
        const val DATESYNC = "last_sync_date"
    }

    object Location {
        val MAPQUEST_HEADQUARTERS: LatLng = LatLng(56.3287, 44.002) //Нижний Новгород

        val DEFAULT_INTERVAL_IN_MILLISECONDS: Long = 1000L
        val DEFAULT_MAX_WAIT_TIME: Long = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    object Extras {
        const val DIR_NAME = "dir_name"
    }
}