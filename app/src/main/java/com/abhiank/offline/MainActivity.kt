package com.abhiank.offline

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.io.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val MBTILES_NAME = "maps_south_sulawesi.mbtiles"
    }

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }

    private lateinit var map: MapboxMap
    private lateinit var bounds: LatLngBounds
    private var minZoomLevel: Double = 0.0

    private lateinit var zoomSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, null)
        setContentView(R.layout.activity_main)

        fun changeLanguage(lang: String) {
            map.getStyle { style ->
                style.getLayer("waterway-name")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("water-name-lakeline")
                    ?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("water-name")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("poi-level-3")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("poi-level-2")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("poi-level-1")?.setProperties(PropertyFactory.textField(lang))

                style.getLayer("highway-name-path")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("highway-name-minor")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("highway-name-major")?.setProperties(PropertyFactory.textField(lang))
                /*
                Not translating the highway-shield cuz numbers will be in latin and doing this
                causes errors
                https://github.com/systemed/tilemaker/issues/305#issuecomment-927429019
                 */
                //style.getLayer("highway-shield")?.setProperties(textField(lang))

                style.getLayer("place-other")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-village")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-town")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-city")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-city-capital")?.setProperties(PropertyFactory.textField(lang))

                style.getLayer("place-country-3")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-country-2")?.setProperties(PropertyFactory.textField(lang))
                style.getLayer("place-country-1")?.setProperties(PropertyFactory.textField(lang))

                style.getLayer("place-continent")?.setProperties(PropertyFactory.textField(lang))
            }
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

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it

            //Showing a green dot at map center to show how the map panning is restricted to the bbox
            //showMapCenter()

            map.addOnCameraMoveListener {
                Log.d("zoom", map.cameraPosition.zoom.toString())
            }

            //Getting reference to mbtiles file in assets
            showMbTilesMap(getFileFromAssets(this, MBTILES_NAME))
//            showMbTilesMap(getFileFromAssets(this, "maps_salzburg_4.mbtiles"))

//            showMbTilesMap(File("/storage/emulated/0/Android/data/com.abhiank.offline/files/belarus.mbtiles"))

            var files = ""
            getExternalFilesDir(null)!!.listFiles()!!.forEach { eachFile ->
                files = files + eachFile.path + " ,"
            }
            Log.i("files", files)

            changeLanguage("{name_en}")
        }

        findViewById<Button>(R.id.pickFileButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            filePickerReturnResult.launch(intent)
        }
    }

    private val filePickerReturnResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val returnIntent = result.data!!
                showMbTilesMap(File(returnIntent.data!!.path!!))
            }
        }

    private fun showMbTilesMap(mbtilesFile: File) {
        val styleJsonInputStream = assets.open("bright.json")

        //Creating a new file to which to copy the json content to
        val dir = File(filesDir.absolutePath)
        val styleFile = File(dir, "bright.json")
        //Copying the original JSON content to new file
        copyStreamToFile(styleJsonInputStream, styleFile)

        bounds = getLatLngBounds(mbtilesFile)
        minZoomLevel = getMinZoom(mbtilesFile).toDouble()

        val uri = Uri.fromFile(mbtilesFile)

        Log.d("showMBTilesFile", "fileUri = $uri")

        //Replacing placeholder with uri of the mbtiles file
        val newFileStr = styleFile.inputStream().readToString()
            .replace("___FILE_URI___", "mbtiles://$uri")

        //Writing new content to file
        val gpxWriter = FileWriter(styleFile)
        val out = BufferedWriter(gpxWriter)
        out.write(newFileStr)
        out.close()

        //Setting the map style using the new edited JSON file
        map.setStyle(
            Style.Builder().fromUri(Uri.fromFile(styleFile).toString())
        ) { style -> }

        //Setting camera view over the mbtiles area
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0),
            object : MapboxMap.CancelableCallback {
                override fun onCancel() {}

                override fun onFinish() {
                    Log.d("zoom_min", map.cameraPosition.zoom.toString())

                    if (minZoomLevel == 0.0) {
                        minZoomLevel = map.cameraPosition.zoom
                    }

                    if (zoomSwitch.isChecked) {
                        //Now that the camera is showing the new bounds fully, the current zoom becomes the min zoom
                        map.setMinZoomPreference(minZoomLevel)

                        //Limiting the camera to this bounds at this zoom level
                        map.limitViewToBounds(bounds)
                    }


                    /*
                    Added a scale listener so that when zoom changes, new bbox can be created
                    and map can be limited to that
                     */
                    map.addOnScaleListener(object : MapboxMap.OnScaleListener {
                        override fun onScaleBegin(detector: StandardScaleGestureDetector) {}

                        override fun onScale(detector: StandardScaleGestureDetector) {
                            if (zoomSwitch.isChecked) {
                                map.limitViewToBounds(bounds)
                            }
                        }

                        override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
                    })

                    map.addOnCameraIdleListener {
                        if (zoomSwitch.isChecked) {
                            map.limitViewToBounds(bounds)
                        }
                    }
                }
            })
    }

    private fun showMapCenter() {
        val mapCenter = View(this)
        mapCenter.layoutParams = FrameLayout.LayoutParams(15, 15, Gravity.CENTER)
        mapCenter.setBackgroundColor(Color.GREEN)
        mapView.addView(mapCenter)
    }
}

