package com.github.livingwithhippos.unchained.utilities.extension

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.github.livingwithhippos.unchained.utilities.BlurTransformation
import com.github.livingwithhippos.unchained.utilities.GlideApp

@BindingAdapter("imageURL")
fun ImageView.loadImage(imageURL: String?) {
    if (imageURL != null)
        GlideApp.with(this.context)
            .load(imageURL)
            .into(this)
}

@BindingAdapter("blurredBackground")
fun ConstraintLayout.blurredBackground(drawable: Drawable) {
    val layout = this
    GlideApp.with(this)
        .load(drawable)
        .apply(RequestOptions.bitmapTransform(BlurTransformation(context)))
        .into(object : CustomViewTarget<ConstraintLayout, Drawable>(layout) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                // error handling
            }

            override fun onResourceCleared(placeholder: Drawable?) {
                // clear all resources
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                layout.background = resource
            }
        })
}