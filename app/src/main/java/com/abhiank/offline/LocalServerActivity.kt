package com.abhiank.offline

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
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
//import io.javalin.Javalin
import java.io.File
import kotlin.math.pow

class LocalServerActivity : AppCompatActivity() {

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }
    private lateinit var map: MapboxMap
    //private lateinit var serverApp: Javalin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        Mapbox.getInstance(this, null)
//        setContentView(R.layout.activity_local_server)
//
//        findViewById<Button>(R.id.stopButton).setOnClickListener {
//            serverApp.stop()
//        }
//
//        mapView.onCreate(savedInstanceState)
//        mapView.getMapAsync {
//            map = it
//
//            map.isDebugActive = true
//
//            //So that no files are cashed by mapbox itself
//            //https://docs.mapbox.com/android/maps/guides/cache-management/
//            val fileSource = OfflineManager.getInstance(this)
//            fileSource.setMaximumAmbientCacheSize(0, null)
//
//            val dbFile = getFileFromAssets(this, MainActivity.MBTILES_NAME)
//            val openDatabase =
//                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
//
//            val bounds = getLatLngBounds(dbFile)
//            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
//
//            serverApp = Javalin.create().start(7000)
//            serverApp.get("/tiles/{z}/{x}/{y}") { ctx ->
//
//                Log.d(
//                    "tiles",
//                    "zoom = ${ctx.pathParam("z")}, x = ${ctx.pathParam("x")}, y = ${ctx.pathParam("y")}"
//                )
//
//                val zoom = ctx.pathParam("z").toInt()
//
//                val tile = getTile(
//                    openDatabase,
//                    zoom,
//                    ctx.pathParam("x").toInt(),
//                    (2.0.pow(zoom.toDouble())).toInt() - 1 - ctx.pathParam("y").toInt()
//                )
//
//                if (tile != null) {
//                    ctx.result(tile)
//                    ctx.contentType("application/x-protobuf")
//                    ctx.header("Content-Encoding", "gzip")
//                    ctx.status(200)
//                } else {
//                    ctx.result("Error")
//                    ctx.status(404)
//                    ctx.contentType("text/html; charset=utf-8")
//                }
//            }
//
//            map.setStyle(
//                Style.Builder().fromUri("asset://bright_server.json")
//            )
//
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //serverApp.stop()
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