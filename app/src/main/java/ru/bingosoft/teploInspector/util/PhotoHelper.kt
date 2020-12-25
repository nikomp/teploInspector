package ru.bingosoft.teploInspector.util

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PHOTO
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class PhotoHelper {
    lateinit var parentFragment: Fragment

    /**
     * Метод для создания фото и сохранения ее в файл и БД
     *
     */
    fun createPhoto(dirName: String, step: Models.TemplateControl) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val uri: Uri?
        try {
            val photoFile=createImageFile("$dirName/${step.guid}")
            Timber.d("photoFile=${photoFile.absolutePath}")
            (parentFragment.requireActivity() as MainActivity).lastKnownFilenamePhoto=photoFile.absolutePath

            uri = FileProvider.getUriForFile(
                parentFragment.requireContext(), "${parentFragment.context?.packageName}.provider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            parentFragment.requireActivity().startActivityForResult(intent, PHOTO)


        } catch (ex: IOException) {
            Timber.d("Ошибка createImageFile %s", ex.message)
        }
    }

    /**
     * Метод для создание структуры папок для фотографии и самого файла для фото
     *
     * @return - возвращается файл
     * @throws IOException - метод может вызвать исключение
     */
    @Throws(IOException::class)
    private fun createImageFile(dirname: String): File {
        // Имя для папки с файлами PhotoForApp/+<id_заявки>. Если потребуется делать фотки Захоронений и Памятников, в папке с местом захоронения создадим еще 2 папки
        val stDir = "PhotoForApp/$dirname" //+Integer.toString(inSector)+"."+Integer

        Timber.d("папка с фото $stDir")

        // Создадим имя для файла с картинкой
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale("ru","RU")) .format(Date())

        val imageFileName = "JPEG_$timeStamp" + "_"
        val storageDir = File(DCIM_DIR, stDir)

        if (!storageDir.exists()) {
            Timber.d("создадим папку")
            storageDir.mkdirs() // Создадим сразу все необходимые каталоги
        }


        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    fun checkDirAndEmpty(dirName: String): Boolean {
        val file=File( "$DCIM_DIR/PhotoForApp/$dirName")
        if (file.exists() && file.isDirectory && !file.listFiles().isNullOrEmpty()) {
            return true
        }
        return false
    }

    fun deletePhoto(filename: String):Boolean {
        val file=File(filename)
        return if (file.exists() && file.isFile ) {
            file.delete()
        } else {
            false
        }
    }


}