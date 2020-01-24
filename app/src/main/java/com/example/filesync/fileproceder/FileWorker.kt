package com.example.filesync.fileproceder

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile

import com.example.filesync.MainActivity
import com.google.gson.*
import org.w3c.dom.Document
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


data class FileModel(
    val thumbnail : Bitmap?,
    val extension : String,
    val name: String,
    val dateModif : String,
    val sizeMB: String,
    val uri : Uri
) : Serializable

class UriSerializer : JsonSerializer<Uri>{
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.toString())
    }
}

class UriDeserializer : JsonDeserializer<Uri>{
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri {
        return Uri.parse(json?.asString)
    }
}

class FileWorker(appContext : Context) {
    private val appContext = appContext
    private val contentResolver = appContext.contentResolver
    private val defProjection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    private val dateFormatter = SimpleDateFormat("MMM dd")

    fun updateListMeta(list: List<FileModel>) : List<FileModel>{
        val tmp = mutableListOf<FileModel>()
        for (i in list.indices){
            val fm = getFileModelByUri(list.elementAt(i).uri)
            fm?.let {
                tmp.add(it)
            }
        }
        return tmp
    }

    fun getFileModelByUri(uri : Uri) : FileModel? {
        try {
            val pFD = contentResolver.openFileDescriptor(uri, "r")
            val fd = pFD?.fileDescriptor
            var img = BitmapFactory.decodeFileDescriptor(fd)
            pFD?.close()
            val cursor : Cursor? = contentResolver.query(uri, defProjection, null, null, null )
            var dName = ""
            var extension = ""
            var size : Long = 0
            cursor?.use {
                if (it.moveToNext()){
                    dName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val sizeInd : Int = it.getColumnIndex(OpenableColumns.SIZE)
                    size = it.getLong(sizeInd)
                    val ind = dName.lastIndexOf('.')
                    if (ind >= 0 && ind != dName.length-1 && (dName.length - ind) < 5){
                        extension = dName.substring(ind+1).toUpperCase()
                        dName = dName.substring(0,ind)
                    }
                }
            }
            cursor?.close()
            if (dName.isNotEmpty()){
                var dm = "Unknown"
                uri.path?.let{
                    val file = DocumentFile.fromSingleUri(appContext, uri)
                    file?.let{
                        dm = dateFormatter.format(it.lastModified())
                    }

                }
                return FileModel(thumbnail = img,
                    name =  dName,
                    dateModif = dm,
                    sizeMB = getSizeInFormat(size),
                    uri = uri,
                    extension = extension
                )
            }
        }
        catch (e : Exception) {
            Log.e("FileModelByUri", e.message)
        }
        return null
    }

    private fun getSizeInFormat(bytes_count : Long) : String{
        if (bytes_count == 0.toLong()){
            return "Unknown"
        }
        var size = (bytes_count / 1000).toFloat()
        if(size > 1000){
            size /= 1000
            return String.format("%.1f MB", size)
        }
        return String.format("%.1f KB", size)
    }
}