package com.example.myapplication.ui.fragment

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.data.database.QRCodeDatabase
import com.example.myapplication.data.repository.QRCodeRepository
import com.example.myapplication.databinding.FragmentScanQrBinding
import com.example.myapplication.ui.viewmodel.ScanQRViewModel
import com.example.myapplication.ui.viewmodel.ViewModelFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanQRFragment : Fragment() {
    
    private var _binding: FragmentScanQrBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ScanQRViewModel
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isCapturing = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
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
        
        // Start camera preview
        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    fun handleScanResult(content: String) {
        viewModel.setScannedContent(content)
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
            // Capture image from camera and scan
            captureImageFromCamera()
        }
        
        binding.btnCopy.setOnClickListener {
            copyToClipboard()
        }
        
        binding.btnSaveScan.setOnClickListener {
            viewModel.saveScannedQR()
        }
        
        // Also allow clicking on preview to scan
        binding.cameraPreview.setOnClickListener {
            captureImageFromCamera()
        }
    }
    
    private fun captureImageFromCamera() {
        // Prevent multiple captures
        if (isCapturing) {
            return
        }
        
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show()
            return
        }
        
        isCapturing = true
        
        // Create output file
        val outputFile = File(requireContext().cacheDir, "QR_SCAN_${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        // Take picture
        imageCapture?.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    // Load bitmap from saved file
                    val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                    bitmap?.let {
                        // Show loading message
                        Toast.makeText(requireContext(), "Đang quét...", Toast.LENGTH_SHORT).show()
                        scanQRCodeFromBitmap(it)
                        // Clean up temp file
                        outputFile.delete()
                        // Recycle bitmap to free memory
                        if (!it.isRecycled) {
                            it.recycle()
                        }
                    } ?: run {
                        Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    Toast.makeText(requireContext(), "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun observeViewModel() {
        viewModel.scannedContent.observe(viewLifecycleOwner) { content ->
            if (content != null) {
                binding.cardResult.visibility = View.VISIBLE
                setupContentText(content)
            }
        }
        
        viewModel.scannedType.observe(viewLifecycleOwner) { type ->
            // Update UI based on type if needed
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
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Lỗi khởi động camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun scanQRCodeFromBitmap(bitmap: Bitmap) {
        viewModel.scanQRCodeFromBitmap(bitmap)
    }
    
    private fun setupContentText(content: String) {
        val type = viewModel.scannedType.value
        if (type == com.example.myapplication.data.model.QRCodeType.URL || 
            content.startsWith("http://") || 
            content.startsWith("https://")) {
            // Make it clickable
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
            binding.tvScannedContent.text = spannable
            binding.tvScannedContent.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        } else {
            binding.tvScannedContent.text = content
            binding.tvScannedContent.movementMethod = null
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
    
    private fun copyToClipboard() {
        val content = viewModel.scannedContent.value
        if (content != null) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code", content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Đã sao chép", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}

