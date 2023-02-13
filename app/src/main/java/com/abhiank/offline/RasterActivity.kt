package com.abhiank.offline

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class RasterActivity : AppCompatActivity() {

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }

    private lateinit var map: MapboxMap
    private lateinit var bounds: LatLngBounds
    private var minZoomLevel: Double = 0.0

    private lateinit var zoomSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this)
        setContentView(R.layout.activity_main)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
//            map.setStyle(Style.Builder().fromUri("asset://raster_style.json"))
            showMbTilesMap(getFileFromAssets(this, "test_database3.mbtiles"))
        }

        zoomSwitch = findViewById(R.id.lockZoomSwitch)
        zoomSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                map.setMinZoomPreference(minZoomLevel)
            } else {
                map.setMinZoomPreference(0.0)
            }
        }

        findViewById<SwitchCompat>(R.id.debugModeSwitch).setOnCheckedChangeListener { _, isChecked ->
            map.isDebugActive = isChecked
            if (isChecked) {
                showBoundsArea(map.style!!, bounds, Color.RED, "source-id-1", "layer-id-1", 0.25f)
            } else {
                map.style?.let {
                    it.removeLayer("layer-id-1")
                    it.removeSource("source-id-1")
                }
            }
        }
    }

    private fun showMbTilesMap(mbtilesFile: File) {
        val styleJsonInputStream = assets.open("raster_style.json")

        //Creating a new file to which to copy the json content to
        val dir = File(filesDir.absolutePath)
        val styleFile = File(dir, "raster_style.json")
        //Copying the original JSON content to new file
        copyStreamToFile(styleJsonInputStream, styleFile)

        bounds = getLatLngBounds(mbtilesFile)
        minZoomLevel = getMinZoom(mbtilesFile).toDouble()

        Log.d("showMBTilesFile", "bounds = $bounds")
        Log.d(
            "showMBTilesFile",
            "northeast = ${bounds.northEast}, southEast = ${bounds.southEast}, northWest = ${bounds.northWest}, southWest = ${bounds.southWest}"
        )

        //Replacing placeholder with uri of the mbtiles file
        val newFileStr = styleFile.inputStream().readToString()
            .replace("___FILE_URI___", "mbtiles:///${mbtilesFile.absolutePath}")

        Log.d("showMBTilesFile", "new_file_str = $newFileStr")

        //Writing new content to file
        val gpxWriter = FileWriter(styleFile)
        val out = BufferedWriter(gpxWriter)
        out.write(newFileStr)
        out.close()

        //Setting the map style using the new edited JSON file
        map.setStyle(
            Style.Builder().fromUri(Uri.fromFile(styleFile).toString())
        ) { style ->

//            map.moveCamera(CameraUpdateFactory.newCameraPosition(
//                CameraPosition.Builder()
//                    .target(LatLng(bounds.center))
//                    .zoom(minZoomLevel.toDouble())
//                    .build()
//            ))

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0),
                object : MapboxMap.CancelableCallback {
                    override fun onCancel() {}

                    override fun onFinish() {

                        if (zoomSwitch.isChecked) {
                            map.setMinZoomPreference(minZoomLevel)
                            map.limitViewToBounds(bounds)
                        }

                        map.addOnScaleListener(object : MapboxMap.OnScaleListener {
                            override fun onScaleBegin(detector: StandardScaleGestureDetector) {}

                            override fun onScale(detector: StandardScaleGestureDetector) {
                                if (zoomSwitch.isChecked)
                                    map.limitViewToBounds(bounds)
                            }

                            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
                        })

                        map.addOnCameraIdleListener {
                            if (zoomSwitch.isChecked)
                                map.limitViewToBounds(bounds)
                        }
                    }

                })
        }
    }
}