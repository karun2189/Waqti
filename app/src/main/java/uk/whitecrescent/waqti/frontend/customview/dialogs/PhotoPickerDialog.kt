package uk.whitecrescent.waqti.frontend.customview.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.github.basshelal.unsplashpicker.data.UnsplashPhoto
import com.github.basshelal.unsplashpicker.presentation.PhotoSize
import com.github.basshelal.unsplashpicker.presentation.UnsplashPhotoPicker
import kotlinx.android.synthetic.main.dialog_photopicker.*
import uk.whitecrescent.waqti.R
import uk.whitecrescent.waqti.extensions.invoke
import uk.whitecrescent.waqti.frontend.fragments.parents.WaqtiFragment

/**
 * This is actually not a [MaterialDialog], just a regular [Fragment]
 */
class PhotoPickerDialog : WaqtiFragment() {

    var onConfirm: (UnsplashPhoto) -> Unit = { }
    var onCancel: () -> Unit = { }
    var onClick: (UnsplashPhoto) -> Unit = { }
    var selectedPhoto: UnsplashPhoto? = null
        set(value) {
            field = value
            confirm_button {
                alpha = if (value == null) 0.3F else 1F
                isEnabled = value != null
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_photopicker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpAppBar()
        setUpViews()
    }

    override fun setUpViews() {
        selectedPhoto = null

        photoPicker {
            clickOpensPhoto = false
            longClickSelectsPhoto = false
            isMultipleSelection = false
            onClickPhoto = { photo, _ ->
                selectedPhoto = photo
                onClick(photo)
                selectPhoto(photo)
                showPhoto(photo, PhotoSize.REGULAR)
            }
            onLongClickPhoto = { photo, _ ->
                selectPhoto(photo)
            }
        }

        confirm_button {
            setOnClickListener {
                selectedPhoto?.also {
                    onConfirm(it)
                    UnsplashPhotoPicker.downloadPhoto(it)
                }
            }
        }
        cancel_button {
            setOnClickListener {
                onCancel()
            }
        }
    }

    override fun setUpAppBar() {
        mainActivity.appBar.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()

        mainActivity.appBar.isVisible = true
    }

    fun dismiss() {
        finish()
    }

    override fun finish() {
        mainActivity.supportFragmentManager.popBackStack()
    }

}