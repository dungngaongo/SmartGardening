package com.example.smartgardening

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgardening.firebase.FirebaseAuthManager
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)


        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etLocation = findViewById<EditText>(R.id.etLocation)

        val btnSignUp = findViewById<MaterialButton>(R.id.btnSignUp)
        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)

        // 2. Xử lý sự kiện nút Back
        btnBack.setOnClickListener {
            finish()
        }

        // 3. Xử lý Đăng Ký
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val location = etLocation.text.toString().trim()

            // Validate dữ liệu (Kiểm tra rỗng)
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hiển thị thông báo chờ
            Toast.makeText(this, "Đang tạo tài khoản...", Toast.LENGTH_SHORT).show()
            btnSignUp.isEnabled = false // Khóa nút để tránh bấm nhiều lần

            // Gọi Firebase Auth
            FirebaseAuthManager.register(email, password) { isSuccess, message ->
                btnSignUp.isEnabled = true

                if (isSuccess) {
                    // Ở đây bạn có thể lưu thêm Name và Location vào Firestore (Bước sau)
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lỗi: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 4. Nút Google (Chưa xử lý logic)
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Chức năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }
}