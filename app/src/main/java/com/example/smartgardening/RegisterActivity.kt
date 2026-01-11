package com.example.smartgardening

import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgardening.firebase.FirebaseAuthManager
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val ivToggleConfirmPassword = findViewById<ImageView>(R.id.ivToggleConfirmPassword)

        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)

        // Back
        btnBack.setOnClickListener { finish() }

        // 👁 Toggle password
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePassword(etPassword, ivTogglePassword, isPasswordVisible)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePassword(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible)
        }

        // Đăng ký
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                toast("Vui lòng điền đầy đủ thông tin!")
                return@setOnClickListener
            }

            if (password.length < 6) {
                toast("Mật khẩu phải từ 6 ký tự trở lên")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                toast("Mật khẩu xác nhận không khớp")
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false
            toast("Đang tạo tài khoản...")

            FirebaseAuthManager.register(email, password) { success, message ->
                btnSignUp.isEnabled = true
                if (success) {
                    toast("Đăng ký thành công!")
                    finish()
                } else {
                    toast("Lỗi: $message")
                }
            }
        }

        btnGoogle.setOnClickListener {
            toast("Chức năng đang phát triển")
        }
    }

    private fun togglePassword(
        editText: EditText,
        imageView: ImageView,
        isVisible: Boolean
    ) {
        if (isVisible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye_on)
        } else {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_eye_off)
        }
        editText.setSelection(editText.text.length)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
