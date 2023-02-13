package com.abhiank.offline

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class RasterActivity : AppCompatActivity() {

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }
    private lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, null)
        setContentView(R.layout.activity_raster)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it

//            map.setStyle(Style.Builder().fromUri("asset://raster_style.json"))

            showMbTilesMap(getFileFromAssets(this, "test_database3.mbtiles"))
        }
    }

    private fun showMbTilesMap(mbtilesFile: File) {
        val styleJsonInputStream = assets.open("raster_style.json")

        //Creating a new file to which to copy the json content to
        val dir = File(filesDir.absolutePath)
        val styleFile = File(dir, "raster_style.json")
        //Copying the original JSON content to new file
        copyStreamToFile(styleJsonInputStream, styleFile)

        val bounds = getLatLngBounds(mbtilesFile)
        val minZoomLevel = getMinZoom(mbtilesFile)

        val uri = Uri.fromFile(mbtilesFile)

        Log.d("showMBTilesFile", "bounds = $bounds")
        Log.d(
            "showMBTilesFile",
            "northeast = ${bounds.northEast}, southEast = ${bounds.southEast}, northWest = ${bounds.northWest}, southWest = ${bounds.southWest}"
        )
        Log.d("showMBTilesFile", "fileUri = $uri")

        //Replacing placeholder with uri of the mbtiles file
        val newFileStr = styleFile.inputStream().readToString()
            .replace("___FILE_URI___", "mbtiles://$uri")

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
                        map.setMinZoomPreference(map.cameraPosition.zoom)

                        map.limitViewToBounds(bounds)

                        map.addOnScaleListener(object : MapboxMap.OnScaleListener {
                            override fun onScaleBegin(detector: StandardScaleGestureDetector) {}

                            override fun onScale(detector: StandardScaleGestureDetector) {
                                map.limitViewToBounds(bounds)
                            }

                            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
                        })

                        map.addOnCameraIdleListener { map.limitViewToBounds(bounds) }
                    }

                })

            map.isDebugActive = true
            showBoundsArea(style, bounds, Color.RED, "source-id-1", "layer-id-1", 0.25f)
        }
    }
}