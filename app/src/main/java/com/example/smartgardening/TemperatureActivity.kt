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

class TemperatureActivity : AppCompatActivity() {

    // View Components
    private lateinit var tvMainValue: TextView
    private lateinit var pbTempColumn: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvMinValue: TextView
    private lateinit var tvMaxValue: TextView
    private lateinit var tempChart: LineChart

    // Firebase
    private val dbRef = FirebaseDatabase.getInstance().reference

    // SharedPrefs
    private lateinit var sharedPreferences: SharedPreferences

    // Chart Labels (12 points)
    private val relativeLabels = arrayListOf(
        "-11h", "-10h", "-9h", "-8h", "-7h", "-6h",
        "-5h", "-4h", "-3h", "-2h", "-1h", "Now"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp)

        // 1. Init Storage
        sharedPreferences = getSharedPreferences("GardenStats", Context.MODE_PRIVATE)

        // 2. Bind Views (Khớp ID trong XML của bạn)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        tvMainValue = findViewById<TextView>(R.id.tvMainValue)
        pbTempColumn = findViewById<ProgressBar>(R.id.pbTempColumn)
        tvStatus = findViewById(R.id.tvStatus)
        tvMinValue = findViewById(R.id.tvMinValue)
        tvMaxValue = findViewById(R.id.tvMaxValue)
        tempChart = findViewById<LineChart>(R.id.tempChart)

        btnBack.setOnClickListener { finish() }

        // 3. Load Saved Data
        loadMinMaxStats()

        // 4. Setup Chart
        setupTempChart(tempChart)

        // 5. Load History from Firebase
        loadHistoryFromFirebase()

        // 6. Start Live Data
        startListeningMqtt()
    }

    private fun setupTempChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        for (i in 0..11) {
            entries.add(Entry(i.toFloat(), 0f))
        }

        val dataSet = LineDataSet(entries, "Temperature (°C)")

        // --- MÀU ĐỎ CHO NHIỆT ĐỘ ---
        dataSet.apply {
            color = Color.parseColor("#E53935") // Đỏ đậm
            setCircleColor(Color.parseColor("#B71C1C"))
            lineWidth = 2.5f
            circleRadius = 3f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#FFCDD2") // Hồng nhạt
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
            axisLeft.axisMaximum = 50f // Max nhiệt độ khoảng 50 là vừa
            invalidate()
        }
    }

    private fun loadHistoryFromFirebase() {
        // Path: history/temperature
        dbRef.child("history").child("temperature").limitToLast(12)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = tempChart.data ?: return
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
                    tempChart.notifyDataSetChanged()
                    tempChart.invalidate()
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
                    // Key: temp
                    val temp = json.optDouble("temp", 0.0).toFloat()

                    updateRealtimeUI(temp)
                    updateMinMaxStats(temp)
                    updateChartNowPoint(temp)

                } catch (e: Exception) {
                    Log.e("Temp", "Error: ${e.message}")
                }
            }
        }
    }

    private fun updateRealtimeUI(value: Float) {
        tvMainValue.text = "${value.toInt()}°C"
        pbTempColumn.progress = value.toInt() // Cập nhật cột nhiệt kế

        // Logic trạng thái Nhiệt độ
        if (value < 18) {
            tvStatus.text = "LOW"
            tvStatus.setTextColor(Color.parseColor("#2196F3")) // Xanh dương
            // Đổi màu thanh ProgressBar thành Xanh
            pbTempColumn.progressDrawable.setTint(Color.parseColor("#2196F3"))

        } else if (value > 35) {
            tvStatus.text = "HIGH"
            tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Đỏ đậm
            // Đổi màu thanh ProgressBar thành Đỏ
            pbTempColumn.progressDrawable.setTint(Color.parseColor("#D32F2F"))

        } else {
            tvStatus.text = "NORMAL"
            tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Xanh lá
            // Đổi màu thanh ProgressBar thành Xanh lá
            pbTempColumn.progressDrawable.setTint(Color.parseColor("#4CAF50"))
        }
    }

    private fun updateChartNowPoint(value: Float) {
        val data = tempChart.data ?: return
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
        tempChart.notifyDataSetChanged()
        tempChart.invalidate()
    }

    private fun updateMinMaxStats(value: Float) {
        val currentDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        val lastDate = sharedPreferences.getString("TEMP_DATE", "")

        var savedMin = sharedPreferences.getFloat("TEMP_MIN", 100f)
        var savedMax = sharedPreferences.getFloat("TEMP_MAX", 0f)

        if (lastDate != currentDate) {
            savedMin = 100f
            savedMax = 0f
            sharedPreferences.edit().putString("TEMP_DATE", currentDate).apply()
        }

        var isChanged = false
        if (value < savedMin) {
            savedMin = value
            isChanged = true
        }
        if (value > savedMax) {
            savedMax = value
            isChanged = true
        }

        if (isChanged) {
            val editor = sharedPreferences.edit()
            editor.putFloat("TEMP_MIN", savedMin)
            editor.putFloat("TEMP_MAX", savedMax)
            editor.apply()

            tvMinValue.text = "${savedMin.toInt()}°C"
            tvMaxValue.text = "${savedMax.toInt()}°C"
        }
    }

    private fun loadMinMaxStats() {
        val savedMin = sharedPreferences.getFloat("TEMP_MIN", 0f)
        val savedMax = sharedPreferences.getFloat("TEMP_MAX", 0f)

        if (savedMin == 0f && savedMax == 0f) {
            tvMinValue.text = "--"
            tvMaxValue.text = "--"
        } else {
            tvMinValue.text = "${savedMin.toInt()}°C"
            tvMaxValue.text = "${savedMax.toInt()}°C"
        }
    }
}