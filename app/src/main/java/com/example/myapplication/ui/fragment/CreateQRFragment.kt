package com.example.myapplication.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.database.QRCodeDatabase
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.databinding.FragmentCreateQrBinding
import com.example.myapplication.ui.viewmodel.CreateQRViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory

class CreateQRFragment : Fragment() {
    
    private var _binding: FragmentCreateQrBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: CreateQRViewModel
    private val qrTypes = arrayOf(
        "Văn bản",
        "URL",
        "WiFi",
        "SMS",
        "Danh thiếp (vCard)",
        "Email",
        "Điện thoại",
        "Địa điểm",
        "Sự kiện"
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateQrBinding.inflate(inflater, container, false)
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
        viewModel = ViewModelProvider(this, factory)[CreateQRViewModel::class.java]
    }
    
    private fun setupUI() {
        // Setup QR Type Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, qrTypes)
        binding.spinnerQrType.setAdapter(adapter)
        binding.spinnerQrType.setText(qrTypes[0], false)
        binding.spinnerQrType.threshold = 0
        binding.spinnerQrType.setOnClickListener {
            binding.spinnerQrType.showDropDown()
        }
        binding.spinnerQrType.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.spinnerQrType.showDropDown()
            }
        }
        binding.spinnerQrType.setOnItemClickListener { _, _, position, _ ->
            val type = when (position) {
                0 -> QRCodeType.TEXT
                1 -> QRCodeType.URL
                2 -> QRCodeType.WIFI
                3 -> QRCodeType.SMS
                4 -> QRCodeType.VCARD
                5 -> QRCodeType.EMAIL
                6 -> QRCodeType.PHONE
                7 -> QRCodeType.GEO
                8 -> QRCodeType.EVENT
                else -> QRCodeType.TEXT
            }
            viewModel.setQRType(type)
            updateUIForType(type)

            // Đồng bộ lựa chọn với ChipGroup
            when (position) {
                0 -> binding.chipGroupQrType.check(R.id.chip_text)
                1 -> binding.chipGroupQrType.check(R.id.chip_url)
                2 -> binding.chipGroupQrType.check(R.id.chip_wifi)
                3 -> binding.chipGroupQrType.check(R.id.chip_sms)
                4 -> binding.chipGroupQrType.check(R.id.chip_vcard)
                5 -> binding.chipGroupQrType.check(R.id.chip_email)
                6 -> binding.chipGroupQrType.check(R.id.chip_phone)
                7 -> binding.chipGroupQrType.check(R.id.chip_geo)
                8 -> binding.chipGroupQrType.check(R.id.chip_event)
            }
        }

        // QR Type ChipGroup
        binding.chipGroupQrType.check(R.id.chip_text)
        binding.chipGroupQrType.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val (position, type) = when (checkedId) {
                R.id.chip_text -> 0 to QRCodeType.TEXT
                R.id.chip_url -> 1 to QRCodeType.URL
                R.id.chip_wifi -> 2 to QRCodeType.WIFI
                R.id.chip_sms -> 3 to QRCodeType.SMS
                R.id.chip_vcard -> 4 to QRCodeType.VCARD
                R.id.chip_email -> 5 to QRCodeType.EMAIL
                R.id.chip_phone -> 6 to QRCodeType.PHONE
                R.id.chip_geo -> 7 to QRCodeType.GEO
                R.id.chip_event -> 8 to QRCodeType.EVENT
                else -> 0 to QRCodeType.TEXT
            }
            binding.spinnerQrType.setText(qrTypes[position], false)
            viewModel.setQRType(type)
            updateUIForType(type)
        }
        
        // WiFi Security Spinner
        val securityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            arrayOf("Không", "WPA/WPA2")
        )
        binding.etWifiSecurity.setAdapter(securityAdapter)
        binding.etWifiSecurity.setText("WPA/WPA2", false)
        
        // Buttons
        binding.btnGenerate.setOnClickListener {
            generateQRCode()
        }
        
        binding.btnCustomize.setOnClickListener {
            // TODO: Navigate to customize screen
            Toast.makeText(requireContext(), "Tính năng tùy chỉnh sẽ được thêm sau", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnSave.setOnClickListener {
            viewModel.saveQRCode()
        }
        
        binding.btnShare.setOnClickListener {
            shareQRCode()
        }
        
        // Initialize with TEXT type
        viewModel.setQRType(QRCodeType.TEXT)
        updateUIForType(QRCodeType.TEXT)
    }
    
    private fun updateUIForType(type: QRCodeType) {
        binding.layoutTextInput.visibility = when (type) {
            QRCodeType.TEXT,
            QRCodeType.URL,
            QRCodeType.EMAIL,
            QRCodeType.PHONE,
            QRCodeType.GEO,
            QRCodeType.EVENT -> View.VISIBLE
            else -> View.GONE
        }
        binding.layoutWifi.visibility = if (type == QRCodeType.WIFI) View.VISIBLE else View.GONE
        binding.layoutSms.visibility = if (type == QRCodeType.SMS) View.VISIBLE else View.GONE
        binding.layoutVcard.visibility = if (type == QRCodeType.VCARD) View.VISIBLE else View.GONE
        
        // Update hint
        binding.layoutTextInput.hint = when (type) {
            QRCodeType.TEXT -> "Nhập văn bản"
            QRCodeType.URL -> "Nhập URL (https://...)"
            QRCodeType.EMAIL -> "Nhập email hoặc dạng mailto:example@mail.com"
            QRCodeType.PHONE -> "Nhập số điện thoại hoặc dạng tel:0123456789"
            QRCodeType.GEO -> "Nhập vị trí, ví dụ geo:10.123,106.123"
            QRCodeType.EVENT -> "Nhập nội dung sự kiện (BEGIN:VEVENT...)"
            else -> "Nhập nội dung"
        }
    }
    
    private fun generateQRCode() {
        val type = viewModel.currentType.value ?: QRCodeType.TEXT
        
        when (type) {
            QRCodeType.TEXT -> {
                val text = binding.etTextInput.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    viewModel.generateQRCode(text = text)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập văn bản", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeType.URL -> {
                val url = binding.etTextInput.text?.toString() ?: ""
                if (url.isNotEmpty()) {
                    viewModel.generateQRCode(url = url)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập URL", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeType.WIFI -> {
                val ssid = binding.etWifiSsid.text?.toString() ?: ""
                val password = binding.etWifiPassword.text?.toString() ?: ""
                val security = binding.etWifiSecurity.text?.toString() ?: "WPA/WPA2"
                if (ssid.isNotEmpty()) {
                    viewModel.generateQRCode(
                        wifiSSID = ssid,
                        wifiPassword = password,
                        wifiSecurity = security
                    )
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập tên mạng WiFi", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeType.SMS -> {
                val number = binding.etSmsNumber.text?.toString() ?: ""
                val message = binding.etSmsMessage.text?.toString() ?: ""
                if (number.isNotEmpty()) {
                    viewModel.generateQRCode(smsNumber = number, smsMessage = message)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeType.VCARD -> {
                val name = binding.etVcardName.text?.toString() ?: ""
                if (name.isNotEmpty()) {
                    viewModel.generateQRCode(
                        vcardName = name,
                        vcardPhone = binding.etVcardPhone.text?.toString(),
                        vcardEmail = binding.etVcardEmail.text?.toString(),
                        vcardOrg = binding.etVcardOrg.text?.toString(),
                        vcardAddress = binding.etVcardAddress.text?.toString()
                    )
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập tên", Toast.LENGTH_SHORT).show()
                }
            }
            QRCodeType.EMAIL,
            QRCodeType.PHONE,
            QRCodeType.GEO,
            QRCodeType.EVENT -> {
                val text = binding.etTextInput.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    viewModel.generateQRCode(text = text)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }
    
    private fun observeViewModel() {
        viewModel.qrCodeBitmap.observe(viewLifecycleOwner) { bitmap ->
            bitmap?.let {
                binding.ivQrCode.setImageBitmap(it)
            }
        }
        
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                viewModel.resetSaveSuccess()
            }
        }
    }
    
    private fun shareQRCode() {
        val bitmap = viewModel.qrCodeBitmap.value
        if (bitmap != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, bitmapToUri(bitmap))
                putExtra(Intent.EXTRA_TEXT, viewModel.currentContent.value ?: "")
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_qr)))
        } else {
            Toast.makeText(requireContext(), "Vui lòng tạo QR code trước", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun bitmapToUri(bitmap: Bitmap): android.net.Uri? {
        // Simplified - in production, save to file first
        return null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

