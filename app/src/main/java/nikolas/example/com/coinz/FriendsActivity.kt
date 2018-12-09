package nikolas.example.com.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_friends.*
import kotlin.math.roundToInt
import android.view.ViewGroup
import android.widget.*
import nikolas.example.com.coinz.R.id.friends_button_sendOne


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

        val editText = findViewById<EditText>(R.id.username_editText_friends)
        val spare =findViewById<TextView>(R.id.friends_textView_spare)
        val listView=findViewById<ListView>(R.id.list_view)
        var list = mutableListOf<Model>()

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
                        if (numCollectedCoins>=25){
                            list.add(Model(collectedCoinId))
                        }
                        val pair = Pair<String, Int>(collectedCoinId, (it.getDouble("gold")!!.roundToInt()))
                        collectedCoins.add(pair)
                    }
                }
                listView.adapter=MyListAdapter(this,R.layout.row,list)
                collectedCoins = sortListPairDesc(collectedCoins)


                if (numCollectedCoins > 25) {
                    numSpareChange = numCollectedCoins - 25
                    val mostValuableCoinId=collectedCoins[25]
                    println(mostValuableCoinId)


                } else {
                    numSpareChange = 0
                }
                spare.text="$numSpareChange"


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
                            var coinToSendId=collectedCoins[25].first
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
                                    spare.text="$numSpareChange"
                                    collectedCoins.removeAt(25)
                                    list.removeAt(0)
                                    listView.adapter=MyListAdapter(this,R.layout.row,list)



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

