package com.example.kidsdrawingapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.kidsdrawingapp.activities.MainActivity.Companion.OPEN_COLOR
import com.example.kidsdrawingapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private var color = 0

    private var binding: ActivityHomeBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.homeSPPalette?.setOnColorSelectedListener {
            col -> color = col
            openMainActivity();
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(OPEN_COLOR, color.toString())
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}