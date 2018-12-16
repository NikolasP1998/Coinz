package nikolas.example.com.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import kotlinx.android.synthetic.main.activity_achievements.*
import kotlin.math.roundToInt

@Suppress("UNCHECKED_CAST")
class AchievementsActivity : AppCompatActivity() {

    private lateinit var ownUsername: String
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var coinsLeftToCollect = true
    var found = false
    private var foundMarker = MarkerOptions()
    companion object {
        var collectedCoins=ArrayList<MarkerOptions>()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        supportActionBar?.title = "achievements"



        val markers = MainActivity.markers
        if (markers.isEmpty()) {
            coinsLeftToCollect = false

        }

        val coinInd = MainActivity.coinInd
        val rates=MainActivity.rates
        val editTextID = findViewById<EditText>(R.id.achievements_editText_id)
        if (!coinsLeftToCollect) {
            editTextID.visibility = View.GONE
        }
        val textViewWand = findViewById<TextView>(R.id.achievements_TextView_numberOfWands)
        val progressDays = findViewById<ProgressBar>(R.id.achievements_progressBar_days)
        val progressGold = findViewById<ProgressBar>(R.id.achievements_progressBar_gold)
        val progressSpareChange = findViewById<ProgressBar>(R.id.achievements_progressBar_spareChange)

        val userid = FirebaseAuth.getInstance().uid
        db.collection("usernames").document("$userid").get().addOnSuccessListener {
            ownUsername = it.getString("username")!!

            db.collection("userData").document(ownUsername).get().addOnSuccessListener {
                if (!coinsLeftToCollect) {
                    val msg ="ALL COINS COLLECTED"
                    textViewWand.text = msg
                } else {
                    textViewWand.text = it.getDouble("wand")!!.toInt().toString()
                }

                progressDays.progress = it.getDouble("achievementDays")!!.toInt()
                progressGold.progress = it.getDouble("achievementGold")!!.toInt()
                progressSpareChange.progress = it.getDouble("achievementSpareChange")!!.toInt()
            }
            achievents_button_collect.setOnClickListener {
                if (!coinsLeftToCollect) {
                    Toast.makeText(this, "All coins collected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if ((textViewWand.text.toString().toInt())==0){
                    Toast.makeText(this, "No more magic wand available", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val id = editTextID.text.toString().toIntOrNull()
                if (id == null) {
                    Toast.makeText(this, "Enter a number", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if ((id > 49) || (id < 0)) {
                    Toast.makeText(this, "Enter a number between 0 and 49", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                found = false
                // coins left to collect , markers are not empty
                markers.forEach {
                    val markerId = it.title.toInt()
                    if (id == markerId) {
                        found = true
                        foundMarker = it


                        return@forEach
                    }
                }

                if (found) {
                    if (collectedCoins.contains(foundMarker)){
                        Toast.makeText(this, "select another coin", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    var wand = textViewWand.text.toString().toInt()
                    wand--
                    textViewWand.text = wand.toString()
                    db.collection("userData").document(ownUsername).update("wand", wand).addOnSuccessListener {
                        Log.d("achievements", "wand value updates to database")
                    }

                    val idCollected = coinInd!![foundMarker.title.toInt()]
                    val coinVal = foundMarker.snippet.substringAfter(":").toDouble()
                    val curr = foundMarker.snippet.substringBefore(":")
                    val gold = (coinVal * rates[curr]!!).roundToInt()
                    val coinCollected = Coin(idCollected, coinVal, curr, gold)
                    collectedCoins.add(foundMarker)


                    db.collection(userid!!).document(idCollected).set(coinCollected).addOnSuccessListener {


                        Toast.makeText(this, "Coin collected", Toast.LENGTH_LONG).show()

                    }


                } else {
                    Toast.makeText(this, "Coin was already collected", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
