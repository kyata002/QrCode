package com.example.myapplication.ui.fragment

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentCustomizeQrBinding
import com.example.myapplication.util.QRCodeGenerator
import com.example.myapplication.data.model.QRCodeCustomization
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class CustomizeQRFragment : Fragment() {
    
    private var _binding: FragmentCustomizeQrBinding? = null
    private val binding get() = _binding!!
    
    private var fgColor = Color.BLACK
    private var bgColor = Color.WHITE
    private var qrSize = 500
    private var useRoundedCorners = false
    
    private var currentBitmap: Bitmap? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomizeQrBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        updatePreview()
    }
    
    private fun setupUI() {
        binding.etContent.doOnTextChanged { _, _, _, _ -> updatePreview() }
        
        binding.chipGroupPattern.setOnCheckedStateChangeListener { _, checkedIds ->
            useRoundedCorners = checkedIds.firstOrNull() != binding.chipSquare.id
            updatePreview()
        }
        
        binding.btnPickFgColor.setOnClickListener { showColorPicker(true) }
        binding.btnPickBgColor.setOnClickListener { showColorPicker(false) }
        binding.switchGradient.setOnCheckedChangeListener { _, _ -> updatePreview() }
        
        binding.sliderSize.addOnChangeListener { _, value, _ ->
            qrSize = value.toInt()
            updatePreview()
        }
        
        binding.btnSave.setOnClickListener { saveQRCode() }
        binding.btnShare.setOnClickListener { shareQRCode() }
    }
    
    private fun showColorPicker(isForeground: Boolean) {
        ColorPickerDialog.Builder(requireContext())
            .setTitle(if (isForeground) "Pick Foreground Color" else "Pick Background Color")
            .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                val color = envelope.color
                if (isForeground) {
                    fgColor = color
                    binding.viewFgColor.setBackgroundColor(color)
                } else {
                    bgColor = color
                    binding.viewBgColor.setBackgroundColor(color)
                }
                updatePreview()
            })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun updatePreview() {
        val content = binding.etContent.text?.toString() ?: ""
        if (content.isEmpty()) return
        
        val customization = QRCodeCustomization(
            size = qrSize,
            foregroundColor = fgColor,
            backgroundColor = bgColor,
            borderRadius = if (useRoundedCorners) 8 else 0
        )
        
        currentBitmap = QRCodeGenerator.generateQRCode(content, customization)
        binding.ivQrPreview.setImageBitmap(currentBitmap)
    }
    
    private fun saveQRCode() {
        val bitmap = currentBitmap ?: return
        try {
            val file = File(requireContext().getExternalFilesDir(null), "custom_qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareQRCode() {
        val bitmap = currentBitmap ?: return
        try {
            val file = File(requireContext().cacheDir, "share_qr.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share QR Code"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
