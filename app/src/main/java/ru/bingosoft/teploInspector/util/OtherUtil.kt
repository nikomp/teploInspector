package ru.bingosoft.teploInspector.util

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import ru.bingosoft.teploInspector.db.Orders.Orders
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

class OtherUtil(private val toaster: Toaster) {
    val ctx=toaster.ctx

    fun getDistance(userLocation: Location, order: Orders): Float {
        val orderLocation=Location(LocationManager.GPS_PROVIDER)
        orderLocation.longitude=order.lon
        orderLocation.latitude=order.lat

        val dist=userLocation.distanceTo(orderLocation)/1000
        return round(dist*10)/10
    }

    fun saveExifLocation(filename: String, photoLocation: Location?):Boolean {
        var success=true
        try {
            Timber.d("Exif")
            if (!File(filename).exists()) {
                return false
            }
            val exif = ExifInterface(filename)
            //Timber.d(convertLat(photoLocation!!.latitude))
            if (photoLocation!=null) {
                val num1Lat=photoLocation.latitude.roundToInt()
                val num2Lat=((photoLocation.latitude-num1Lat)*60).roundToInt()
                val num3Lat=(photoLocation.latitude-(num1Lat+num2Lat/60))

                val num1Lon=photoLocation.longitude.roundToInt()
                val num2Lon=((photoLocation.longitude-num1Lon)*60).roundToInt()
                val num3Lon=(photoLocation.longitude-(num1Lon+num2Lon/60))

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "$num1Lat/1,$num2Lat/1,$num3Lat/1000")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "$num1Lon/1,$num2Lon/1,$num3Lon/1000")

                println(photoLocation.latitude)
                if (photoLocation.latitude>0) {
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
                } else {
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
                }

                if (photoLocation.longitude>0) {
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
                } else {
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
                }

                exif.saveAttributes()
                Timber.d("Exif_saved")
            }

        } catch (e: IOException) {
            e.printStackTrace()
            success=false
        }
        return success
    }

    fun getFilesFromDir(dir: String): List<String> {
        Timber.d("getFilesFromDir=${dir}")
        val list = mutableListOf<String>()
        val directory = File(dir)
        if (directory.exists()) {
            val files = directory.listFiles()
            files?.forEach {
                if (it.length() != 0L) {
                    list.add("$dir/${it.name}")
                } else {
                    it.delete()
                }
            }
        }
        return list
    }

    fun deleteDir(dir: String) {
        val directory = File(dir)
        if (directory.exists()) {
            val files = directory.listFiles()
            files?.forEach {
                deleteDir(it.path)
            }
            directory.delete()
        }
    }

    fun getDifferenceTime(startTime: Long, endTime: Long): Long {
        val seconds=(endTime-startTime)/1000
        return seconds/60
    }

    fun writeToFile(message:String) {
        if (ContextCompat.checkSelfPermission(ctx,READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx,WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            val date= SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).format(Date())
            val dir = "TeploInspectorLogs/$date"

            try {
                val storageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}",dir)
                if (!storageDir.exists()) {
                    storageDir.mkdirs() // Создадим сразу все необходимые каталоги
                }

                val file="Log.log"
                val logFile=File("$storageDir/$file")
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }
                logFile.appendText("$message\n")
            } catch (e: Exception) {
                toaster.showErrorToast("Ошибка записи лога ${e.message}")
            }

        }

    }

}