package com.convenient.salescall.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.convenient.salescall.R
import com.convenient.salescall.datas.UserVoiceInfo

class UserVoiceInfoAdapter(
    private val context: Context,
    private val dataList: List<UserVoiceInfo>
) : BaseAdapter() {
    override fun getCount(): Int = dataList.size
    override fun getItem(position: Int): Any = dataList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_user_voice_info, parent, false)
        val item = dataList[position]

        view.findViewById<TextView>(R.id.tvDimensionType).text =
            when (item.dimensionType) {
                1 ->
                    "日"

                2 ->
                    "周"

                3 ->
                    "月"

                4 ->
                    "年"

                else ->
                    "未知"
            }
        // 设置不同颜色
        val colorRes = when (item.dimensionType) {
            1 -> R.color.type_day
            2 -> R.color.type_week
            3 -> R.color.type_month
            4 -> R.color.type_year
            else -> R.color.white
        }
        view.findViewById<TextView>(R.id.tvDimensionType)
            .setTextColor(ContextCompat.getColor(context, colorRes))
        view.findViewById<TextView>(R.id.tvVoiceCount).text = "${item.voiceCount}"
        view.findViewById<TextView>(R.id.tvConnectedCount).text = "${item.connectedCount}"
        view.findViewById<TextView>(R.id.tvUnconnectedCount).text = "${item.unconnectedCount}"
        view.findViewById<TextView>(R.id.tvDuration).text = "${item.durationMsFormat}"

        return view
    }
}