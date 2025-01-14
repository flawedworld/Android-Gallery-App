package app.grapheneos.gallery

import androidx.recyclerview.widget.DiffUtil

data class Album(var name: String, var mediaItems: MutableList<ListItem.MediaItem>) {

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(oldItem: Album, newItem: Album) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Album, newItem: Album) =
                oldItem.mediaItems == newItem.mediaItems
        }
    }
}