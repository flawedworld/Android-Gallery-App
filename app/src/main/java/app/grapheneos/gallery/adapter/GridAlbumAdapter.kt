package app.grapheneos.gallery.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.MediaStoreSignature
import app.grapheneos.gallery.Album
import app.grapheneos.gallery.GlideApp
import app.grapheneos.gallery.R
import app.grapheneos.gallery.databinding.AlbumHolderBinding
import app.grapheneos.gallery.ui.BottomNavFrag
import app.grapheneos.gallery.ui.MainActivity
import com.google.android.material.navigation.NavigationBarView

class GridAlbumAdapter(private val frag: BottomNavFrag) : ListAdapter<Album,
        AlbumHolder>(Album.DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(
            AlbumHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        GlideApp.with(holder.binding.ivThumbnailAlbum)
            .load(getItem(position).mediaItems[0].uri)
            .centerCrop()
            .signature(
                MediaStoreSignature(
                    null, getItem(position)
                        .mediaItems[0].dateModified, 0
                )
            )
            .error(R.drawable.ic_baseline_image_not_supported_24)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.ivThumbnailAlbum)

        holder.binding.tvAlbumName.text = getItem(position).name

        holder.binding.ivThumbnailAlbum.setOnClickListener {
            if ((frag.binding.bnvMain as NavigationBarView).selectedItemId == R.id.miAlbums
                || frag.requireActivity().intent.action == Intent.ACTION_PICK || frag.requireActivity()
                    .intent.action ==
                Intent.ACTION_GET_CONTENT
            ) {
                MainActivity.currentListPosition = 0

                MainActivity.currentAlbumName = getItem(holder.layoutPosition).name

                frag.setSharedAxisTransition()

                frag.findNavController().navigate(
                    R.id.action_bottomNavFrag_to_albumDetailFrag
                )
            }
        }
    }
}

class AlbumHolder(val binding: AlbumHolderBinding) :
    RecyclerView.ViewHolder(binding.root)