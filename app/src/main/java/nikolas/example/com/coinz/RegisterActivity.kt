package nikolas.example.com.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_register.*


class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.title="Register to Coinz"

        register_button_register.setOnClickListener{
            val email= email_edittext_register.text.toString()
            val password=password_edittext_register.text.toString()
            val username = username_editText_register.text.toString()
            if (email.isEmpty()||password.isEmpty()||username.isEmpty()){
                Toast.makeText(this,"please enter text in all fields",Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }
                //fetch used usernames from firebase to see if selected username is available
                val db = FirebaseFirestore.getInstance()
                db.collection("usernames").get().addOnSuccessListener {
                    it.forEach {
                        val name = it.getString("username")
                        Log.d("Register", "name is $name")
                        if (name == username) {
                            //returns , username is in use
                            Toast.makeText(this, "username already in use", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }
                    //username not in use , proceed to registering user
                    //note that email availability is checked by firebase and handled by exception below
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
                                intent.flags=Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //Clear activity stack
                                startActivity(intent)

                            }
                            .addOnFailureListener{
                                Log.d("Main:","Failed to create user:${it.message}")
                                Toast.makeText(this,"Failed to create user:${it.message}",Toast.LENGTH_SHORT).show()
                                println("Failed to create user:${it.message}")
                            }


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
        val username = username_editText_register.text.toString()
        val email =email_edittext_register.text.toString()
        val user=User(uid,username,email,0.0,ArrayList(),"noGoal","noGoal")
        db.collection("userData").document(username)
                .set(user)
                .addOnSuccessListener {
                    Log.d("Register","user added")
                }
        val uname=Username(uid,username)
        db.collection("usernames").document(uid)

                .set(uname)
                .addOnSuccessListener {
                    println("user added")
                }

        }






    }






data class User(val uid:String,val username:String,val email:String,val totalGold:Double,val sentCoins:ArrayList<String>,val dailyGoal:String,val nextDailyGoal:String)

data class Username(val uid:String,val username: String)
