package com.example.smartgardening // Hãy đảm bảo tên package trùng với project của bạn

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        btnSignUp.setOnClickListener {
            Toast.makeText(this, "Đang khởi tạo tài khoản...", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }, 1500)
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Đang kết nối Google...", Toast.LENGTH_SHORT).show()
        }
    }
}