package ru.bingosoft.teploInspector.db.User

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface TrackingUserLocationDao {
    @Query("SELECT * FROM TrackingUserLocation\n" +
            "where lat<>0 and lon<>0")
    fun getAll(): Flowable<List<TrackingUserLocation>>

    @Query("SELECT * FROM TrackingUserLocation\n" +
            "where datetime(round(dateLocation/1000), 'unixepoch')>date() \n" +
            "and lat<>0 and lon<>0")
    fun getTrackingForCurrentDay(): Flowable<List<TrackingUserLocation>>

    @Query("SELECT count(*) FROM TrackingUserLocation")
    fun getSize(): Int

    @Query("DELETE FROM TrackingUserLocation")
    fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userLocation: TrackingUserLocation)

    @Update
    fun update(userLocation: TrackingUserLocation)

    @Delete
    fun delete(userLocation: TrackingUserLocation)
}