package com.abhiank.offline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import java.io.*

class MainActivity : AppCompatActivity() {

    private val mapView: MapView by lazy { findViewById(R.id.mapView) }
    private lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, null)
        setContentView(R.layout.activity_main)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { it ->
            map = it

            val styleJsonInputStream = assets.open("bright.json")

            //Creating a new file to which to copy the json content to
            val dir = File(filesDir.absolutePath)
            val styleFile = File(dir, "bright.json")
            //Copying the original JSON content to new file
            copyStreamToFile(styleJsonInputStream, styleFile)

            //Getting reference to mbtiles file in assets
            val mbtilesFile = getFileFromAssets(this, "india_coimbatore.mbtiles")
            val bounds = getLatLngBounds(mbtilesFile)

            //Replacing placeholder with uri of the mbtiles file
            val newFileStr = styleFile.inputStream().readToString()
                .replace("___FILE_URI___", "mbtiles://${Uri.fromFile(mbtilesFile)}")

            //Writing new content to file
            val gpxWriter = FileWriter(styleFile)
            val out = BufferedWriter(gpxWriter)
            out.write(newFileStr)
            out.close()

            //Setting the map style using the new edited JSON file
            map.setStyle(Style.Builder().fromUri(Uri.fromFile(styleFile).toString()))
            //Setting camera view over the mbtiles area
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
        }
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

    cursor.close()
    openDatabase.close()

    return LatLngBounds
        .Builder()
        .include(LatLng(boundsStr[1].toDouble(), boundsStr[0].toDouble()))
        .include(LatLng(boundsStr[3].toDouble(), boundsStr[2].toDouble()))
        .build()
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