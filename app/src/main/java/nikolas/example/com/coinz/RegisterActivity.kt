package nikolas.example.com.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_register.*

import kotlinx.android.synthetic.main.content_register.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.title="Register"

        register_button_register.setOnClickListener{
            val email= email_edittext_register.text.toString()
            val password=password_edittext_register.text.toString()
            val username = username_editText_register.text.toString()
            if (email.isEmpty()||password.isEmpty()){
                Toast.makeText(this,"please enter text in email and password",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("RegisterActivity","$email,$password,$username")
            //Firebase Authentication to create user with email and password
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener{
                        if (!it.isSuccessful) return@addOnCompleteListener
                        //else if successful :
                        Log.d("Main","Succesfully logged in with uid:${it.result!!.user.uid}")
                        saveUserToFirebaseDatabase()
                        //login ,updateUI
                        val intent = Intent(this,MainActivity::class.java)
                        intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)

                    }
                    .addOnFailureListener{
                        Log.d("Main:","Failed to create user:${it.message}")
                        Toast.makeText(this,"failed to create user:${it.message}",Toast.LENGTH_SHORT).show()
                    }

        }

        already_have_account_textview_register.setOnClickListener{
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
        }

    }


    private fun saveUserToFirebaseDatabase() {
        val db = FirebaseFirestore.getInstance()

        val uid =FirebaseAuth.getInstance().uid ?:""

        val user=User(uid,username_editText_register.text.toString(),email_edittext_register.text.toString(),0.0)
        db.collection("users")
                .add(user)
                .addOnSuccessListener {
                    Log.d("alo","user added")
                }


    }




}

class User(val uid:String,val username:String,val email:String,val totalGold:Double)


