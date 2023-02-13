package com.abhiank.offline

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class ChooserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chooser)

        findViewById<Button>(R.id.vectorMapButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<Button>(R.id.rasterMapButton).setOnClickListener {
            startActivity(Intent(this, RasterActivity::class.java))
        }

        findViewById<Button>(R.id.localServerButton).setOnClickListener {
            startActivity(Intent(this, LocalServerActivity::class.java))
        }
    }
}