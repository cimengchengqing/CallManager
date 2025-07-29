package com.convenient.salescall.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.convenient.salescall.R
import com.convenient.salescall.datas.CallLogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CallLogAdapter(
    private val context: Context,
    private val callLogs: MutableList<CallLogItem> // 改为MutableList
) : ArrayAdapter<CallLogItem>(context, 0, callLogs) {

    // 添加更新数据的方法
    fun updateData(newData: List<CallLogItem>) {
        callLogs.clear() // 清空旧数据
        callLogs.addAll(newData) // 添加新数据
        notifyDataSetChanged() // 通知适配器数据已更新
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_call_log, parent, false)

        val callLog = getItem(position)
        callLog?.let {
            // 只显示呼出电话
            if (it.type == "呼出") {
                view.findViewById<TextView>(R.id.tvNumber).text = "号码: ${it.number}"

                // 设置通话类型和颜色
                val tvType = view.findViewById<TextView>(R.id.tvType)
                if (it.duration == 0) {
                    tvType.text = "未接通"
                    tvType.setTextColor(Color.RED)
                } else {
                    tvType.text = "接通"
                    tvType.setTextColor(Color.GREEN)
                }

                // 设置日期
                view.findViewById<TextView>(R.id.tvDate).text = "日期: ${formatDate(it.date)}"

                // 设置通话时长
                val tvDuration = view.findViewById<TextView>(R.id.tvDuration)
                if (it.duration > 0) {
                    tvDuration.text = formatDuration(it.duration)
                    tvDuration.visibility = View.VISIBLE
                } else {
                    tvDuration.visibility = View.GONE
                }
            }
        }

        return view
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(seconds: Int): String {
        val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
        val remainingSeconds = seconds % 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
            else -> String.format("%02d:%02d", minutes, remainingSeconds)
        }
    }
}