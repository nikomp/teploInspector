package ru.bingosoft.teploInspector.util

import android.location.Location
import android.location.LocationManager
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.math.round
import kotlin.math.roundToInt

class OtherUtil {
    fun getDirForSync(checkups: List<Checkup>) :List<String> {
        Timber.d("getDirForSync")
        val list= mutableListOf<String>()
        checkups.forEach { it ->
            val controlList = Gson().fromJson(it.textResult, Models.ControlList::class.java)
            val controlPhoto=controlList.list.filter { it.type=="photo" }
            controlPhoto.forEach{
                it.resvalue?.let { it1 -> list.add(it1) }
            }
        }

        return list
    }

    fun getDistance(userLocation: Location, order: Orders): Float {
        val orderLocation=Location(LocationManager.GPS_PROVIDER)
        orderLocation.longitude=order.lon
        orderLocation.latitude=order.lat

        val dist=userLocation.distanceTo(orderLocation)/1000
        return round(dist*10)/10
    }

    fun saveExifLocation(filename: String, photoLocation: Location?) {
        try {
            Timber.d("Exif")
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
        }
    }

    fun getFilesFromDir(dir: String): List<String> {
        Timber.d("getFilesFromDir")
        val list = mutableListOf<String>()
        val directory = File(dir)
        val files = directory.listFiles()
        files?.forEach {
            if (it.length() != 0L) {
                list.add("$dir/${it.name}")
            } else {
                it.delete()
            }
        }

        return list
    }
}