package com.example.smartgardening

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton // Import thêm ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgardening.firebase.FirebaseAuthManager // Import file quản lý Auth
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. TỰ ĐỘNG ĐĂNG NHẬP
        // Nếu đã đăng nhập rồi thì vào thẳng Main luôn, không cần nhập lại
//        if (FirebaseAuthManager.isUserLoggedIn()) {
//            goToMainActivity()
//            return
//        }

        setContentView(R.layout.activity_login)

        // 2. ÁNH XẠ VIEW (Kết nối với giao diện)
        // Lưu ý: Phải chắc chắn trong file XML bạn đã thêm id cho 2 ô nhập liệu này
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val btnGoogle = findViewById<MaterialButton>(R.id.btnGoogle)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // 3. XỬ LÝ NÚT ĐĂNG NHẬP
        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Kiểm tra xem người dùng có bỏ trống không
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ Email và Mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hiển thị thông báo đang xử lý
            Toast.makeText(this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show()
            btnSignIn.isEnabled = false // Khóa nút tạm thời để tránh bấm nhiều lần

            // Gọi hàm login từ FirebaseAuthManager
            FirebaseAuthManager.login(email, pass) { isSuccess, message ->
                btnSignIn.isEnabled = true // Mở lại nút

                if (isSuccess) {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    // Nếu sai mật khẩu hoặc lỗi mạng
                    Toast.makeText(this, "Lỗi: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Chuyển sang màn hình Đăng ký
        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Nút quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // Nút Google (Tính năng mở rộng - làm sau)
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    // Hàm chuyển sang màn hình chính
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Xóa lịch sử (Back Stack) để người dùng không bấm nút Back quay lại màn hình Login được
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}