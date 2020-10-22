package com.github.livingwithhippos.unchained.utilities.extension

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import coil.load

/**
 * Load an image from a url into an [ImageView]
 * @param imageURL: the image url
 */
@BindingAdapter("imageURL")
fun ImageView.loadImage(imageURL: String?) {
    this.load(imageURL){
        crossfade(true)
    }
}