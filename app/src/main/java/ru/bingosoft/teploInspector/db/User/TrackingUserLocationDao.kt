package ru.bingosoft.teploInspector.db.User

import androidx.room.*

@Dao
interface TrackingUserLocationDao {
    @Query("SELECT * FROM TrackingUserLocation\n" +
            "where \n" +
            "datetime(dateLocation/1000, 'unixepoch')>date('now') and\n" +
            "lat<>0 and lon<>0 and synced=0")
    fun getTrackingForCurrentDay(): List<TrackingUserLocation>

     @Query("SELECT * FROM TrackingUserLocation\n" +
            "where datetime(dateLocation/1000, 'unixepoch')>datetime('now','-3 minutes')\n" +
            "and lat<>0 and lon<>0 and synced=0")
    fun getTrackingForLastMinutes(): List<TrackingUserLocation>

    @Query("SELECT count(*) FROM TrackingUserLocation")
    fun getSize(): Int

    @Query("DELETE FROM TrackingUserLocation")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userLocation: TrackingUserLocation)

    @Update
    fun update(userLocation: TrackingUserLocation)

    @Query("UPDATE TrackingUserLocation set synced = :isTrue where dateLocation in (:ids)")
    fun updateLocationSynced(ids: List<Long>, isTrue: Boolean =true)

    @Delete
    fun delete(userLocation: TrackingUserLocation)
}