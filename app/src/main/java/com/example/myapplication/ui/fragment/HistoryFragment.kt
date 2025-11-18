package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.database.QRCodeDatabase
import com.example.myapplication.data.model.QRCodeEntity
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.databinding.FragmentHistoryBinding
import com.example.myapplication.ui.adapter.QRHistoryAdapter
import com.example.myapplication.ui.viewmodel.HistoryViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryFragment : Fragment() {
    
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: QRHistoryAdapter
    private var currentTab = 0 // 0 = scanned, 1 = created
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupUI()
        observeViewModel()
    }
    
    private fun setupViewModel() {
        val database = QRCodeDatabase.getDatabase(requireContext())
        val repository = QRCodeRepository(database.qrCodeDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]
    }
    
    private fun setupUI() {
        adapter = QRHistoryAdapter(
            onItemClick = { qrCode ->
                showQRCodeDetails(qrCode)
            },
            onDeleteClick = { qrCode ->
                confirmDelete(qrCode)
            }
        )
        
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateList()
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun observeViewModel() {
        viewModel.scannedQRCodes.observe(viewLifecycleOwner) { qrCodes ->
            if (currentTab == 0) {
                updateAdapter(qrCodes)
            }
        }
        
        viewModel.createdQRCodes.observe(viewLifecycleOwner) { qrCodes ->
            if (currentTab == 1) {
                updateAdapter(qrCodes)
            }
        }
    }
    
    private fun updateList() {
        when (currentTab) {
            0 -> {
                viewModel.scannedQRCodes.value?.let { updateAdapter(it) }
            }
            1 -> {
                viewModel.createdQRCodes.value?.let { updateAdapter(it) }
            }
        }
    }
    
    private fun updateAdapter(qrCodes: List<QRCodeEntity>) {
        adapter.submitList(qrCodes)
        binding.tvEmpty.visibility = if (qrCodes.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showQRCodeDetails(qrCode: QRCodeEntity) {
        val message = "Nội dung: ${qrCode.content}\n\nLoại: ${getTypeString(qrCode.type)}"
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chi tiết QR Code")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
        
        // Add open button if it's a URL
        if (qrCode.type == com.example.myapplication.data.model.QRCodeType.URL || 
            qrCode.content.startsWith("http://") || 
            qrCode.content.startsWith("https://")) {
            builder.setNeutralButton("Mở link") { _, _ ->
                openUrl(qrCode.content)
            }
        }
        
        builder.show()
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể mở link", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun confirmDelete(qrCode: QRCodeEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa QR code này?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteQRCode(qrCode)
                Toast.makeText(requireContext(), "Đã xóa", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun getTypeString(type: com.example.myapplication.data.model.QRCodeType): String {
        return when (type) {
            QRCodeType.TEXT -> "Văn bản"
            QRCodeType.URL -> "URL"
            QRCodeType.WIFI -> "WiFi"
            QRCodeType.SMS -> "SMS"
            QRCodeType.VCARD -> "Danh thiếp"
            QRCodeType.UNKNOWN -> "Không xác định"
            QRCodeType.EMAIL -> "Email"
            QRCodeType.PHONE -> "Điện thoại"
            QRCodeType.GEO -> "Địa điểm"
            QRCodeType.EVENT -> "Sự kiện"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

