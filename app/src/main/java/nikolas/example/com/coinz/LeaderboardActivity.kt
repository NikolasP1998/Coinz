package nikolas.example.com.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import com.google.firebase.firestore.FirebaseFirestore

class LeaderboardActivity : AppCompatActivity() {
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        supportActionBar?.title = "Leaderboard"
        val listView=findViewById<ListView>(R.id.list_leaderboard)
        val list = mutableListOf<LeaderboardData>()
        //loop through all users to sort them by gold
        var usersList = ArrayList<Pair<String,Int>>()
        db.collection("userData").get().addOnSuccessListener{
            it.forEach {
                val username = it.getString("username")!!
                val gold = it.getDouble("totalGold")!!.toInt()
                usersList.add(Pair(username, gold))
            }
            usersList=sortListPair(usersList) //sort users by gold

            var i=1
            usersList.forEach {
                val userData=LeaderboardData("$i    ${it.first}      ${it.second}")
                list.add(userData)
                i++
            }

            listView.adapter=LeaderboardListAdapter(this,R.layout.row,list)

        }
    }
}
private fun sortListPair(list: ArrayList<Pair<String, Int>>): ArrayList<Pair<String, Int>> {
    val result = ArrayList(list.sortedWith(compareByDescending{ it.second }))
    return (result  )
}
class LeaderboardData(val userData:String)
