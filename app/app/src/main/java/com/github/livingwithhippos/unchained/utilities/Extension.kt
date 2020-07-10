package com.github.livingwithhippos.unchained.utilities

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("profileImage")
fun ImageView.loadImage(profileImage: String?) {
    if (profileImage != null)
        Glide.with(this.context)
            .load(profileImage)
            .into(this)
}