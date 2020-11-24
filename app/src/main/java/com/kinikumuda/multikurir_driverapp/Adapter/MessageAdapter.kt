package com.kinikumuda.multikurir_driverapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kinikumuda.multikurir_driverapp.Model.Message
import com.kinikumuda.multikurir_driverapp.R
import kotlinx.android.synthetic.main.message_item.view.*

class MessageAdapter(val messages: ArrayList<Message>, val itemClick: (Message) -> Unit) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindForecast(messages[position])
    }

    override fun getItemCount() = messages.size

    class ViewHolder(view: View, val itemClick: (Message) -> Unit) : RecyclerView.ViewHolder(view) {

        fun bindForecast(message: Message) {
            with(message) {
                itemView.messageAdapterMessageItem.text = message.text
                itemView.setOnClickListener { itemClick(this) }
            }
        }
    }
}