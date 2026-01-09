package com.example.smartgardening

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgardening.mqtt.MqttManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class WaterLevelActivity : AppCompatActivity() {

    // View Components
    private lateinit var tvWaterPercent: TextView
    private lateinit var tvWaterStatus: TextView
    private lateinit var pbWaterTank: ProgressBar

    // 3 Ô nhập cấu hình
    private lateinit var etTankHeight: EditText
    private lateinit var etWaterOffset: EditText // 🔥 MỚI
    private lateinit var etLowWarning: EditText

    private lateinit var waterChart: LineChart

    // Firebase
    private val dbRef = FirebaseDatabase.getInstance().reference
    private lateinit var sharedPreferences: SharedPreferences

    // Chart Labels
    private val relativeLabels = arrayListOf(
        "-11h", "-10h", "-9h", "-8h", "-7h", "-6h",
        "-5h", "-4h", "-3h", "-2h", "-1h", "Now"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_level)

        sharedPreferences = getSharedPreferences("GardenStats", Context.MODE_PRIVATE)

        // 1. Bind Views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        tvWaterPercent = findViewById(R.id.tvWaterPercent)
        tvWaterStatus = findViewById(R.id.tvWaterStatus)
        pbWaterTank = findViewById(R.id.pbWaterTank)

        etTankHeight = findViewById(R.id.etTankHeight)
        etWaterOffset = findViewById(R.id.etWaterOffset) // 🔥 MỚI
        etLowWarning = findViewById(R.id.etLowWarning)

        waterChart = findViewById(R.id.waterChart)

        btnBack.setOnClickListener { finish() }

        // 2. Setup Chart & Listeners
        setupChart(waterChart)
        loadSettings()
        setupConfigListeners() // Thiết lập sự kiện nhập liệu
        loadHistoryFromFirebase()
        startListeningMqtt()
    }

    private fun loadSettings() {
        // Load giá trị cũ, nếu không có thì lấy mặc định (Height=100, Offset=4, Warning=20)
        val height = sharedPreferences.getString("TANK_HEIGHT", "100")
        val offset = sharedPreferences.getString("WATER_OFFSET", "4") // 🔥 MỚI
        val warning = sharedPreferences.getString("MIN_WATER", "20")

        etTankHeight.setText(height)
        etWaterOffset.setText(offset) // 🔥 MỚI
        etLowWarning.setText(warning)
    }

    private fun setupConfigListeners() {
        // 1. Chiều cao bồn (tank_height)
        etTankHeight.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val value = v.text.toString()
                if (value.isNotEmpty()) {
                    sharedPreferences.edit().putString("TANK_HEIGHT", value).apply()
                    MqttManager.publish("settings/tank_height", value)
                    Toast.makeText(this, "Đã gửi chiều cao: $value cm", Toast.LENGTH_SHORT).show()
                }
                etTankHeight.clearFocus()
            }
            false
        }

        // 2. 🔥 MỚI: Khoảng hở Offset (water_offset)
        etWaterOffset.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val value = v.text.toString()
                if (value.isNotEmpty()) {
                    sharedPreferences.edit().putString("WATER_OFFSET", value).apply()
                    MqttManager.publish("settings/water_offset", value)
                    Toast.makeText(this, "Đã gửi Offset: $value cm", Toast.LENGTH_SHORT).show()
                }
                etWaterOffset.clearFocus()
            }
            false
        }

        // 3. Cảnh báo cạn (min_water)
        etLowWarning.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val value = v.text.toString()
                if (value.isNotEmpty()) {
                    sharedPreferences.edit().putString("MIN_WATER", value).apply()
                    MqttManager.publish("settings/min_water", value)
                    Toast.makeText(this, "Đã cập nhật mức cảnh báo: $value%", Toast.LENGTH_SHORT).show()
                }
                etLowWarning.clearFocus()
            }
            false
        }
    }

    // --- CÁC PHẦN DƯỚI ĐÂY GIỮ NGUYÊN NHƯ CŨ ---

    private fun setupChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        for (i in 0..11) entries.add(Entry(i.toFloat(), 0f))

        val dataSet = LineDataSet(entries, "Water Level (%)")
        dataSet.apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#1976D2"))
            lineWidth = 2.5f
            circleRadius = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
            fillAlpha = 60
            setDrawValues(false)
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(relativeLabels)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 11f
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            invalidate()
        }
    }

    private fun loadHistoryFromFirebase() {
        dbRef.child("history").child("water").limitToLast(12)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = waterChart.data ?: return
                    val set = data.getDataSetByIndex(0) as LineDataSet
                    set.clear()
                    val totalPoints = snapshot.childrenCount.toInt()
                    var currentIndex = 12 - totalPoints
                    if (currentIndex < 0) currentIndex = 0
                    for (child in snapshot.children) {
                        val value = child.child("value").getValue(Float::class.java) ?: 0f
                        set.addEntry(Entry(currentIndex.toFloat(), value))
                        currentIndex++
                    }
                    if (set.entryCount == 0) set.addEntry(Entry(11f, 0f))
                    data.notifyDataChanged()
                    waterChart.notifyDataSetChanged()
                    waterChart.invalidate()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun startListeningMqtt() {
        MqttManager.connect()
        MqttManager.subscribe("sensor/data") { message ->
            runOnUiThread {
                try {
                    val json = JSONObject(message)
                    val water = json.optInt("water", 0)
                    updateRealtimeUI(water)
                    updateChartNowPoint(water.toFloat())
                } catch (e: Exception) {
                    Log.e("Water", "Error: ${e.message}")
                }
            }
        }
    }

    private fun updateRealtimeUI(value: Int) {
        if (value == -1) {
            tvWaterPercent.text = "ERR"
            tvWaterPercent.setTextColor(Color.RED)
            tvWaterStatus.text = "SENSOR ERROR"
            tvWaterStatus.setTextColor(Color.RED)
            pbWaterTank.progress = 0
            return
        }

        tvWaterPercent.text = "$value%"
        pbWaterTank.progress = value

        val warningLimit = etLowWarning.text.toString().toIntOrNull() ?: 20

        if (value < warningLimit) {
            tvWaterPercent.setTextColor(Color.parseColor("#FF5722"))
            tvWaterStatus.text = "LOW WATER"
            tvWaterStatus.setTextColor(Color.parseColor("#FF5722"))
            pbWaterTank.progressDrawable.setTint(Color.parseColor("#FF5722"))
        } else {
            tvWaterPercent.setTextColor(Color.parseColor("#2196F3"))
            tvWaterStatus.text = "SAFE LEVEL"
            tvWaterStatus.setTextColor(Color.parseColor("#2ECC71"))
            pbWaterTank.progressDrawable.setTint(Color.parseColor("#2196F3"))
        }
    }

    private fun updateChartNowPoint(value: Float) {
        val data = waterChart.data ?: return
        val set = data.getDataSetByIndex(0) as LineDataSet
        if (value < 0) return
        val entries = set.values
        var found = false
        for (e in entries) {
            if (e.x == 11f) {
                e.y = value
                found = true
                break
            }
        }
        if (!found) set.addEntry(Entry(11f, value))
        data.notifyDataChanged()
        waterChart.notifyDataSetChanged()
        waterChart.invalidate()
    }
}