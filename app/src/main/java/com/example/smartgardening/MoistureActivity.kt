package com.example.smartgardening

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MoistureActivity : AppCompatActivity() {

    // View Components
    private lateinit var tvMainValue: TextView
    private lateinit var pbMoisture: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvMinValue: TextView
    private lateinit var tvMaxValue: TextView
    private lateinit var moistureChart: LineChart

    // Firebase Reference
    private val dbRef = FirebaseDatabase.getInstance().reference

    // Local Storage for Min/Max
    private lateinit var sharedPreferences: SharedPreferences

    // Chart Labels
    private val relativeLabels = arrayListOf(
        "-11h", "-10h", "-9h", "-8h", "-7h", "-6h",
        "-5h", "-4h", "-3h", "-2h", "-1h", "Now"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moisture)

        // 1. Init Storage
        sharedPreferences = getSharedPreferences("GardenStats", Context.MODE_PRIVATE)

        // 2. Bind Views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        tvMainValue = findViewById<TextView>(R.id.tvMainValue)
        pbMoisture = findViewById<ProgressBar>(R.id.pbMoisture)
        tvStatus = findViewById(R.id.tvStatus) // Nhớ thêm ID trong XML
        tvMinValue = findViewById(R.id.tvMinValue) // Nhớ thêm ID trong XML
        tvMaxValue = findViewById(R.id.tvMaxValue) // Nhớ thêm ID trong XML
        moistureChart = findViewById<LineChart>(R.id.moistureChart)

        btnBack.setOnClickListener { finish() }

        // 3. Load Saved Data
        loadMinMaxStats()

        // 4. Setup Chart
        setupChart(moistureChart)

        // 5. Load History from Firebase (Soil path)
        loadHistoryFromFirebase()

        // 6. Start Live Data (MQTT)
        startListeningMqtt()
    }

    private fun setupChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        for (i in 0..11) {
            entries.add(Entry(i.toFloat(), 0f))
        }

        val dataSet = LineDataSet(entries, "Soil Moisture (%)")

        // --- CẤU HÌNH MÀU SẮC CHO ĐẤT (MÀU XANH LÁ) ---
        dataSet.apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#1B5E20"))
            lineWidth = 2.5f
            circleRadius = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#C8E6C9") // Xanh nhạt
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

    // --- TẢI LỊCH SỬ TỪ FIREBASE (Path: history/soil) ---
    private fun loadHistoryFromFirebase() {
        dbRef.child("history").child("soil").limitToLast(12)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = moistureChart.data ?: return
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
                    moistureChart.notifyDataSetChanged()
                    moistureChart.invalidate()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // --- LẮNG NGHE MQTT (Key: soil) ---
    private fun startListeningMqtt() {
        MqttManager.connect()
        MqttManager.subscribe("sensor/data") { message ->
            runOnUiThread {
                try {
                    val json = JSONObject(message)
                    // Lấy giá trị Soil từ JSON
                    val soil = json.optDouble("soil", 0.0).toFloat()

                    updateRealtimeUI(soil)
                    updateMinMaxStats(soil)
                    updateChartNowPoint(soil)

                } catch (e: Exception) {
                    Log.e("Moisture", "Error: ${e.message}")
                }
            }
        }
    }

    private fun updateRealtimeUI(value: Float) {
        tvMainValue.text = "${value.toInt()}%"
        pbMoisture.progress = value.toInt()

        // Logic trạng thái Đất
        if (value < 30) {
            tvStatus.text = "DRY"
            tvStatus.setTextColor(Color.parseColor("#FF5722")) // Cam đậm
            pbMoisture.progressDrawable.setTint(Color.parseColor("#FF5722"))

        } else if (value > 70) {
            tvStatus.text = "WET"
            tvStatus.setTextColor(Color.parseColor("#2196F3")) // Xanh dương
            pbMoisture.progressDrawable.setTint(Color.parseColor("#2196F3"))

        } else {
            tvStatus.text = "IDEAL"
            tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Xanh lá
            pbMoisture.progressDrawable.setTint(Color.parseColor("#4CAF50"))
        }
    }

    private fun updateChartNowPoint(value: Float) {
        val data = moistureChart.data ?: return
        val set = data.getDataSetByIndex(0) as LineDataSet

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
        moistureChart.notifyDataSetChanged()
        moistureChart.invalidate()
    }

    private fun updateMinMaxStats(value: Float) {
        val currentDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        val lastDate = sharedPreferences.getString("SOIL_DATE", "")

        // Dùng Key riêng SOIL_MIN / SOIL_MAX để không trùng với Humidity
        var savedMin = sharedPreferences.getFloat("SOIL_MIN", 100f)
        var savedMax = sharedPreferences.getFloat("SOIL_MAX", 0f)

        if (lastDate != currentDate) {
            savedMin = 100f
            savedMax = 0f
            sharedPreferences.edit().putString("SOIL_DATE", currentDate).apply()
        }

        var isChanged = false
        if (value < savedMin && value > 0) {
            savedMin = value
            isChanged = true
        }
        if (value > savedMax) {
            savedMax = value
            isChanged = true
        }

        if (isChanged) {
            val editor = sharedPreferences.edit()
            editor.putFloat("SOIL_MIN", savedMin)
            editor.putFloat("SOIL_MAX", savedMax)
            editor.apply()

            tvMinValue.text = "${savedMin.toInt()}%"
            tvMaxValue.text = "${savedMax.toInt()}%"
        }
    }

    private fun loadMinMaxStats() {
        val savedMin = sharedPreferences.getFloat("SOIL_MIN", 0f)
        val savedMax = sharedPreferences.getFloat("SOIL_MAX", 0f)

        if (savedMin == 0f && savedMax == 0f) {
            tvMinValue.text = "--%"
            tvMaxValue.text = "--%"
        } else {
            tvMinValue.text = "${savedMin.toInt()}%"
            tvMaxValue.text = "${savedMax.toInt()}%"
        }
    }
}