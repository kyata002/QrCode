package com.example.myapplication.ui.fragment

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.database.QRCodeDatabase
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.databinding.DialogScanResultBinding
import com.example.myapplication.databinding.FragmentScanQrBinding
import com.example.myapplication.ui.viewmodel.ScanQRViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import com.example.myapplication.util.CameraHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ScanQRFragment : Fragment() {

    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanQRViewModel
    private lateinit var cameraHelper: CameraHelper
    private var isCapturing = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraHelper.startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.permission_camera_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { scanQRCodeFromBitmap(it) }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        observeViewModel()

        cameraHelper = CameraHelper(requireContext(), this, binding.cameraPreview)

        if (hasCameraPermission()) {
            cameraHelper.startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupViewModel() {
        val database = QRCodeDatabase.getDatabase(requireContext())
        val repository = QRCodeRepository(database.qrCodeDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ScanQRViewModel::class.java]
    }

    private fun setupUI() {
        binding.btnScanFromImage.setOnClickListener {
            pickImageFromGallery()
        }

        binding.fabToggleCamera.setOnClickListener {
            captureImageFromCamera()
        }

        binding.cameraPreview.setOnClickListener {
            captureImageFromCamera()
        }

        binding.switchBatchMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBatchMode(isChecked)
            val message = if (isChecked) getString(R.string.batch_scan_on) else getString(R.string.batch_scan_off)
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
        
        // Auto-scan toggle
        binding.switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            cameraHelper.enableAutoScan(isChecked) { qrContent ->
                // Auto-scan callback - run on main thread
                requireActivity().runOnUiThread {
                    viewModel.setScannedContent(qrContent)
                }
            }
            val message = if (isChecked) "Auto-scan: ON" else "Auto-scan: OFF"
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
        
        // Flashlight toggle
        binding.fabFlashlight.setOnClickListener {
            val isOn = cameraHelper.toggleFlashlight()
            binding.fabFlashlight.setImageResource(
                if (isOn) android.R.drawable.ic_menu_day 
                else android.R.drawable.ic_menu_info_details
            )
        }
    }

    private fun captureImageFromCamera() {
        if (isCapturing) return

        isCapturing = true
        Toast.makeText(requireContext(), getString(R.string.scanning), Toast.LENGTH_SHORT).show()

        cameraHelper.takePicture(
            onImageSaved = { outputFile ->
                isCapturing = false
                val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                bitmap?.let {
                    scanQRCodeFromBitmap(it)
                    outputFile.delete()
                    if (!it.isRecycled) it.recycle()
                } ?: run {
                    Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                isCapturing = false
                Toast.makeText(requireContext(), "Lỗi chụp ảnh: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun observeViewModel() {
        viewModel.scannedContent.observe(viewLifecycleOwner) { content ->
            if (content != null) {
                if (viewModel.isBatchMode.value == true) {
                    // Batch Mode: Show Snackbar and vibrate (simulated by Toast for now)
                    Snackbar.make(binding.root, "Đã quét: $content", Snackbar.LENGTH_SHORT)
                        .setAction(getString(R.string.copy)) {
                            copyToClipboard(content)
                        }
                        .show()
                    // Auto save in batch mode could be a future improvement
                    viewModel.resetScanResult() // Reset immediately for next scan
                } else {
                    // Normal Mode: Show Dialog
                    showScanResultDialog(content)
                }
            }
        }

        viewModel.scanError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                viewModel.resetSaveSuccess()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun scanQRCodeFromBitmap(bitmap: Bitmap) {
        viewModel.scanQRCodeFromBitmap(bitmap)
    }

    private fun showScanResultDialog(content: String) {
        val dialogBinding = DialogScanResultBinding.inflate(LayoutInflater.from(requireContext()))

        val type = viewModel.scannedType.value
        dialogBinding.tvDialogQrType.text = type?.let { getTypeString(it) } ?: "Không xác định"

        setupDialogContentText(dialogBinding, content)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.setOnDismissListener {
            viewModel.resetScanResult()
        }

        dialogBinding.btnDialogCopy.setOnClickListener {
            copyToClipboard(content)
        }

        dialogBinding.btnDialogSave.setOnClickListener {
            viewModel.saveScannedQR()
        }

        dialogBinding.btnDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDialogContentText(dialogBinding: DialogScanResultBinding, content: String) {
        val type = viewModel.scannedType.value
        if (type == QRCodeType.URL ||
            content.startsWith("http://") ||
            content.startsWith("https://")
        ) {
            val spannable = SpannableString(content)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUrl(content)
                }
            }
            spannable.setSpan(
                clickableSpan,
                0,
                content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(requireContext().getColor(android.R.color.holo_blue_dark)),
                0,
                content.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            dialogBinding.tvDialogScannedContent.text = spannable
            dialogBinding.tvDialogScannedContent.movementMethod =
                android.text.method.LinkMovementMethod.getInstance()
        } else {
            dialogBinding.tvDialogScannedContent.text = content
            dialogBinding.tvDialogScannedContent.movementMethod = null
        }
    }

    private fun getTypeString(type: QRCodeType): String {
        return when (type) {
            QRCodeType.TEXT -> getString(R.string.text)
            QRCodeType.URL -> getString(R.string.url)
            QRCodeType.WIFI -> getString(R.string.wifi)
            QRCodeType.SMS -> getString(R.string.sms)
            QRCodeType.VCARD -> getString(R.string.vcard)
            QRCodeType.EMAIL -> "Email"
            QRCodeType.PHONE -> "Điện thoại"
            QRCodeType.GEO -> "Địa điểm"
            QRCodeType.EVENT -> "Sự kiện"
            QRCodeType.UNKNOWN -> "Không xác định"
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể mở link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(content: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code", content)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.copy), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraHelper.shutdown()
        _binding = null
    }
}
