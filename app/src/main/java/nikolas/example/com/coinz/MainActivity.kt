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
import android.graphics.Color
import android.os.CountDownTimer
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.ArrayList
import android.widget.Toast
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity(),OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var downloadDate = "" // Format: YYYY/MM/DD
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin
    val arg_for_download = DownloadCompleteRunner
    val link = DownloadFileTask(arg_for_download)
    var handler:Handler= Handler()
    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    // variables for markers declared below
    private lateinit var markers: ArrayList<MarkerOptions>
    private lateinit var markersList: ArrayList<Marker>
    private lateinit var user: FirebaseUser
    private var numCollectedCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)


        val uid=FirebaseAuth.getInstance().uid
        Log.d("Login","$uid")
        // if not logged in , return to register screen
        if (uid==null) {
            val intent = Intent(this, RegisterActivity::class.java)
            //finish()
            Log.d("Login2", "$intent")
            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //clear activity stack
            startActivity(intent)

        }
        else {
            setContentView(R.layout.activity_main)

            val date = getCurrentDateTime()
            downloadDate = date.toString("yyyy/MM/dd")
            link.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson")
            


            //setSupportActionBar(toolbar)
            user = FirebaseAuth.getInstance()?.currentUser!!




            Mapbox.getInstance(applicationContext, getString(R.string.access_token))
            mapView = findViewById(R.id.mapboxMapView)

            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)



        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater=menuInflater
        inflater.inflate(R.menu.menu_main,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.action_friends->{
                Toast.makeText(this,"friends", Toast.LENGTH_LONG).show()
                return true
            }
            R.id.action_lead->{
                Toast.makeText(this,"lead", Toast.LENGTH_LONG).show()
                return true
            }
            R.id.action_goal->{
                Toast.makeText(this,"Goal", Toast.LENGTH_LONG).show()
                return true
            }
            else->{
                return super.onOptionsItemSelected(item)
            }

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
            markers = viewMarkers()
            var userid = user?.uid
            db?.collection("$userid")?.get()?.addOnSuccessListener {
                val markerIds = markers.map { marker -> marker.title } as ArrayList<String>

                numCollectedCoins = it.size()
                println("collected coins:$numCollectedCoins")
                it.forEach {
                    var id = it.getString("coinid")
                    println("query coin id :$id")
                    if (markerIds.contains(id)) {//coin already collected , remove marker
                        //map?.removeMarker()
                        println("before:${markers.size} ")
                        markers.removeAt(markerIds.indexOf(id))
                        println("after:${markers.size} ")

                        markerIds.remove(id)

                        Log.d("MarkerRemoval", "Removed marker $id")

                    }


                }
                mapView?.getMapAsync { _ ->
                    markersList = map?.addMarkers(markers) as ArrayList

                }


            }
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
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
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
            originLocation = location
            setCameraPosition(originLocation)
            var userLoc=LatLng(location.latitude,location.longitude)
            for (m in markers){
                var markerPos =m.position
                if (userLoc.distanceTo(markerPos)<=25) {
                    var id =m.title
                    var coinVal=m.snippet.substringAfter(": ").toDouble()
                    var curr=m.snippet.substringBefore(":")
                    var coinCollected=Coin(id,coinVal,curr)
                    var userid=user?.uid
                    numCollectedCoins=numCollectedCoins+1
                    println("updated collected coins:$numCollectedCoins")

                    db?.collection("$userid")?.document(id)?.set(coinCollected)?.addOnSuccessListener {





                        Toast.makeText(this,"Coin collected", Toast.LENGTH_LONG).show()
                        markers.remove(m)

                        //update map
                        mapView?.getMapAsync{_->
                            markersList.forEach{
                                if (it.title==m.title){
                                    // numCollectedCoins=collected!!.size
                                    map?.removeMarker(it)
                                }
                            }
                        }


                    }?.addOnFailureListener{
                        exception: java.lang.Exception -> Toast.makeText(this,exception.toString(), Toast.LENGTH_LONG).show()

                    }
                }

            }
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


    fun viewMarkers() : ArrayList<MarkerOptions> {
        val list = ArrayList<MarkerOptions>()
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
            var id = prop.get("id").asString
            var value = prop.get("value").asString
            var mark = MarkerOptions().title(id).snippet(currency + ": $value").position(x).icon(findIcon(currency))
            list.add(mark)
        }

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
        Log.d("Login","$uid")
        // if not logged in , return to register screen
        if (uid==null) {
            val intent = Intent(this, RegisterActivity::class.java)
            finish()
            Log.d("Login2","$intent")
            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //clear activity stack
            startActivity(intent)
        }
    }



}

class Coin(val coinid:String,val coinvalue:Double,val curr: String)