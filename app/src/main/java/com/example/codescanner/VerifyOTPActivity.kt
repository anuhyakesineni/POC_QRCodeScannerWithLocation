package com.example.codescanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class VerifyOTPActivity : AppCompatActivity() {
    var phoneNo: String? = null
    var verificationId: String = ""
    var otp: Int = 0
    private lateinit var auth: FirebaseAuth
    lateinit var mCallBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otpactivity)

        val b: Bundle? = intent.extras
        phoneNo = "+91" + b!!.getString("PhoneNo")
        val sendOTP = findViewById<Button>(R.id.sendOTP)
        val verifyOTP = findViewById<Button>(R.id.verifyOTP)
        val otpEditText = findViewById<EditText>(R.id.otp)
        auth = Firebase.auth
        val currentUser = auth.currentUser

        mCallBack = object : OnVerificationStateChangedCallbacks() {
            // below method is used when
            // OTP is sent from Firebase
            override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                // when we receive the OTP it
                // contains a unique id which
                // we are storing in our string
                // which we have already created.
                verificationId = s
            }

            // this method is called when user
            // receive OTP from Firebase.
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                // below line is used for getting OTP code
                // which is sent in phone auth credentials.
                val code = phoneAuthCredential.smsCode

                // checking if the code
                // is null or not.
                if (code != null) {
                    // if the code is not null then
                    // we are setting that code to
                    // our OTP edittext field.
                    otpEditText.setText(code)

                    // after setting this code
                    // to OTP edittext field we
                    // are calling our verifycode method.
                    verifyCode(code)
                }
            }

            // this method is called when firebase doesn't
            // sends our OTP code due to any error or issue.
            override fun onVerificationFailed(e: FirebaseException) {
                // displaying error message with firebase exception.
                Toast.makeText(this@VerifyOTPActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }



        sendOTP.setOnClickListener {
//            sendOTP_func()
            sendVerificationCode(phoneNo!!)
            Toast.makeText(this, "OTP sent", Toast.LENGTH_SHORT).show()
        }

        verifyOTP.setOnClickListener {
            val s = otpEditText.text.toString()
            if (s.isEmpty())
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
            else
                verifyCode(s)

        }
    }


    fun sendVerificationCode(number: String) {
        // this method is used for getting
        // OTP on user phone number.
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number) // Phone number to verify
            .setTimeout(120L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(mCallBack)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode(code: String) {
        // below line is used for getting
        // credentials from our verification id and code.
        val credential = PhoneAuthProvider.getCredential(verificationId, code)

        // after getting credential we are
        // calling sign in method.
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        // inside this method we are checking if
        // the code entered is correct or not.
        auth.signInWithCredential(credential)
            .addOnCompleteListener(OnCompleteListener<AuthResult?> { task ->
                if (task.isSuccessful) {
                    // if the code is correct and the task is successful
                    // we are sending our user to new activity.

                    Toast.makeText(this, "Handsake is Successful", Toast.LENGTH_LONG).show()


                } else {
                    // if the code is not correct then we are
                    // displaying an error message to the user.
                    Toast.makeText(
                        this, task.exception?.message, Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    fun generateRandomOTP(): Int {
        val r = Random
        var s = ""
        for (i in 0..3) {
            var n = r.nextInt(9)
            if (n == 0 && i == 0) {
                continue
            }
            s = s + n
        }

        var otp = s.toInt()
        Toast.makeText(this, "$otp", Toast.LENGTH_SHORT).show()
        return otp
    }

    fun sendOTP_func() {
        otp = generateRandomOTP()
        try {

            val apiKey =
                "apikey=" + "NzQ2OTRmMzY3MTRjNzI1NTMwNDI1OTc5NTM2ZDRlNDI="

            val message = "&message=Hey, Your OTP is $otp"
            val sender = "&sender=" + "TXTLCL"
            val numbers = "&numbers=$phoneNo"

            val conn: HttpURLConnection =
                URL("https://api.textlocal.in/send/?").openConnection() as HttpURLConnection
            val data = apiKey + numbers + message + sender
            conn.setDoOutput(true)
            conn.setRequestMethod("POST")
            conn.setRequestProperty("Content-Length", Integer.toString(data.length))
            conn.getOutputStream().write(data.toByteArray(charset("UTF-8")))
            val rd = BufferedReader(InputStreamReader(conn.getInputStream()))
            val stringBuffer = StringBuffer()
            var line: String?
            while (rd.readLine().also { line = it } != null) {
                stringBuffer.append(line)
            }
            rd.close()
        } catch (e: Exception) {
            e.message?.let { Log.d("Error", it) }
        }
    }

    fun generateOTP(): String {
        val randomPin = (Math.random() * 9000).toInt() + 1000
        return randomPin.toString()
    }

}
