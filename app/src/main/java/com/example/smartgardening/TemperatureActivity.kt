package com.example.smartgardening

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class TemperatureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp)

        // 1. Ánh xạ các view từ XML
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvMainValue = findViewById<TextView>(R.id.tvMainValue)
        val pbTempColumn = findViewById<ProgressBar>(R.id.pbTempColumn)
        val tempChart = findViewById<LineChart>(R.id.tempChart)

        // 2. Thiết lập nút quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // 3. Cập nhật giá trị hiển thị (Giả lập 28 độ)
        tvMainValue.text = "28°C"
        pbTempColumn.progress = 28 // Cột nhiệt kế sẽ dâng lên mức 28/100

        // 4. Khởi tạo biểu đồ nhiệt độ
        setupTempChart(tempChart)
    }

    private fun setupTempChart(chart: LineChart) {
        // Tạo dữ liệu giả lập: x là giờ (8h-18h), y là nhiệt độ (°C)
        val entries = ArrayList<Entry>()
        entries.add(Entry(8f, 24f))
        entries.add(Entry(10f, 27f))
        entries.add(Entry(12f, 31f))
        entries.add(Entry(14f, 32f))
        entries.add(Entry(16f, 29f))
        entries.add(Entry(18f, 26f))

        // Cấu hình đường kẻ biểu đồ
        val dataSet = LineDataSet(entries, "Temperature (°C)")
        dataSet.apply {
            color = Color.parseColor("#FF5252") // Màu đỏ cho nhiệt độ
            setCircleColor(Color.parseColor("#FF5252"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false) // Tắt hiện số trên các điểm cho thoáng
            mode = LineDataSet.Mode.CUBIC_BEZIER // Đường cong mượt mà

            // Hiệu ứng tô bóng phía dưới đường kẻ
            setDrawFilled(true)
            fillColor = Color.parseColor("#FFCDD2")
            fillAlpha = 60
        }

        // Đưa dữ liệu vào chart
        chart.data = LineData(dataSet)

        // Tinh chỉnh thẩm mỹ cho biểu đồ
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)

            // Ẩn các lưới kẻ ô
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false

            animateX(1200) // Hiệu ứng vẽ từ trái sang phải
            invalidate()   // Vẽ lại biểu đồ
        }
    }
}