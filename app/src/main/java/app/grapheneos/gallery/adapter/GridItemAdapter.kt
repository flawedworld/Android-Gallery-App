package app.grapheneos.gallery.adapter

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import app.grapheneos.gallery.GlideApp
import app.grapheneos.gallery.ListItem
import app.grapheneos.gallery.R
import app.grapheneos.gallery.databinding.LayoutSearchBinding
import app.grapheneos.gallery.databinding.ListGridHeaderBinding
import app.grapheneos.gallery.databinding.ListGridMediaItemHolderBinding
import app.grapheneos.gallery.ui.BinFrag
import app.grapheneos.gallery.ui.MainActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.shape.ShapeAppearanceModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Based on:
 * https://github.com/android/animation-samples/tree/main/GridToPager
 */
class GridItemAdapter(
    private val frag: Fragment,
    private val isAlbum: Boolean,
    val onClick: (extras: FragmentNavigator.Extras, position: Int) -> Unit
) :
    ListAdapter<ListItem, ViewHolder>(ListItem.ListItemDiffCallback) {

    private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()
    var tracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder(
                ListGridHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )

            ITEM_VIEW_TYPE_SEARCH -> SearchViewHolder(
                LayoutSearchBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )

            else -> MediaItemHolder(
                ListGridMediaItemHolderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MediaItemHolder) {
            holder.binding.image.isActivated = tracker?.isSelected(getItemId(position)) == true

            if (holder.binding.image.isActivated) {
                holder.binding.image.apply {
                    shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(70f)
                    animate().scaleX(0.75f).scaleY(0.75f).duration = 100
                }

            } else if (!holder.binding.image.isActivated) {

                holder.binding.image.apply {
                    shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(0f)
                    animate().scaleX(1f).scaleY(1f).duration = 100
                }
            }

            if ((getItem(position) as ListItem.MediaItem).type ==
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            ) {
                holder.binding.ivPlayMediaItem.isVisible = true
            }

            holder.binding.image.transitionName = getItemId(holder.layoutPosition).toString()

            GlideApp.with(holder.binding.image)
                .load((getItem(position) as ListItem.MediaItem).uri)
                .error(R.drawable.ic_baseline_image_not_supported_24)
                .centerCrop()
                .signature(
                    MediaStoreSignature(
                        null,
                        (getItem(position) as ListItem.MediaItem).dateModified, 0
                    )
                )
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any,
                        target: Target<Drawable?>, isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPosition != holder.layoutPosition) {
                            return false
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return false
                        }

                        frag.startPostponedEnterTransition()

                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPosition != holder.layoutPosition) {
                            return false
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return false
                        }

                        frag.startPostponedEnterTransition()

                        return false
                    }
                }
                )
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.binding.image)

            holder.binding.image.setOnClickListener {
                if (frag.requireActivity().intent.action == Intent.ACTION_PICK ||
                    frag.requireActivity().intent.action == Intent.ACTION_GET_CONTENT
                ) {
                    if (frag.requireActivity().intent.getBooleanExtra(
                            Intent.EXTRA_ALLOW_MULTIPLE,
                            false
                        ) && frag.requireActivity().intent.action == Intent.ACTION_GET_CONTENT
                    ) {
                        tracker?.select(getItemId(holder.layoutPosition))

                        return@setOnClickListener
                    } else {
                        val intent = Intent().apply {
                            data = (getItem(holder.layoutPosition) as ListItem.MediaItem).uri
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        frag.requireActivity().setResult(Activity.RESULT_OK, intent)
                        frag.requireActivity().finish()

                        return@setOnClickListener
                    }
                }

                if (frag !is BinFrag) {
                    MainActivity.currentListPosition = holder.layoutPosition
                    MainActivity.currentViewPagerPosition = if (isAlbum) {
                        holder.layoutPosition
                    } else {
                        (getItem(holder.layoutPosition) as ListItem.MediaItem).viewPagerPosition
                    }
                }

                onClick(
                    FragmentNavigatorExtras(
                        it to it.transitionName
                    ),
                    holder.layoutPosition
                )
            }
        } else if (holder is HeaderViewHolder) {
            if ((getItem(position) as ListItem.Header).description.isNullOrEmpty()) {
                holder.binding.tvDate.text =
                    SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL).format(
                        Date(getItemId(position))
                    )
            } else {
                holder.binding.tvDate.text = (getItem(position) as ListItem.Header).description
            }
        } else if (holder is SearchViewHolder) {
            val searchManager =
                frag.activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager

            holder.binding.searchInput.setSearchableInfo(
                searchManager.getSearchableInfo(
                    frag.activity?.componentName
                )
            )

            holder.binding.btnSearchDate.setOnClickListener {
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select dates")
                    .build()
                    .also { picker ->
                        picker.show(frag.childFragmentManager, it.toString())
                        picker.addOnPositiveButtonClickListener { pair ->
                            val min = Calendar.getInstance().apply {
                                timeInMillis = pair.first
                                println("time: " + get(Calendar.HOUR_OF_DAY))
                                set(Calendar.HOUR_OF_DAY, 0)
                            }
                            val max = Calendar.getInstance().apply {
                                timeInMillis = pair.second
                                println("time: " + get(Calendar.HOUR_OF_DAY))
                                set(Calendar.HOUR_OF_DAY, 23)
                            }

                            holder.binding.searchInput.setQuery(
                                "DATE:${min.timeInMillis.div(1000)}/${max.timeInMillis.div(1000)}",
                                true
                            )
                            holder.binding.searchInput.setQuery(null, false)
                        }
                    }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.MediaItem -> (getItem(position) as ListItem.MediaItem).type
            is ListItem.Header -> ITEM_VIEW_TYPE_HEADER
            is ListItem.Search -> ITEM_VIEW_TYPE_SEARCH
            else -> throw IllegalStateException("Unknown ViewType")
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    inner class MediaItemHolder(val binding: ListGridMediaItemHolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    getItem(layoutPosition).id
            }
    }

    companion object {
        const val ITEM_VIEW_TYPE_HEADER: Int = 8123
        const val ITEM_VIEW_TYPE_SEARCH: Int = 149003
    }
}

class HeaderViewHolder(val binding: ListGridHeaderBinding) :
    RecyclerView.ViewHolder(binding.root)

class SearchViewHolder(val binding: LayoutSearchBinding) :
    RecyclerView.ViewHolder(binding.root)