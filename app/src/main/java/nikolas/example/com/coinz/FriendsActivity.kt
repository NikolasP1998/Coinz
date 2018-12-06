package nikolas.example.com.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_friends.*
import kotlin.math.roundToInt





class FriendsActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var recieverUsername:String
    private lateinit var ownUsername:String
    private lateinit var recieverId:String
    private var numCollectedCoins:Int=0
    private var numSpareChange:Int=0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)
        supportActionBar?.title = "Send coins to friends"

        val txtview = findViewById<TextView>(R.id.coin_textView_friends)
        val editText = findViewById<EditText>(R.id.username_editText_friends)


        val userid = FirebaseAuth.getInstance().uid
        println(userid)
        var collectedCoins = ArrayList<Pair<String, Int>>() //arraylist of pairs to add coin id and value to sort
        //check that user has spare change and display number
        db.collection("$userid").get().addOnSuccessListener {
            numCollectedCoins = 0
            if (it.isEmpty) {
                return@addOnSuccessListener
            } else {
                //find number of coins in users wallet (collected but not sent)
                //coins are added to an arraylist of pairs , to avoid second iteration
                it.forEach {
                    val collectedCoinId = it.getString("coinid")!!
                    if (!(collectedCoinId.startsWith("RECIEVED"))) {
                        numCollectedCoins++
                        val pair = Pair<String, Int>(collectedCoinId, (it.getDouble("gold")!!.roundToInt()))
                        collectedCoins.add(pair)
                    }
                }
                collectedCoins = sortListPairDesc(collectedCoins)
                val mostValuableCoinId=collectedCoins[25]
               println(mostValuableCoinId)

                if (numCollectedCoins > 25) {
                    numSpareChange = numCollectedCoins - 25

                } else {
                    numSpareChange = 0
                }
                val display ="number of spare : $numSpareChange "
                txtview.text =display


            }
        }
            //fetch username to ensure that user is not sending coins to himself
        db.collection("usernames").document("$userid").get().addOnSuccessListener {
            ownUsername = it.getString("username")!!
        }
        friends_button_sendOne.setOnClickListener {
            recieverUsername = editText.text.toString()
            if (numSpareChange==0){
                Toast.makeText(this, "no spare change", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

              if (recieverUsername == ownUsername) {
                    Toast.makeText(this, "cannot send spare change to yourself", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {

                    db.collection("usernames").get().addOnSuccessListener {
                        var found = false
                        it.forEach {
                            val name = it.getString("username")

                            if (name == recieverUsername) {
                                found = true
                                recieverId = it.getString("uid")!!

                            }


                        }
                        if (!found) {
                            Toast.makeText(this, "username not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        } else {//send coin
                            var coinToSendId=collectedCoins[26].first
                            db.collection("$userid").document(coinToSendId).get().addOnSuccessListener {
                                //remove from database , add to sent coins
                                db.collection("userData").document(ownUsername).get().addOnSuccessListener {
                                    @Suppress("UNCHECKED_CAST")

                                    val sentCoins =it.get("sentCoins") as ArrayList<String>?
                                    sentCoins?.add(coinToSendId)
                                    it.reference.update("sentCoins",sentCoins)
                                }
                                //append RECIEVED to distinguish coin
                                coinToSendId="RECIEVED$coinToSendId"
                                val coinToSend = Coin(coinToSendId,it.getDouble("coinvalue")!!,it.getString("curr")!!,it.getDouble("gold")!!.roundToInt())
                                it.reference.delete()
                                db.collection(recieverId).document(coinToSendId).set(coinToSend).addOnSuccessListener {

                                    Toast.makeText(this, "coin sent", Toast.LENGTH_SHORT).show()
                                    numSpareChange--

                                    //update text box
                                    val display ="number of spare : $numSpareChange "
                                    txtview.text =display

                                }

                            }



                        }
                    }


                }
            }


    }
    fun sortListPairDesc(list: ArrayList<Pair<String, Int>>): ArrayList<Pair<String, Int>> {
        val result = ArrayList(list.sortedWith(compareBy({ it.second })))
        return (result  )
    }
}