package com.example.smartgardening

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        btnSignIn.setOnClickListener {
            Toast.makeText(this, "Đang xử lý đăng nhập...", Toast.LENGTH_SHORT).show()
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Đang mở Google Login", Toast.LENGTH_SHORT).show()
        }
    }
}