//https://stackoverflow.com/a/56074084/3090120
fun copyStreamToFile(inputStream: InputStream, outputFile: File) {

    inputStream.use { input ->
        val outputStream = FileOutputStream(outputFile)
        outputStream.use { output ->
            val buffer = ByteArray(4 * 1024) // buffer size
            while (true) {
                val byteCount = input.read(buffer)
                if (byteCount < 0) break
                output.write(buffer, 0, byteCount)
            }
            output.flush()
        }
    }
}

//https://stackoverflow.com/a/56455963/3090120
@Throws(IOException::class)
fun getFileFromAssets(context: Context, fileName: String): File =
    File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

fun getLatLngBounds(file: File): LatLngBounds {

    Log.d("getLatLngBounds", "absolutePath = ${file.absoluteFile}")

    val openDatabase =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    val cursor = openDatabase.query(
        "metadata",
        arrayOf("name", "value"),
        "name=?",
        arrayOf("bounds"),
        null,
        null,
        null,
    )
    cursor?.moveToFirst()
    val boundsStr = cursor.getString(1).split(",")

    Log.d("showMBTilesFile", "boundsStr = $boundsStr")

    cursor.close()
    openDatabase.close()

    return LatLngBounds
        .Builder()
        .include(LatLng(boundsStr[1].toDouble(), boundsStr[0].toDouble()))
        .include(LatLng(boundsStr[3].toDouble(), boundsStr[2].toDouble()))
        .build()
}

fun getMinZoom(file: File): Int {

    Log.d("getLatLngBounds", "absolutePath = ${file.absoluteFile}")

    val openDatabase =
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    val cursor = openDatabase.query(
        "metadata",
        arrayOf("name", "value"),
        "name=?",
        arrayOf("minzoom"),
        null,
        null,
        null,
    )
    cursor?.moveToFirst()
    val minZoomLevel = cursor.getString(1)

    Log.d("showMBTilesFile", "minZoomLevel = $minZoomLevel")

    cursor.close()
    openDatabase.close()

    return minZoomLevel.toInt()
}

//https://stackoverflow.com/a/2549222/3090120
fun InputStream.readToString(): String {
    val r = BufferedReader(InputStreamReader(this))
    val total = StringBuilder("")
    var line: String?
    while (r.readLine().also { line = it } != null) {
        total.append(line).append('\n')
    }
    return total.toString()
}

fun Context.convertDpToPixel(dp: Int): Float {
    val resources = this.resources
    val metrics = resources.displayMetrics
    return dp * (metrics.densityDpi / 160f)
}

//Passing color, source, layer etc since it will be different for actual bbox and limited bbox
fun showBoundsArea(
    loadedMapStyle: Style,
    bounds: LatLngBounds,
    color: Int,
    sourceId: String,
    layerId: String,
    opacity: Float
) {
    val outerPoints: MutableList<Point> = ArrayList()

    outerPoints.add(Point.fromLngLat(bounds.northWest.longitude, bounds.northWest.latitude))
    outerPoints.add(Point.fromLngLat(bounds.northEast.longitude, bounds.northEast.latitude))
    outerPoints.add(Point.fromLngLat(bounds.southEast.longitude, bounds.southEast.latitude))
    outerPoints.add(Point.fromLngLat(bounds.southWest.longitude, bounds.southWest.latitude))
    outerPoints.add(Point.fromLngLat(bounds.northWest.longitude, bounds.northWest.latitude))

    loadedMapStyle.removeLayer(layerId)
    loadedMapStyle.removeSource(sourceId)

    loadedMapStyle.addSource(
        GeoJsonSource(
            sourceId,
            Polygon.fromLngLats(mutableListOf(outerPoints.toMutableList()))
        )
    )
    loadedMapStyle.addLayer(
        FillLayer(layerId, sourceId).withProperties(
            fillColor(color),
            fillOpacity(opacity)
        )
    )
}

/*
Wrote about this in detail over here
https://medium.com/@ty2/how-to-limit-the-map-window-to-a-bounding-box-for-mapbox-maplibre-e504d3df1ae4
*/
fun MapboxMap.limitViewToBounds(bounds: LatLngBounds) {

    val newBoundsHeight = bounds.latitudeSpan - projection.visibleRegion.latLngBounds.latitudeSpan
    val newBoundsWidth = bounds.longitudeSpan - projection.visibleRegion.latLngBounds.longitudeSpan

    val leftTopLatLng = LatLng(
        bounds.latNorth - (bounds.latitudeSpan - newBoundsHeight) / 2,
        bounds.lonEast - (bounds.longitudeSpan - newBoundsWidth) / 2 - newBoundsWidth,
    )

    val rightBottomLatLng = LatLng(
        bounds.latNorth - (bounds.latitudeSpan - newBoundsHeight) / 2 - newBoundsHeight,
        bounds.lonEast - (bounds.longitudeSpan - newBoundsWidth) / 2,
    )

    val newBounds = LatLngBounds.Builder()
        .include(leftTopLatLng)
        .include(rightBottomLatLng)
        .build()

    setLatLngBoundsForCameraTarget(newBounds)

    //Showing limited bbox
    /*
    getStyle {
        showBoundsArea(it, newBounds, Color.GRAY, "source-id-2", "layer-id-2", 0.4f)
    }
    */
}