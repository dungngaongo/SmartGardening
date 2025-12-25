package com.example.smartgardening

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.widget.ImageButton

class MoistureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moisture)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val moistureChart = findViewById<LineChart>(R.id.moistureChart)
        setupChart(moistureChart)
    }

    private fun setupChart(chart: LineChart) {
        // 1. Tạo dữ liệu giả lập (x: giờ, y: độ ẩm %)
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 40f))
        entries.add(Entry(1f, 45f))
        entries.add(Entry(2f, 42f))
        entries.add(Entry(3f, 38f))
        entries.add(Entry(4f, 50f))
        entries.add(Entry(5f, 42f))

        // 2. Cấu hình đường kẻ
        val dataSet = LineDataSet(entries, "Soil Moisture (%)")
        dataSet.color = Color.parseColor("#1D431F") // Màu xanh rừng của app
        dataSet.setCircleColor(Color.parseColor("#1D431F"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false) // Tắt hiển thị số tại mỗi điểm cho đỡ rối
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Làm đường cong mượt mà

        // Tạo hiệu ứng tô màu dưới đường kẻ (Gradient)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#A5D6A7")
        dataSet.fillAlpha = 50

        // 3. Đưa dữ liệu vào biểu đồ
        val lineData = LineData(dataSet)
        chart.data = lineData

        // 4. Tinh chỉnh thẩm mỹ cho biểu đồ
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)

        // Ẩn các lưới kẻ ô cho giao diện sạch sẽ
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false // Ẩn cột trục bên phải

        chart.animateX(1000) // Hiệu ứng chạy từ trái sang phải
        chart.invalidate() // Vẽ lại
    }
}