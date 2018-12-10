package nikolas.example.com.coinz


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
private var auth:FirebaseAuth=FirebaseAuth.getInstance()
class LoginActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.title="Login"

        no_account_textView_login.setOnClickListener{
            val intent = Intent(this,RegisterActivity::class.java)
            startActivity(intent)
        }
        login_button_login.setOnClickListener{
            val email=email_editText_login.text.toString()
            val password=password_editText_login.text.toString()
            if (email.isEmpty()||password.isEmpty()){
                Toast.makeText(this,"please enter text in all fields",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email,password)

                    .addOnCompleteListener {
                        if (it.isSuccessful){
                            // Sign in success, update UI with signed-in user's information
                            Log.d("LoginActivity", "signInWithEmail:success")
                            //updateUi
                            val intent = Intent(this,MainActivity::class.java)
                            intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

                            startActivity(intent)
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            Log.e( "LoginActivity", "$it.exception")
                            Toast.makeText(this@LoginActivity, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show()

                        }

                    }
        }
    }

}