package ru.bingosoft.teploInspector.util

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import junit.framework.Assert.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.BDDMockito.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.db.Orders.Orders
import java.io.*


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class OtherUtilTest {
    lateinit var otherUtil: OtherUtil
    lateinit var context: Context
    lateinit var toaster: Toaster

    @Rule @JvmField
    var folder = TemporaryFolder()

    @Before
    fun setUp() {
        /*val mockToast=mock(Toaster::class.java)
        otherUtil=OtherUtil(mockToast)*/
        context =  ApplicationProvider.getApplicationContext<App>()
        toaster= Toaster(context)
        otherUtil= OtherUtil(toaster)
    }

    @Test
    fun testGetCtx() {
        assertEquals(toaster.ctx, otherUtil.ctx)
    }

    @Test
    fun testGetDifferenceTime() {
        assertEquals(1, otherUtil.getDifferenceTime(60000, 120000))
    }

    // Проверим, что после выполнения otherUtil.deleteDir нет папки
    @Test
    fun testDeleteDir() {
        val createdFolder = folder.newFolder("subfolder")
        otherUtil.deleteDir(createdFolder.path)
        assertTrue(!createdFolder.exists())
    }

    // Проверим, что после выполнения otherUtil.deleteDir нет папки и файлов в этой папке
    @Test
    fun testDeleteDirWithFiles() {
        val createdFolder = folder.newFolder("subfolder")
        val createdFile=folder.newFile("subfolder/myfile.txt")
        otherUtil.deleteDir(createdFolder.path)
        assertTrue(!createdFile.exists())
        assertTrue(!createdFolder.exists())
    }

    @Test
    fun testGetDistance() {
        //56.291627, 43.983062
        //56.290620, 43.995439

        val userLocation = mock(Location::class.java)
        val order= mock(Orders::class.java)
        `when`(userLocation.distanceTo(any(Location::class.java))).thenReturn(1000f)

        assertEquals(1f, otherUtil.getDistance(userLocation = userLocation, order = order))
    }

    @Test
    fun testGetFilesFromDir() {
        val createdFolder = folder.newFolder("subfolder")
        val createdFile=folder.newFile("subfolder/myfile.txt")
        try {
            val fw1 = FileWriter(createdFile)
            val bw1 = BufferedWriter(fw1)
            bw1.write("content for file1")
            bw1.close()
        } catch (e: IOException) {
            println(e.message)
        }
        val listFiles=otherUtil.getFilesFromDir(createdFolder.path)
        assertTrue(listFiles.isNotEmpty())
    }

    @Test
    fun testGetFilesFromDirEmptyBecauseFileSizeIsZero() {
        val createdFolder = folder.newFolder("subfolder")
        folder.newFile("subfolder/myfile.txt")
        val listFiles=otherUtil.getFilesFromDir(createdFolder.path)
        assertTrue(listFiles.isEmpty())
    }

    @Test
    fun testSaveExifLocation()  {
        val mockLocation=mock(Location::class.java)
        assertFalse(otherUtil.saveExifLocation("", mockLocation))

        val file = File("fakeImageFile.jpg")
        if (file.exists()) file.delete()
        try {
            val bitmap =
                Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        assertTrue(otherUtil.saveExifLocation(file.path, mockLocation))
        val fakeLocation=Location(LocationManager.GPS_PROVIDER)
        fakeLocation.latitude=56.291627
        fakeLocation.longitude=43.983062

        assertTrue(otherUtil.saveExifLocation(file.path, fakeLocation))
    }


}