package com.example.smartgardening

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.smartgardening.firebase.FirebaseWateringManager
import com.example.smartgardening.mqtt.MqttManager
import com.google.android.material.button.MaterialButton
import java.util.*

class PumpModesActivity : AppCompatActivity() {

    private var isPumpOn = false
    private val TOPIC_MODE = "settings/mode"
    private val TOPIC_THRESHOLD = "settings/soil_threshold"

    private var pumpStartTime: Long = 0L
    private var currentMode = "MANUAL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pump_modes)

        // View
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnPumpPower = findViewById<MaterialButton>(R.id.btnPumpPower)
        val tvPumpStatus = findViewById<TextView>(R.id.tvPumpStatus)
        val swScheduleMode = findViewById<SwitchCompat>(R.id.swScheduleMode)
        val swAutoMode = findViewById<SwitchCompat>(R.id.swAutoMode)
        val layoutScheduleSettings = findViewById<LinearLayout>(R.id.layoutScheduleSettings)
        val layoutAutoSettings = findViewById<LinearLayout>(R.id.layoutAutoSettings)
        val btnSelectTime = findViewById<Button>(R.id.btnSelectTime)
        val sbThreshold = findViewById<SeekBar>(R.id.sbThreshold)
        val tvThresholdValue = findViewById<TextView>(R.id.tvThresholdValue)

        btnBack.setOnClickListener { finish() }

        // 🔥 KẾT NỐI MQTT 1 LẦN
        MqttManager.connect()

        // ===== NÚT BẬT / TẮT MÁY BƠM =====
        btnPumpPower.setOnClickListener {

            if (!isPumpOn) {
                // ===== BẬT BƠM =====
                isPumpOn = true
                pumpStartTime = System.currentTimeMillis()
                currentMode = "MANUAL"
                MqttManager.publish(TOPIC_MODE, "0")
                MqttManager.publish("pump/control", "on")
            } else {
                // ===== TẮT BƠM =====
                isPumpOn = false
                MqttManager.publish("pump/control", message = "off")

                if (pumpStartTime > 0) {
                    FirebaseWateringManager.saveLastWatering(
                        startTime = pumpStartTime,
                        endTime = System.currentTimeMillis(),
                        mode = currentMode
                    )
                }

                pumpStartTime = 0L
            }

            updatePumpUI(btnPumpPower, tvPumpStatus)
        }

        // ===== SCHEDULE MODE =====
        swScheduleMode.setOnCheckedChangeListener { _, isChecked ->
            layoutScheduleSettings.isEnabled = isChecked
            layoutScheduleSettings.alpha = if (isChecked) 1f else 0.4f
        }

        btnSelectTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                btnSelectTime.text = String.format("Giờ: %02d:%02d", h, m)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        // ===== AUTO MODE =====
        swAutoMode.setOnCheckedChangeListener { _, isChecked ->
            // Cập nhật giao diện mờ/sáng
            layoutAutoSettings.isEnabled = isChecked
            layoutAutoSettings.alpha = if (isChecked) 1f else 0.4f

            if (isChecked) {
                // >>> KHI BẬT AUTO <<<
                // 1. Tắt Schedule nếu đang bật
                if (swScheduleMode.isChecked) swScheduleMode.isChecked = false

                // 2. Nếu đang Bật bơm thủ công -> Tắt ngay để giao quyền cho Auto
                if (isPumpOn) {
                    isPumpOn = false
                    pumpStartTime = 0L // Reset thời gian đếm
                    updatePumpUI(btnPumpPower, tvPumpStatus) // Cập nhật nút về màu xám
                    // Không gửi lệnh off bơm ở đây, để ESP tự quyết định dựa trên cảm biến
                }

                // 3. Gửi lệnh chuyển Mode 1
                MqttManager.publish(TOPIC_MODE, "1")

                // 4. Đồng bộ lại Threshold
                val currentThreshold = sbThreshold.progress
                MqttManager.publish(TOPIC_THRESHOLD, currentThreshold.toString())

                // 5. Khóa nút bấm Manual
                btnPumpPower.isEnabled = false
                btnPumpPower.alpha = 0.5f

                Toast.makeText(this, "Đã BẬT Auto Mode", Toast.LENGTH_SHORT).show()

            } else {
                // >>> KHI TẮT AUTO (VỀ MANUAL) <<<

                // 1. Gửi lệnh chuyển Mode 0
                MqttManager.publish(TOPIC_MODE, "0")

                // 2. [QUAN TRỌNG] Gửi lệnh TẮT BƠM NGAY để tránh bơm bị treo nếu đang chạy dở
                MqttManager.publish("pump/control", "off")

                // 3. Đảm bảo trạng thái biến App đồng bộ
                isPumpOn = false
                updatePumpUI(btnPumpPower, tvPumpStatus)

                // 4. Mở khóa nút bấm Manual
                btnPumpPower.isEnabled = true
                btnPumpPower.alpha = 1.0f

                Toast.makeText(this, "Đã về Manual Mode", Toast.LENGTH_SHORT).show()
            }
        }
        //== THANH KÉO NGƯỠNG ĐỘ ẨM ====
        sbThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Cập nhật số hiển thị realtime khi kéo
                tvThresholdValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Không làm gì khi bắt đầu chạm
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // QUAN TRỌNG: Chỉ gửi MQTT khi người dùng THẢ TAY ra khỏi thanh trượt
                // Để tránh gửi hàng trăm tin nhắn liên tục khi đang kéo gây lag ESP

                val value = seekBar?.progress ?: 30

                // Chỉ gửi nếu đang bật chế độ Auto hoặc muốn cập nhật trước
                MqttManager.publish(TOPIC_THRESHOLD, value.toString())

                Toast.makeText(applicationContext, "Đã cập nhật ngưỡng tưới: $value%", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        MqttManager.disconnect()
    }

    private fun updatePumpUI(button: MaterialButton, statusText: TextView) {
        if (isPumpOn) {
            button.setStrokeColorResource(android.R.color.holo_green_light)
            button.setIconTintResource(android.R.color.holo_green_light)
            statusText.text = "MÁY BƠM ĐANG CHẠY"
            statusText.setTextColor(Color.parseColor("#2ECC71"))
        } else {
            button.setStrokeColorResource(android.R.color.white)
            button.setIconTintResource(android.R.color.white)
            statusText.text = "MÁY BƠM ĐANG TẮT"
            statusText.setTextColor(Color.parseColor("#1D431F"))
        }
    }
}
