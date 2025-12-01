package com.example.myapplication.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.database.QRCodeDatabase
import com.example.myapplication.data.repository.AnalyticsRepository
import com.example.myapplication.databinding.FragmentAnalyticsBinding
import com.example.myapplication.ui.viewmodel.AnalyticsViewModel
import com.example.myapplication.ui.viewmodel.AnalyticsViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class AnalyticsFragment : Fragment() {
    
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: AnalyticsViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        observeViewModel()
        viewModel.loadAnalytics()
    }
    
    private fun setupViewModel() {
        val database = QRCodeDatabase.getDatabase(requireContext())
        val repository = AnalyticsRepository(database.qrCodeDao())
        val factory = AnalyticsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AnalyticsViewModel::class.java]
    }
    
    private fun observeViewModel() {
        viewModel.analyticsSummary.observe(viewLifecycleOwner) { summary ->
            // Update stat cards
            binding.tvTotalScans.text = summary.totalScans.toString()
            binding.tvTotalGenerated.text = summary.totalGenerated.toString()
            
            // Setup charts
            if (summary.typeStats.isNotEmpty()) {
                setupPieChart(summary.typeStats.map { it.type.name to it.count })
            }
            
            if (summary.dateStats.isNotEmpty()) {
                setupLineChart(summary.dateStats.map { it.date to it.count })
            }
        }
    }
    
    private fun setupPieChart(data: List<Pair<String, Int>>) {
        val pieChart: PieChart = binding.pieChartTypes
        
        // Create entries
        val entries = data.map { (label, value) ->
            PieEntry(value.toFloat(), label)
        }
        
        val dataSet = PieDataSet(entries, "QR Types")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        
        // Styling
        pieChart.description.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = true
        pieChart.animateY(1000)
        
        pieChart.invalidate()
    }
    
    private fun setupLineChart(data: List<Pair<String, Int>>) {
        val lineChart: LineChart = binding.lineChartTimeline
        
        // Create entries (reversed to show chronologically)
        val entries = data.reversed().mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value.toFloat())
        }
        
        val dataSet = LineDataSet(entries, "Scans per Day")
        dataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.setCircleColor(ColorTemplate.MATERIAL_COLORS[0])
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        
        // X-Axis configuration
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(data.reversed().map { it.first })
        xAxis.labelRotationAngle = -45f
        
        // Y-Axis
        lineChart.axisRight.isEnabled = false
        lineChart.axisLeft.setDrawGridLines(true)
        
        // Styling
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.animateX(1000)
        
        lineChart.invalidate()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
