package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ObservationsAdapter(private var observationsList: List<Observation>) : RecyclerView.Adapter<ObservationsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdNameTextView: TextView = itemView.findViewById(R.id.birdNameTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val observationImageView: ImageView = itemView.findViewById(R.id.observationImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.observation_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return observationsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val observation = observationsList[position]
        holder.birdNameTextView.text = observation.birdName
        holder.descriptionTextView.text = observation.description
        holder.dateTextView.text = observation.date
        Glide.with(holder.itemView.context).load(observation.imageUri).into(holder.observationImageView)
    }
    fun updateObservations(newObservations: List<Observation>) {
        observationsList = newObservations
        notifyDataSetChanged()
    }


}
