package app.grapheneos.gallery

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import app.grapheneos.gallery.ui.MainActivity
import app.grapheneos.gallery.ui.MainViewModel

class MyItemKeyProvider(
    private val viewModel: MainViewModel,
    private val isAlbum: Boolean = false
) : ItemKeyProvider<Long>(SCOPE_CACHED) {

    override fun getKey(position: Int): Long? {
        return if (isAlbum) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems
                ?.get(position)?.id
        } else {
            viewModel.recyclerViewItems.value?.get(position)?.id
        }
    }

    override fun getPosition(key: Long): Int {
        return if (isAlbum) {
            viewModel.albums.value?.find { it.name == MainActivity.currentAlbumName }?.mediaItems
                ?.indexOfFirst { it.id == key } ?: RecyclerView.NO_POSITION
        } else {
            viewModel.recyclerViewItems.value?.indexOfFirst { it.id == key }
                ?: RecyclerView.NO_POSITION
        }
    }
}

