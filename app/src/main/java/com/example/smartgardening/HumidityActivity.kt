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

class HumidityActivity : AppCompatActivity() {

    // View
    private lateinit var tvMainValue: TextView
    private lateinit var pbHumidity: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvMinValue: TextView
    private lateinit var tvMaxValue: TextView
    private lateinit var humidityChart: LineChart

    // Firebase
    private val dbRef = FirebaseDatabase.getInstance().reference

    // SharedPreferences (Lưu Min/Max)
    private lateinit var sharedPreferences: SharedPreferences

    // Danh sách nhãn trục X (Cố định 12 mốc)
    private val relativeLabels = arrayListOf(
        "-11h", "-10h", "-9h", "-8h", "-7h", "-6h",
        "-5h", "-4h", "-3h", "-2h", "-1h", "Now"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humid)

        // 1. Khởi tạo Storage
        sharedPreferences = getSharedPreferences("GardenStats", Context.MODE_PRIVATE)

        // 2. Ánh xạ View
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        tvMainValue = findViewById<TextView>(R.id.tvMainValue)
        pbHumidity = findViewById<ProgressBar>(R.id.pbHumidity)

        // 🔥 Ánh xạ thêm View trạng thái
        tvStatus = findViewById(R.id.tvStatus)

        tvMinValue = findViewById(R.id.tvMinValue)
        tvMaxValue = findViewById(R.id.tvMaxValue)
        humidityChart = findViewById<LineChart>(R.id.humidityChart)

        btnBack.setOnClickListener { finish() }

        // 3. Hiển thị Min/Max đã lưu
        loadMinMaxStats()

        // 4. Cấu hình biểu đồ
        setupHumidityChart(humidityChart)

        // 5. Tải lịch sử từ Firebase
        loadHistoryFromFirebase()

        // 6. Bắt đầu lắng nghe MQTT (Live)
        startListeningMqtt()
    }

    private fun setupHumidityChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        for (i in 0..11) {
            entries.add(Entry(i.toFloat(), 0f))
        }

        val dataSet = LineDataSet(entries, "Độ ẩm (%)")

        dataSet.apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#1565C0"))
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
        dbRef.child("history").child("humidity").limitToLast(12)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = humidityChart.data ?: return
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

                    if (set.entryCount == 0) {
                        set.addEntry(Entry(11f, 0f))
                    }

                    data.notifyDataChanged()
                    humidityChart.notifyDataSetChanged()
                    humidityChart.invalidate()
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
                    val humi = json.optDouble("humi", 0.0).toFloat()

                    // 1. Cập nhật Giao diện (Số, Thanh đo, Trạng thái)
                    updateRealtimeUI(humi)

                    // 2. Cập nhật Min/Max
                    updateMinMaxStats(humi)

                    // 3. Cập nhật điểm "Now" trên biểu đồ
                    updateChartNowPoint(humi)

                } catch (e: Exception) {
                    Log.e("Humidity", "Error: ${e.message}")
                }
            }
        }
    }

    // 🔥 HÀM CẬP NHẬT UI ĐÃ NÂNG CẤP
    private fun updateRealtimeUI(value: Float) {
        tvMainValue.text = "${value.toInt()}%"
        pbHumidity.progress = value.toInt()

        // Logic đổi màu và chữ trạng thái
        if (value < 40) {
            // Khô
            tvStatus.text = "DRY"
            tvStatus.setTextColor(Color.parseColor("#FF9800")) // Cam
            pbHumidity.progressDrawable.setTint(Color.parseColor("#FF9800"))

        } else if (value > 75) {
            // Ẩm ướt
            tvStatus.text = "WET"
            tvStatus.setTextColor(Color.parseColor("#1565C0")) // Xanh đậm
            pbHumidity.progressDrawable.setTint(Color.parseColor("#1565C0"))

        } else {
            // Thoải mái
            tvStatus.text = "COMFORT"
            tvStatus.setTextColor(Color.parseColor("#2ECC71")) // Xanh lá
            pbHumidity.progressDrawable.setTint(Color.parseColor("#2ECC71"))
        }
    }

    private fun updateChartNowPoint(value: Float) {
        val data = humidityChart.data ?: return
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

        if (!found) {
            set.addEntry(Entry(11f, value))
        }

        data.notifyDataChanged()
        humidityChart.notifyDataSetChanged()
        humidityChart.invalidate()
    }

    private fun updateMinMaxStats(value: Float) {
        val currentDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        val lastDate = sharedPreferences.getString("STATS_DATE", "")

        var savedMin = sharedPreferences.getFloat("HUMI_MIN", 100f)
        var savedMax = sharedPreferences.getFloat("HUMI_MAX", 0f)

        if (lastDate != currentDate) {
            savedMin = 100f
            savedMax = 0f
            sharedPreferences.edit().putString("STATS_DATE", currentDate).apply()
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
            editor.putFloat("HUMI_MIN", savedMin)
            editor.putFloat("HUMI_MAX", savedMax)
            editor.apply()

            tvMinValue.text = "${savedMin.toInt()}%"
            tvMaxValue.text = "${savedMax.toInt()}%"
        }
    }

    private fun loadMinMaxStats() {
        val savedMin = sharedPreferences.getFloat("HUMI_MIN", 0f)
        val savedMax = sharedPreferences.getFloat("HUMI_MAX", 0f)

        if (savedMin == 0f && savedMax == 0f) {
            tvMinValue.text = "--%"
            tvMaxValue.text = "--%"
        } else {
            tvMinValue.text = "${savedMin.toInt()}%"
            tvMaxValue.text = "${savedMax.toInt()}%"
        }
    }
}