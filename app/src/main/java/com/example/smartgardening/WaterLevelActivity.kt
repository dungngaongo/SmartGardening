package com.example.smartgardening

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class WaterLevelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_level)

        val pbWaterTank = findViewById<ProgressBar>(R.id.pbWaterTank)
        val tvWaterPercent = findViewById<TextView>(R.id.tvWaterPercent)
        val tvWaterStatus = findViewById<TextView>(R.id.tvWaterStatus)
        val waterChart = findViewById<LineChart>(R.id.waterChart)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Giả lập dữ liệu hiện tại (75%)
        updateWaterUI(75, pbWaterTank, tvWaterPercent, tvWaterStatus)

        setupWaterChart(waterChart)
    }

    private fun updateWaterUI(percent: Int, pb: ProgressBar, tvP: TextView, tvS: TextView) {
        pb.progress = percent
        tvP.text = "$percent%"

        if (percent < 20) {
            tvS.text = "Sắp Hết Nước!"
            tvS.setTextColor(Color.RED)
        } else {
            tvS.text = "An Toàn"
            tvS.setTextColor(Color.parseColor("#2ECC71"))
        }
    }

    private fun setupWaterChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 90f))
        entries.add(Entry(1f, 85f))
        entries.add(Entry(2f, 80f))
        entries.add(Entry(3f, 75f))
        entries.add(Entry(4f, 75f))

        val dataSet = LineDataSet(entries, "Mực nước (%)")
        dataSet.apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#2196F3"))
            lineWidth = 3f
            mode = LineDataSet.Mode.STEPPED // Dạng bậc thang nhìn giống mực nước tiêu thụ
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
            setDrawValues(false)
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(1000)
            invalidate()
        }
    }
}