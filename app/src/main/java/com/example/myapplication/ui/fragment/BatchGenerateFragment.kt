package com.example.myapplication.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentBatchGenerateBinding
import com.example.myapplication.util.BatchQRGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BatchGenerateFragment : Fragment() {
    
    private var _binding: FragmentBatchGenerateBinding? = null
    private val binding get() = _binding!!
    
    private var csvUri: Uri? = null
    private var generatedResults: List<Pair<String, Bitmap>> = emptyList()
    
    private val csvPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                csvUri = uri
                binding.tvCsvFile.text = "File selected: ${uri.lastPathSegment}"
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnSelectCsv.setOnClickListener {
            selectCSVFile()
        }
        
        binding.btnGenerate.setOnClickListener {
            generateBatch()
        }
        
        binding.btnExportZip.setOnClickListener {
            exportAsZip()
        }
    }
    
    private fun selectCSVFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        csvPicker.launch(intent)
    }
    
    private fun generateBatch() {
        val contents = mutableListOf<String>()
        
        // Get contents from CSV if available
        csvUri?.let { uri ->
            contents.addAll(BatchQRGenerator.parseCSV(requireContext(), uri))
        }
        
        // Add manual input
        val manualInput = binding.etBatchInput.text?.toString() ?: ""
        if (manualInput.isNotEmpty()) {
            contents.addAll(BatchQRGenerator.parseTextInput(manualInput))
        }
        
        if (contents.isEmpty()) {
            Toast.makeText(requireContext(), "No content to generate", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show progress
        binding.cardProgress.visibility = View.VISIBLE
        binding.progressBar.max = contents.size
        binding.btnGenerate.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val size = binding.sliderBatchSize.value.toInt()
                val includeText = binding.switchIncludeText.isChecked
                
                generatedResults = withContext(Dispatchers.Default) {
                    BatchQRGenerator.generateBatch(
                        contents = contents,
                        size = size,
                        includeText = includeText
                    ) { current, total ->
                        launch(Dispatchers.Main) {
                            binding.progressBar.progress = current
                            binding.tvProgress.text = "Generating: $current/$total"
                        }
                    }
                }
                
                Toast.makeText(
                    requireContext(),
                    "Generated ${generatedResults.size} QR codes",
                    Toast.LENGTH_SHORT
                ).show()
                
                binding.btnExportZip.isEnabled = true
                
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Generation failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnGenerate.isEnabled = true
            }
        }
    }
    
    private fun exportAsZip() {
        if (generatedResults.isEmpty()) {
            Toast.makeText(requireContext(), "Generate QR codes first", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                binding.btnExportZip.isEnabled = false
                binding.cardProgress.visibility = View.VISIBLE
                binding.progressBar.max = generatedResults.size
                
                val zipFile = File(
                    requireContext().getExternalFilesDir(null),
                    "qr_batch_${System.currentTimeMillis()}.zip"
                )
                
                val success = withContext(Dispatchers.IO) {
                    BatchQRGenerator.exportAsZip(
                        results = generatedResults,
                        zipFile = zipFile
                    ) { current, total ->
                        launch(Dispatchers.Main) {
                            binding.progressBar.progress = current
                            binding.tvProgress.text = "Exporting: $current/$total"
                        }
                    }
                }
                
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Exported to: ${zipFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Export failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Export failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnExportZip.isEnabled = true
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
