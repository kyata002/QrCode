package com.example.myapplication.ui.adapter

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.QRCodeEntity
import com.example.myapplication.data.model.QRCodeType
import com.example.myapplication.util.QRCodeGenerator
import java.text.SimpleDateFormat
import java.util.*

class QRHistoryAdapter(
    private val onItemClick: (QRCodeEntity) -> Unit,
    private val onDeleteClick: (QRCodeEntity) -> Unit
) : ListAdapter<QRCodeEntity, QRHistoryAdapter.QRViewHolder>(QRDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QRViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_qr_history, parent, false)
        return QRViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: QRViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class QRViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val qrPreview: ImageView = itemView.findViewById(R.id.iv_qr_preview)
        private val contentText: TextView = itemView.findViewById(R.id.tv_content)
        private val typeText: TextView = itemView.findViewById(R.id.tv_type)
        private val dateText: TextView = itemView.findViewById(R.id.tv_date)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)
        private val widgetView: View = itemView
        
        fun bind(qrCode: QRCodeEntity) {
            setupContentText(qrCode.content, qrCode.type)
            typeText.text = getTypeString(qrCode.type)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateText.text = dateFormat.format(Date(qrCode.createdAt))
            
            // Generate QR preview
            val bitmap = QRCodeGenerator.generateQRCode(qrCode.content)
            bitmap?.let {
                qrPreview.setImageBitmap(it)
            }
            
            itemView.setOnClickListener {
                onItemClick(qrCode)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(qrCode)
            }
        }
        
        private fun setupContentText(content: String, type: QRCodeType) {
            if (type == QRCodeType.URL || 
                content.startsWith("http://") || 
                content.startsWith("https://")) {
                // Make it clickable
                val spannable = SpannableString(content)
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        openUrl(widget.context, content)
                    }
                }
                spannable.setSpan(
                    clickableSpan,
                    0,
                    content.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(widgetView.context.getColor(android.R.color.holo_blue_dark)),
                    0,
                    content.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                contentText.text = spannable
                contentText.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            } else {
                contentText.text = content
                contentText.movementMethod = null
            }
        }
        
        private fun openUrl(context: android.content.Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể mở link", Toast.LENGTH_SHORT).show()
            }
        }
        
        private fun getTypeString(type: QRCodeType): String {
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
    }
    
    class QRDiffCallback : DiffUtil.ItemCallback<QRCodeEntity>() {
        override fun areItemsTheSame(oldItem: QRCodeEntity, newItem: QRCodeEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: QRCodeEntity, newItem: QRCodeEntity): Boolean {
            return oldItem == newItem
        }
    }
}

