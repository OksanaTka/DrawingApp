package com.example.kidsdrawingapp.objects

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ImageFile(private val context: Context) {

    private var name: String = "DrawingApp_"
    private val type: String = ".jpg"
    private var creationTime: String = ""
    var uri: Uri? = null

    init {
        creationTime = "" + System.currentTimeMillis() / 1000
    }


    fun createUri(file: File): Uri? {
        uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName.toString() + ".provider",
            file
        )
        return uri
    }

    fun getFileName(): String {
        name = "KidDrawingApp_$creationTime$type"
        return name
    }


}