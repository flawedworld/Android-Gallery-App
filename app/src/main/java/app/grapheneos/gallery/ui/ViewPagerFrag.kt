package app.grapheneos.gallery.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import app.grapheneos.gallery.ListItem
import app.grapheneos.gallery.R
import app.grapheneos.gallery.adapter.ViewHolderPager
import app.grapheneos.gallery.adapter.ViewPagerAdapter
import app.grapheneos.gallery.databinding.EditTextBinding
import app.grapheneos.gallery.databinding.FragmentViewPagerBinding
import app.grapheneos.gallery.databinding.ViewDialogInfoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFade
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.set


class ViewPagerFrag : Fragment() {
    private lateinit var _binding: FragmentViewPagerBinding
    val binding: FragmentViewPagerBinding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()
    private var isSystemUiVisible = true
    private var shortAnimationDuration = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        when {
            arguments?.getBoolean("isAlbum") == true -> {
                viewModel.albums.observe(viewLifecycleOwner) { albums ->
                    val items = albums.find { it.name == MainActivity.currentAlbumName }?.mediaItems
                    (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
                }
            }
            else -> {
                viewModel.viewPagerItems.observe(viewLifecycleOwner) { items ->
                    (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
                }
            }
        }

        prepareSharedElementTransition()

        if (::_binding.isInitialized) return binding.root

        _binding = FragmentViewPagerBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
            .toLong()
        binding.tbViewPager.setNavigationOnClickListener {
            if (activity is MainActivity || activity is SearchableActivity) {
                findNavController().navigateUp()
            } else {
                requireActivity().finish()
            }
        }

        setUpViewpager()
        setUpViews()

        return binding.root
    }

    private fun setUpViewpager() {
        val adapter = ViewPagerAdapter(this)

        binding.viewPager.apply {
            this.adapter = adapter

            if (arguments?.getBoolean("isAlbum") == true) {
                adapter.submitList(viewModel.albums.value?.find {
                    it.name == MainActivity.currentAlbumName
                }?.mediaItems)
            } else {
                adapter.submitList(viewModel.viewPagerItems.value)
            }

            setCurrentItem(MainActivity.currentViewPagerPosition, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    MainActivity.currentViewPagerPosition = position

                    if (arguments?.getBoolean("isAlbum") == true) {
                        MainActivity.currentListPosition = position
                    } else {
                        MainActivity.currentListPosition =
                            viewModel.viewPagerItems.value?.get(position)!!.listPosition
                    }
                }
            })

            setPageTransformer(MarginPageTransformer(50))
        }
    }

    fun showSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 250L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })

        if (!isActionView()) {
            binding.cvInfo.isVisible = true
            binding.cvDelete.isVisible = true
            binding.cvEdit.isVisible = true
        }

        binding.tbViewPager.isVisible = true
        binding.cvShare.isVisible = true
        binding.ivGradTop.isVisible = true
        binding.ivGardBottom.isVisible = true

        ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
            ?.show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 180L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })

        binding.tbViewPager.isVisible = false
        binding.cvShare.isVisible = false
        binding.cvEdit.isVisible = false
        binding.cvInfo.isVisible = false
        binding.cvDelete.isVisible = false
        binding.ivGradTop.isVisible = false
        binding.ivGardBottom.isVisible = false

        ViewCompat.getWindowInsetsController(requireActivity().window.decorView)
            ?.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun toggleSystemUI() {
        if (isSystemUiVisible) hideSystemUI() else showSystemUI()
        isSystemUiVisible = !isSystemUiVisible
    }

    private fun setUpSystemBars() =
        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }


    private fun setUpViews() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.tbViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.top
                }
                binding.cvShare.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvEdit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvInfo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                binding.cvDelete.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom
                }
                return@setOnApplyWindowInsetsListener windowInsets
            }
        }

        binding.cvShare.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            share(currentItem, requireActivity())
        }

        binding.cvEdit.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener

            Intent(Intent.ACTION_EDIT).apply {
                data = currentItem.uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.also {
                startActivity(
                    Intent.createChooser(
                        it,
                        "Edit with"
                    )
                )
            }
        }

        if (isActionView()) {
            binding.cvInfo.isVisible = false
            binding.cvDelete.isVisible = false
            binding.cvEdit.isVisible = false
            return
        }

        binding.cvDelete.setOnClickListener {
            getCurrentItem()?.let { viewModel.deleteItem(it) }
        }

        binding.cvInfo.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener

            val info = viewModel.performGetItemInfo(currentItem)

            val infoBinding = ViewDialogInfoBinding.inflate(
                layoutInflater
            )

            infoBinding.tvDateAdded.text = SimpleDateFormat.getDateInstance().format(
                Date(
                    info[0].toLong()
                )
            )

            infoBinding.tvName.text = info[3]

            infoBinding.tvTimeAdded.text =
                SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
                    .format(Date(info[0].toLong()))

            infoBinding.tvPath.text = info[2]

            infoBinding.tvSize.text =
                String.format(resources.getString(R.string.item_size), info[1])

            info[4].let { sInfo: String? ->
                if (sInfo?.isNotEmpty() == true) {
                    infoBinding.tvDescription.text = info[4]
                } else {
                    infoBinding.tvDescription.text = resources.getString(R.string.no_description)
                }
            }

            if (info.size < 6 || info[5] == "0.0" && info[6] == "0.0" ||
                currentItem.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            ) {
                infoBinding.tvLocation.isVisible = false
                infoBinding.tvLocationHolder.isVisible = false
                infoBinding.ivLocation.isVisible = false
                infoBinding.btnLeaveApp.isVisible = false
            } else {
                infoBinding.tvLocation.text = "${info[5]}, ${info[6]}"
                infoBinding.btnLeaveApp.setOnClickListener {
                    launchMapIntent(info)
                }
            }

            infoBinding.btnEditDescription.setOnClickListener {
                val editTextBinding = EditTextBinding.inflate(layoutInflater)

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Edit description")
                    .setView(editTextBinding.root)
                    .setPositiveButton("Edit") { _, _ ->
                        viewModel.editImageDescription(
                            currentItem,
                            editTextBinding.tietDescription.text.toString()
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            )
                .setTitle(getString(R.string.info))
                .setView(infoBinding.root)
                .setIcon(R.drawable.ic_baseline_info_24)
                .setPositiveButton(getString(R.string.close), null)
                .show()

        }
    }

    private fun isActionView() =
        requireActivity().intent.action == Intent.ACTION_VIEW

    private fun launchMapIntent(latLong: List<String>) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("geo:${latLong[5]}, ${latLong[6]}")
        }.also { intent ->
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun getCurrentItem(): ListItem.MediaItem? {
        return try {
            (binding.viewPager.adapter as ViewPagerAdapter)
                .currentList[binding.viewPager.currentItem]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
        setUpSystemBars()
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor =
                resources.getColor(android.R.color.black, requireActivity().theme)
        }

        setEnterSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {
                    val selectedViewHolder =
                        (binding.viewPager.getChildAt(0) as RecyclerView?)
                            ?.findViewHolderForLayoutPosition(binding.viewPager.currentItem)
                                as ViewHolderPager? ?: return

                    sharedElements[names[0]] = selectedViewHolder.binding.pagerImage
                }
            })

        postponeEnterTransition()
    }

    companion object {
        fun share(item: ListItem.MediaItem, activity: Activity) {
            Intent(Intent.ACTION_SEND).apply {
                setDataAndType(
                    item.uri,
                    activity.contentResolver.getType(
                        item.uri
                    )
                )
                putExtra(Intent.EXTRA_STREAM, item.uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.also {
                activity.startActivity(
                    Intent.createChooser(
                        it,
                        "Share with"
                    )
                )
            }
        }

        fun share(items: List<ListItem.MediaItem>, activity: Activity) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                val uris = ArrayList<Uri>()
                for (item in items) {
                    uris.add(item.uri)
                }
                type = "*/*"
                println("type: $type")
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.also {
                activity.startActivity(
                    Intent.createChooser(
                        it,
                        "Share with"
                    )
                )
            }
        }
    }
}