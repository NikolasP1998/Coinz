package nikolas.example.com.coinz

import android.content.Context
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity(),OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var downloadDate = "" // Format: YYYY/MM/DD
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    val arg_for_download = DownloadCompleteRunner
    val link = DownloadFileTask(arg_for_download)




    override fun onCreate(savedInstanceState: Bundle?) {
        println("1")
        super.onCreate(savedInstanceState)
        verifyUserIsLoggedIn()
        setContentView(R.layout.activity_main)
        //setSupportActionBar(toolbar)

        println("hiTest")

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        println("2")
        val date = getCurrentDateTime()
        downloadDate = date.toString("yyyy/MM/dd")
        link.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson")
        println("25")
        mapView?.getMapAsync {_ ->
            map?.addMarkers(viewMarkers())
        //println("55")
        }

    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            println("13")
            // Make location information available
            enableLocation()
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
            println("20")
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
            println("14")
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        println("21")
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            println("15")
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        println("15")
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
            println("16")
        } else { locationEngine.addLocationEngineListener(this) }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) {Log.d(tag, "mapView is null") }
        else {
            if (map == null) {Log.d(tag,"map is null") }
            else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!,map!!,locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                    println("17")
                }
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude),13.0))
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            println("18")
            originLocation = location
            setCameraPosition(originLocation)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag,"Permissions: $permissionsToExplain")
        //Present popup message or dialog
        println("19")
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        //Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        // use "" as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate","")

        //Write a message to "logcat" (for debugging purposes)
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'")
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        Log.d(tag,"[onStop] Storing lastDownloadDate of $downloadDate")

        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        //We need am Editor object to make preference changes
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        //Apply the edits!
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    /*override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView?.onSaveInstanceState()
    }*/

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }


    fun viewMarkers() : List<MarkerOptions> {
        val list: MutableList<MarkerOptions> = ArrayList()
        var str = File("/data/data/nikolas.example.com.coinz/files/coinzmap.geojson").readText(Charsets.UTF_8)
        var json = FeatureCollection.fromJson(str).features()
        json?.forEach {
            var temp = it.geometry()!!.toJson()
            var p = Point.fromJson(temp)
            var long = p.longitude()
            var lat = p.latitude()
            var x = LatLng(lat,long)
            var prop = it.properties()!!
            var symbol = prop.get("marker-symbol").asString
            var currency = prop.get("currency").asString
            var color = prop.get("marker-color").asString
            var mark = MarkerOptions().title(symbol).snippet(color).position(x).icon(findIcon(currency))
            list.add(mark)
        }
        println("testList $list")
        return list
    }

    fun findIcon(currency: String): Icon {
        var id = when (currency) {
            "DOLR" -> R.drawable.dolr
            "SHIL" -> R.drawable.shil
            "PENY" -> R.drawable.peny
            "QUID" -> R.drawable.quid
            else -> R.drawable.coin
        }
        return IconFactory.getInstance(this).fromResource(id)

    }
    private fun verifyUserIsLoggedIn() {
        val uid=FirebaseAuth.getInstance().uid
        // if not logged in , return to register screen
        if (uid==null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //clear activity stack
            startActivity(intent)
        }
    }


}
