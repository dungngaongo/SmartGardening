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

class HumidityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humid)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvMainValue = findViewById<TextView>(R.id.tvMainValue)
        val pbHumidity = findViewById<ProgressBar>(R.id.pbHumidity)
        val humidityChart = findViewById<LineChart>(R.id.humidityChart)

        btnBack.setOnClickListener { finish() }

        // Cập nhật giá trị ảo
        tvMainValue.text = "65%"
        pbHumidity.progress = 65

        setupHumidityChart(humidityChart)
    }

    private fun setupHumidityChart(chart: LineChart) {
        val entries = ArrayList<Entry>()
        entries.add(Entry(0f, 60f))
        entries.add(Entry(1f, 65f))
        entries.add(Entry(2f, 70f))
        entries.add(Entry(3f, 62f))
        entries.add(Entry(4f, 58f))
        entries.add(Entry(5f, 65f))

        val dataSet = LineDataSet(entries, "Humidity (%)")
        dataSet.apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#2196F3"))
            lineWidth = 2.5f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#BBDEFB")
            fillAlpha = 70
            setDrawValues(false)
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }
}