package com.example.smartgardening.firebase

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthManager {
    // Biến auth của bạn đã có sẵn
    private val auth = FirebaseAuth.getInstance()

    // Lấy ID người dùng hiện tại
    fun getUid(): String? = auth.currentUser?.uid

    // Kiểm tra xem người dùng đã đăng nhập chưa
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // 1. Hàm Đăng Ký
    fun register(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null) // Thành công
                } else {
                    onResult(false, task.exception?.message) // Thất bại, trả về lỗi
                }
            }
    }

    // 2. Hàm Đăng Nhập
    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    // 3. Hàm Đăng Xuất
    fun logout() {
        auth.signOut()
    }
}