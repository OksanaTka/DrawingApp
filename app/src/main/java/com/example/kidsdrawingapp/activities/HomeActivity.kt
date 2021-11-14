package com.example.kidsdrawingapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.kidsdrawingapp.activities.MainActivity.Companion.OPEN_COLOR
import com.example.kidsdrawingapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private var color = 0

    private lateinit var binding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.homeSPPalette.setOnColorSelectedListener {
            col -> color = col
            openMainActivity();
        }
        Log.d("whichColor","$color")

      //  binding.homeSPPalette.setSelectedColor(resources.getColor(R.color.white))

    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(OPEN_COLOR, color.toString())
        }
        startActivity(intent)
        finish()
    }
}