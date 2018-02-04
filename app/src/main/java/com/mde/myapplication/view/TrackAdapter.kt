package com.mde.myapplication.view

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mde.myapplication.R
import com.mde.myapplication.utils.bind
import com.squareup.picasso.Picasso


/**
 * Track adapter to display card
 */
class TrackAdapter(val tracks: MutableList<TrackItem>, val listener: OnPlayClickListener) : RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

    /**
     * Event when card are clicked
     */
    interface OnPlayClickListener {
        fun onTrackClicked(trackId: String)
    }

    /**
     * A track item is a visual representation of a track
     */
    data class TrackItem(val id: String,
                         val name: String, val artistName: String,
                         val coverUri: String?,
                         var isPlaying: Boolean = false)

    inner class TrackHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverIv by itemView.bind<ImageView>(R.id.track_cover)
        val nameTv by itemView.bind<TextView>(R.id.track_name)
        val artistTv by itemView.bind<TextView>(R.id.track_artist)
        val playBtn by itemView.bind<ImageView>(R.id.track_play_btn)

        /**
         * Update track info
         */
        fun update(track: TrackItem) {
            itemView.setOnClickListener { listener.onTrackClicked(track.id) }
            playBtn.setImageResource(if (track.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            nameTv.text = track.name
            artistTv.text = track.artistName
            Picasso.with(itemView.context)
                    .load(track.coverUri)
                    .into(coverIv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(LayoutInflater.from(parent.context).inflate(R.layout.track_card, parent, false))

    override fun onBindViewHolder(holder: TrackHolder, position: Int) = holder.update(tracks[position])

    override fun getItemCount(): Int = tracks.size

    /**
     * Function to update list
     */
    fun updateListItem(items: List<TrackItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = tracks[oldItemPosition].id == items[newItemPosition].id

            override fun getOldListSize(): Int = tracks.size

            override fun getNewListSize(): Int = items.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    tracks[oldItemPosition] == items[newItemPosition]
        })
        tracks.clear()
        tracks.addAll(items.map { it.copy() })
        diffResult.dispatchUpdatesTo(this)
    }
}


