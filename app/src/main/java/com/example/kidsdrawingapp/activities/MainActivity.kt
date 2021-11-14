package com.example.kidsdrawingapp.activities

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.kidsdrawingapp.ImageFile
import com.example.kidsdrawingapp.databinding.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingDialog: DialogBrushSizeBinding
    private lateinit var binding_progress_custom: DialogCustomProgressBinding
    private lateinit var binding_select_color: DialogSelectColorBinding
    private var color = 0;
    private val imageFile = ImageFile(this)
    private var mImageButtonCurrentPaint: ImageButton? = null

    private var permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    //get from intent
    companion object {
        const val OPEN_COLOR = "OPEN_COLOR"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getColorFromHomeActivity()
        initPaintMode()
        initButtons()
    }


    private fun initButtons() {
        //choose brush size
        binding.mainIBTNBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        //pick image for background
        binding.mainIBTNGallery.setOnClickListener {
            if (checkPermission()) {
                replaceImageLauncher.launch("image/*")
            } else {
                requestPermission()
            }
        }

        // undo last paint
        binding.mainIBTNUndo.setOnClickListener {
            binding.mainVWLayout.onClickUndo()
        }

        // redo last paint
        binding.mainIBTNRedo.setOnClickListener {
            binding.mainVWLayout.onClickRedo()
        }

        //save image
        binding.mainIBTNSave.setOnClickListener {
            if (checkPermission()) {
                saveImage()
            } else {
                requestPermission();
            }
        }

        //shae image
        binding.mainIBTNShare.setOnClickListener {
            if(imageFile.uri != null){
                share()
            }else{
                Toast.makeText(
                    this@MainActivity,
                    "Please save the file first",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        //select color
        binding.mainIBTNColorPicker.setOnClickListener {
            dialogSelectAColor()
        }
    }

    /**
     * open dialog and select a color
     */
    private fun dialogSelectAColor() {
        val customProgressDialog = Dialog(this)
        //init dialog view
        binding_select_color = DialogSelectColorBinding.inflate(layoutInflater)
        customProgressDialog.setContentView(binding_select_color.root)
        customProgressDialog.show()
        //listener for color select
        binding_select_color.mainSPSelectColor.setOnColorSelectedListener { col ->
            color = col
            //init brush color and button color
            binding.mainVWLayout.setColor(color)
            binding.mainIBTNColorPicker.backgroundTintList = ColorStateList.valueOf(color)
            customProgressDialog.dismiss()
        }
    }

    //
    private fun getColorFromHomeActivity() {
        val colorStr = intent.getStringExtra(OPEN_COLOR)
        if (colorStr != null) {
            color = colorStr.toInt()
        }
    }

    private fun saveImage() {
        // myBitmapAsyncTask(getBitmapFromView(binding.mainFLDrawingViewContainer))
        lifecycleScope.launch {
            saveBitmapFile(getBitmapFromView(binding.mainFLDrawingViewContainer))
        }
    }

    /**
     * async task for saving current image in storage
     * @param mBitmap Bitmap from view
     */
    private suspend fun saveBitmapFile(mBitmap: Bitmap): String {
        var result = ""
        var customProgressDialog = Dialog(this)
        withContext(Dispatchers.IO) {
            runOnUiThread {
                showProgressDialog(customProgressDialog)
            }
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)

                    val f = File(
                        externalCacheDir!!.absoluteFile.toString()
                                + File.separator + imageFile.getFileName()
                    )

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    imageFile.createUri(f)
                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog(customProgressDialog)
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: java.lang.Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }


    /**
     * share image
     */
    private fun share() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageFile.uri)
        shareIntent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share"))
    }


    private fun cancelProgressDialog(customProgressDialog: Dialog) {
        customProgressDialog.dismiss()
    }

    private fun showProgressDialog(customProgressDialog: Dialog) {
        binding_progress_custom = DialogCustomProgressBinding.inflate(layoutInflater)
        customProgressDialog.setContentView(binding_progress_custom.root)
        customProgressDialog.show()
    }

    /**
     * get bitmap from view
     */
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    /**
     * init paint mode
     */
    private fun initPaintMode() {
        //set color-picker button background
        binding.mainVWLayout.setColor(color)
        binding.mainIBTNColorPicker.backgroundTintList = ColorStateList.valueOf(color)
    }


    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        // bind  dialog_brash_size
        bindingDialog = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(bindingDialog.root)

        val brushSize10 = bindingDialog.brushIBTNSize10
        val brushSize15 = bindingDialog.brushIBTNSize15
        val brushSize20 = bindingDialog.brushIBTNSize20
        val brushSize25 = bindingDialog.brushIBTNSize25
        val brushSize30 = bindingDialog.brushIBTNSize30


        brushSize10.setOnClickListener {
            changeBrushSize(10, brushDialog)
        }
        brushSize15.setOnClickListener {
            changeBrushSize(15, brushDialog)
        }
        brushSize20.setOnClickListener {
            changeBrushSize(20, brushDialog)
        }
        brushSize25.setOnClickListener {
            Log.d("brushsizewhat","size clicked: 25")

            changeBrushSize(25, brushDialog)
        }
        brushSize30.setOnClickListener {
            changeBrushSize(30, brushDialog)
        }

        brushDialog.show()
    }

    private fun changeBrushSize(brushSize: Int, brushDialog: Dialog) {
        Log.d("brushsizewhat","size: $brushSize")
        binding.mainVWLayout.setSizeForBrush(brushSize.toFloat())
        brushDialog.dismiss()
    }


    /**
     * check if permission is granted
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // if android version is 11 or above
            Environment.isExternalStorageManager()
            return true;
        } else {
            val readCheck =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            val writeCheck =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            return readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED

        }
    }


    /**
     * request permission for android 11 and below
     */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                var intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", packageName, null);
                intent.data = uri;
                activityResultLauncher.launch(intent)
            } catch (e: Exception) {
                var intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activityResultLauncher.launch(intent)

            }
        } else {
            //below android 11
            // ActivityCompat.requestPermissions(this, permissions, 30)
            requestStoragePermissionLauncher.launch(permissions)
        }
    }

    //check storage permission
    private
    val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Denied", Toast.LENGTH_LONG).show()
                }
            }
        }

    /**
     * upload image from storage
     */
    private
    val replaceImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                binding.mainIMPBackground.visibility = View.VISIBLE
                binding.mainIMPBackground.setImageURI(uri)
            }
        }

    /**
     * request Storage Permission Launcher
     */
    private
    val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var readPer = false
            var writePer = false

            permissions.entries.forEach {
                val permissionName = it.key
                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    readPer = it.value
                } else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    writePer = it.value
                }
            }
            if (readPer && writePer) {
                saveImage()
                Toast.makeText(
                    this,
                    "Permission Granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showRationaleDialog(
                    "Drawing App", "Drawing App " +
                            "needs to Access Your External Storage"
                )
                Toast.makeText(
                    this,
                    "Permission Denied",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(
//            requestCode,
//            permissions,
//            grantResults
//        )
//
//        when (requestCode) {
//            30 -> {
//                if (grantResults.isNotEmpty()) {
//                    val readPer =
//                        grantResults[0] == PackageManager.PERMISSION_GRANTED
//                    val writePer =
//                        grantResults[1] == PackageManager.PERMISSION_GRANTED
//                    if (readPer && writePer) {
//                        Toast.makeText(
//                            this,
//                            "Permission Granted",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    } else {
//                        Toast.makeText(
//                            this,
//                            "Permission Denied",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            }
//            else -> {
//                Toast.makeText(
//                    this,
//                    "You Denied Permission",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }


}