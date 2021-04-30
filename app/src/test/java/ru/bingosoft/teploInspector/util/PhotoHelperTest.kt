package ru.bingosoft.teploInspector.util

import android.content.Context
import androidx.core.content.FileProvider
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.*
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity


class PhotoHelperTest {
    lateinit var photoHelper: PhotoHelper
    lateinit var context: Context
    lateinit var checkupFragment: CheckupFragment
    lateinit var fakeStep: Models.TemplateControl

    @Rule
    @JvmField
    var folder = TemporaryFolder()

    @Before
    fun setUp() {
        context =  mock(Context::class.java)
        //photoHelper= mock(PhotoHelper::class.java)
        photoHelper= PhotoHelper()
        checkupFragment=mock(CheckupFragment::class.java)
        photoHelper.parentFragment=checkupFragment
        fakeStep=Models.TemplateControl(guid="",results_guid = "fake_results_guid")
    }

    @Test
    fun testCreatePhoto() {


        val mainActivity=mock(MainActivity::class.java)
        `when`(photoHelper.parentFragment.requireActivity()).thenReturn(mainActivity)
        `when`(photoHelper.parentFragment.requireContext()).thenReturn(context)
        `when`(photoHelper.parentFragment.requireContext().packageName).thenReturn("ru.bingosoft.teploInspector")
        `when`(mainActivity.lastKnownFilenamePhoto).thenReturn("fakeDirName\\fake_results_guid")

        mockStatic(FileProvider::class.java)
        photoHelper.createPhoto("fakeDirName",fakeStep)
        verify(mainActivity).startActivityForResult(any(), anyInt())

        val expectedLastKnownFilenamePhoto="D:\\OurPrograms\\AndroidStudioProject\\MapQuestYandex_teplo_Inspect\\app\\null\\PhotoForApp\\fakeDirName\\fake_results_guid\\JPEG_2021-04-27_120645_3049707241250673400.jpg"
        assertTrue(expectedLastKnownFilenamePhoto.contains(mainActivity.lastKnownFilenamePhoto))

    }

    @Test
    fun testDeletePhoto() {
        assertFalse(photoHelper.deletePhoto("nonexistentFile"))
        val createdFile=folder.newFile("myfile.txt")
        assertTrue(photoHelper.deletePhoto(createdFile.path))
    }

    @Test
    fun testCheckDirAndEmpty() {
        assertFalse(photoHelper.checkDirAndEmpty("nonexistentDir"))
        val createdDir=folder.newFolder("fakeDirName")
        assertFalse(photoHelper.checkDirAndEmpty(createdDir.path))
        val createdFile=folder.newFile("fakeDirName/myfile.txt")
        assertFalse(photoHelper.checkDirAndEmpty(createdFile.path))
        assertTrue(photoHelper.checkDirAndEmpty(createdDir.path))
    }

}