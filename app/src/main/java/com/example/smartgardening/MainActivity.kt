package com.example.smartgardening

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Đảm bảo tên file layout trùng với file XML của bạn
        setContentView(R.layout.activity_home)

        // 1. Ánh xạ các CardView từ giao diện
        val cardTemp = findViewById<CardView>(R.id.cardTemp)
        val cardHumid = findViewById<CardView>(R.id.cardHumid)
        val cardMoisture = findViewById<CardView>(R.id.cardMoisture)
        val cardPump = findViewById<CardView>(R.id.cardPump)
        val cardWater = findViewById<CardView>(R.id.cardWater)

        // 2. Thiết lập sự kiện bấm cho từng ô

        // Mở màn hình Nhiệt độ
        cardTemp.setOnClickListener {
            val intent = Intent(this, TemperatureActivity::class.java)
            startActivity(intent)
        }

        // Mở màn hình Độ ẩm không khí
        cardHumid.setOnClickListener {
            val intent = Intent(this, HumidityActivity::class.java)
            startActivity(intent)
        }

        // Mở màn hình Độ ẩm đất
        cardMoisture.setOnClickListener {
            val intent = Intent(this, MoistureActivity::class.java)
            startActivity(intent)
        }

        // Mở màn hình Chế độ bơm
        cardPump.setOnClickListener {
            // Chú ý: Đổi tên PumpModesActivity cho đúng với file bạn đã tạo
            val intent = Intent(this, PumpModesActivity::class.java)
            startActivity(intent)
        }

        cardWater.setOnClickListener {
            startActivity(Intent(this, WaterLevelActivity::class.java))
        }
    }
}