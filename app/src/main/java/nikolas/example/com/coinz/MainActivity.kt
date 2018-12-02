package nikolas.example.com.coinz

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
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
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.ArrayList
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import kotlin.collections.HashMap
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(),OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var downloadDate = "" // Format: YYYY/MM/DD
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private  var locationEngine: LocationEngine?=null
    private  var locationLayerPlugin: LocationLayerPlugin?=null
    val arg_for_download = DownloadCompleteRunner
    val link = DownloadFileTask(arg_for_download)
    var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    // variables for markers declared below
    private lateinit var markers: ArrayList<MarkerOptions>
    private lateinit var markersList: ArrayList<Marker>
    private lateinit var user: FirebaseUser
    private var numCollectedCoins = 0
    private var rates = HashMap<String,Double>()
    private var username:String?=null
    private  var coinInd:ArrayList<String>?=ArrayList()
    var mAuth:FirebaseAuth?=null



    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)



         mAuth=FirebaseAuth.getInstance()
         var uid=mAuth?.uid
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
            setSupportActionBar(my_toolbar)


            user = FirebaseAuth.getInstance()?.currentUser!!
            var userid = user.uid
            //fetch username to access correct firebase directory
            db.collection("usernames").document("$userid").get().addOnSuccessListener {

                username=it.getString("username")
            }









            Mapbox.getInstance(applicationContext, getString(R.string.access_token))
            mapView = findViewById(R.id.mapboxMapView)

            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)
            // use "" as the default value (this might be the first time the app is run)
            val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
            downloadDate = settings.getString("lastDownloadDate", "")
            val currentDate = getCurrentDateTime().toString("yyyy/MM/dd")
            if (downloadDate != currentDate) {
                println("newDay")
                downloadDate = currentDate
                link.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson")
                val editor = settings.edit()
                editor.putString("lastDownloadDate", downloadDate)
                //Apply the edits!
                editor.apply()

                db.collection("userData").document("$username").get().addOnSuccessListener {
                    var collectedCoins=it.get("collectedCoins")
                     if (collectedCoins == null) {
                        //no coins collected , return
                         numCollectedCoins=0
                    }
                    else {
                         collectedCoins=collectedCoins as ArrayList<String>
                         numCollectedCoins=collectedCoins.size
                     }
                    db.collection("$userid").get().addOnSuccessListener(){

                        if (numCollectedCoins>0) {// user collected some coins , proceed to award gold

                            var dailyGold = 0
                            var collectedCoins = ArrayList<Pair<String, Int>>()
                            it.forEach() {
                                var collectedCoinId=it.getString("coinid")!!
                                if (!(collectedCoinId.startsWith("RECIEVED"))){
                                    var pair = Pair<String, Int>(collectedCoinId!!, (it.getDouble("gold")!!.roundToInt()))
                                    collectedCoins.add(pair)
                                }
                                else{
                                    dailyGold=dailyGold+it.getDouble("gold")!!.roundToInt()
                                }


                            }
                            collectedCoins = sortListPairDesc(collectedCoins)
                            var numDepositedCoins = min(25, numCollectedCoins)
                            println("deposited coins:$numDepositedCoins")
                            for (i in 1..numDepositedCoins) {
                                dailyGold += collectedCoins.get(i-1).second


                            }
                            println("gold for the day:$dailyGold")
                            db.collection("userData").document("$username").get().addOnSuccessListener {
                                var totalGold=it.getDouble("totalGold")!!.roundToInt()
                                totalGold=totalGold+dailyGold
                                it.reference.update("totalGold",totalGold)

                            }

                        }
                    }

                }

                //update firebase



            }
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
                val intent = Intent(this, FriendsActivity::class.java)
                Log.d("MainActivity","signed out succesfully")

                startActivity(intent)
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
            R.id.action_signout->{
                mAuth?.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                Log.d("MainActivity","signed out succesfully")

                startActivity(intent)
                return true
            }
            R.id.action_wand->{
                Toast.makeText(this,"wand", Toast.LENGTH_LONG).show()
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
            // Make location information available
            enableLocation()
            markers = viewMarkers()



            var userid=user.uid
            db?.collection("$userid")?.get()?.addOnSuccessListener {
                val markerIds = markers.map { marker -> marker.title } as ArrayList<String>

                numCollectedCoins = it.size()
                println("collected coins:$numCollectedCoins")
                it.forEach {
                    var id = it.getString("coinid")
                    var i =coinInd?.indexOf(id).toString()
                    if (markerIds.contains(i)) {//coin already collected , remove marker
                        //map?.removeMarker()
                        markers.removeAt(markerIds.indexOf(i))

                        markerIds.remove(i)

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
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else { locationEngine?.addLocationEngineListener(this) }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {

        if (mapView == null) {Log.d(tag, "mapView is null") }
        else {
            if (map == null) {Log.d(tag,"map is null") }
            else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!,map!!,locationEngine)
                locationLayerPlugin?.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                    var lifecycle :Lifecycle=getLifecycle()
                    lifecycle.addObserver(this)
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
                    var id =coinInd?.get(m.title.toInt())
                    var coinVal=m.snippet.substringAfter(": ").toDouble()
                    var curr=m.snippet.substringBefore(":")
                    var gold=(coinVal*rates.get(curr)!!).roundToInt()
                    var coinCollected=Coin(id!!,coinVal,curr,gold)
                    var userid=user?.uid
                    numCollectedCoins=numCollectedCoins+1


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
                    db.collection("userData")?.document("$username").get().addOnSuccessListener {
                        var collectedCoins =it.get("collectedCoins") as ArrayList<String>?
                        collectedCoins?.add(id)
                        it.reference.update("collectedCoins",collectedCoins)

                    }
                }

            }
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
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
        if (locationEngine != null) {

            try {
                locationEngine?.requestLocationUpdates();
            } catch (ignored: SecurityException) { }
            locationEngine?.addLocationEngineListener(this);
            //Restore preferences
            val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

            // use "" as the default value (this might be the first time the app is run)
            downloadDate = settings.getString("lastDownloadDate", "")

            //Write a message to "logcat" (for debugging purposes)
            Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'")
        }
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        if(locationEngine != null){
            locationEngine?.removeLocationEngineListener(this);
            locationEngine?.removeLocationUpdates();
        }
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
        println("DESTROY")
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
        var ratesStr =JSONObject(str).getJSONObject("rates")
        var shilRate=ratesStr.getString("SHIL").toDouble()
        var dolRate=ratesStr.getString("DOLR").toDouble()
        var quidRate=ratesStr.getString("QUID").toDouble()
        var penyRate=ratesStr.getString("PENY").toDouble()
        rates.put("SHIL",shilRate)
        rates.put("DOLR",dolRate)
        rates.put("QUID",quidRate)
        rates.put("PENY",penyRate)
        println("rates updated")
        db.collection("userData").document("$username").update("latestRates",rates).addOnSuccessListener {
            println("rates updated")
        }
        var json = FeatureCollection.fromJson(str).features()
        //index coins from 1 to 50 , to allow for use of "magic wand" bonus feature
        //to do this we add the marker ids into an arrayList , the position of the id in the list is the index

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
            coinInd?.add(id)


            var i=coinInd!!.indexOf(id)

            var value = prop.get("value").asString

            var mark = MarkerOptions().title(i.toString()).snippet(currency + ": $value").position(x).icon(findIcon(currency))
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


    fun sortListPairDesc(list: ArrayList<Pair<String, Int>>): ArrayList<Pair<String, Int>> {
        val result = ArrayList(list.sortedWith(compareBy({ it.second })))
        return (result  )
    }



}

class Coin(val coinid:String,val coinvalue:Double,val curr: String, val gold:Int)