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
    }

    object Location {
        val MAPQUEST_HEADQUARTERS: LatLng = LatLng(56.3287, 44.002) //Нижний Новгород
    }
}