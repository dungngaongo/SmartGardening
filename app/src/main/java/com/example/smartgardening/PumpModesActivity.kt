package com.example.smartgardening

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import java.util.*

class PumpModesActivity : AppCompatActivity() {

    private var isPumpOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pump_modes)

        // Ánh xạ View
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

        // 1. Logic Nút nguồn tổng
        btnPumpPower.setOnClickListener {
            isPumpOn = !isPumpOn
            updatePumpUI(btnPumpPower, tvPumpStatus)
        }

        // 2. Logic Schedule Mode
        swScheduleMode.setOnCheckedChangeListener { _, isChecked ->
            layoutScheduleSettings.isEnabled = isChecked
            layoutScheduleSettings.alpha = if (isChecked) 1.0f else 0.4f
        }

        btnSelectTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                btnSelectTime.text = String.format("Giờ: %02d:%02d", h, m)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        // 3. Logic Auto Mode
        swAutoMode.setOnCheckedChangeListener { _, isChecked ->
            layoutAutoSettings.isEnabled = isChecked
            layoutAutoSettings.alpha = if (isChecked) 1.0f else 0.4f
        }

        sbThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                tvThresholdValue.text = "$progress%"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // Khởi tạo trạng thái ban đầu (OFF) cho các chế độ
        swScheduleMode.isChecked = false
        layoutScheduleSettings.isEnabled = false
        layoutScheduleSettings.alpha = 0.4f

        swAutoMode.isChecked = false
        layoutAutoSettings.isEnabled = false
        layoutAutoSettings.alpha = 0.4f
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