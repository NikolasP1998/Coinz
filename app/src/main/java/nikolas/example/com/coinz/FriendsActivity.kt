package nikolas.example.com.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class FriendsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)
        supportActionBar?.title="Send coins to friends"
    }
}
