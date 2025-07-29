package com.convenient.salescall.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.convenient.salescall.R
import com.convenient.salescall.datas.RecordFileInfo
import java.text.SimpleDateFormat
import java.util.*

class RecordFileAdapter(
    private val context: Context,
    private var recordFiles: List<RecordFileInfo>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = recordFiles.size

    override fun getItem(position: Int): RecordFileInfo = recordFiles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.item_record_log, parent, false)

        val viewHolder = if (convertView == null) {
            ViewHolder(
                tvFileName = view.findViewById(R.id.tvFileName),
                tvCreateTime = view.findViewById(R.id.tvCreateTime),
                tvFileSize = view.findViewById(R.id.tvFileSize),
                tvUploadStatus = view.findViewById(R.id.tvUploadStatus)
            ).also { view.tag = it }
        } else {
            view.tag as ViewHolder
        }

        val recordFile = getItem(position)
        bindData(viewHolder, recordFile)

        return view
    }

    private fun bindData(viewHolder: ViewHolder, recordFile: RecordFileInfo) {
        // 设置文件名和格式（合并显示，用点分割）
        val fileNameWithoutExtension = recordFile.fileName.substringBeforeLast(".")
        val fileExtension = recordFile.fileType
        viewHolder.tvFileName.text = "$fileNameWithoutExtension.$fileExtension"

        // 设置创建时间（年/月/日格式）
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        viewHolder.tvCreateTime.text = dateFormat.format(recordFile.createTime)

        // 设置文件大小
        viewHolder.tvFileSize.text = recordFile.getFormattedSize()
        // 设置上传状态
        viewHolder.tvUploadStatus.text = recordFile.getUploadStatusText()
        viewHolder.tvUploadStatus.setTextColor(recordFile.getUploadStatusColor())
    }

    fun updateData(newRecordFiles: List<RecordFileInfo>) {
        this.recordFiles = newRecordFiles
        notifyDataSetChanged()
    }

    private data class ViewHolder(
        val tvFileName: TextView,
        val tvCreateTime: TextView,
        val tvFileSize: TextView,
        val tvUploadStatus:TextView
    )
}