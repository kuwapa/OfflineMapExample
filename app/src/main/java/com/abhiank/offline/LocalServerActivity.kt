package com.abhiank.offline

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.safframework.server.core.AndroidServer
import com.safframework.server.core.http.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow

class LocalServerActivity : AppCompatActivity() {

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }

    private lateinit var map: MapboxMap
    private lateinit var serverApp: AndroidServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this)
        setContentView(R.layout.activity_local_server)

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            serverApp.close()
        }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it

            map.isDebugActive = true

            //So that no files are cashed by mapbox itself
            //https://docs.mapbox.com/android/maps/guides/cache-management/
            val fileSource = OfflineManager.getInstance(this)
            fileSource.setMaximumAmbientCacheSize(0, null)

            val dbFile = getFileFromAssets(this, MainActivity.MBTILES_NAME)
            val openDatabase =
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            val bounds = getLatLngBounds(dbFile)
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))

            CoroutineScope(Dispatchers.IO).launch {
                serverApp = AndroidServer.Builder { port { 7000 } }.build()
                serverApp
                    .get("/tiles/{z}/{x}/{y}") { request, response: Response ->
                        Log.d(
                            "tiles",
                            "zoom = ${request.param("z")}, x = ${request.param("x")}, y = ${request.param("y")}"
                        )

                        val zoom = request.param("z")!!.toInt()

                        val tile = getTile(
                            openDatabase,
                            zoom,
                            request.param("x")!!.toInt(),
                            (2.0.pow(zoom.toDouble())).toInt() - 1 - request.param("y")!!.toInt()
                        )

                        if (tile != null) {
                            response.sendFile(tile, "${request.param("y")}.png", "application/x-protobuf")
                            response.addHeader("Content-Encoding", "gzip")
                            response.setStatus(200)
                        } else {
                            response.setBodyText("Error")
                            response.setStatus(404)
                        }
                    }
                    .start()
            }

            map.setStyle(
                Style.Builder().fromUri("asset://bright_server.json")
            )

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverApp.close()
    }

    @SuppressLint("Recycle")
    private fun getTile(db: SQLiteDatabase, z: Int, y: Int, x: Int): ByteArray? {
        val cursor = db.query(
            "tiles",
            arrayOf("zoom_level", "tile_column", "tile_row", "tile_data"),
            "zoom_level = $z AND tile_column = $y AND tile_row = $x",
            arrayOf(),
            null,
            null,
            null,
        )

        return if (cursor == null || cursor.count == 0) {
            null
        } else {
            cursor.moveToFirst()
            cursor.getBlob(3)
        }
    }
}