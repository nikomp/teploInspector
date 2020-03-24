package ru.bingosoft.teploInspector.util

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
        const val QR_SCAN=11
    }

    object SharedPrefConst {
        const val APP_PREFERENCES = "AppSettings"
        const val LOGIN = "login"
        const val IVPASS = "ivpassword"
        const val PASSWORD = "password"
        const val SESSION = "session_id"
        const val DATESYNC = "last_sync_date"
        const val USER_FULLNAME = "fullname"
        const val USER_PHOTO_URL = "photo_url"
        const val FIREBASE_MESSAGE = "message_token"
    }

    object Location {
        val MAPQUEST_HEADQUARTERS: LatLng = LatLng(56.3287, 44.002) //Нижний Новгород

        val DEFAULT_INTERVAL_IN_MILLISECONDS: Long = 1000L
        val DEFAULT_MAX_WAIT_TIME: Long = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
        const val ID_ICON="id-icon"
    }

    object Extras {
        const val DIR_NAME = "dir_name"
    }

    object Orders {
        const val STATE_DONE = "2"
        const val STATE_IN_WORK = "1"
    }
}