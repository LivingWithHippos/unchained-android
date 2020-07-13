package com.github.livingwithhippos.unchained.utilities

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.github.livingwithhippos.unchained.R

@BindingAdapter("profileImage")
fun ImageView.loadImage(profileImage: String?) {
    if (profileImage != null)
        Glide.with(this.context)
            .load(profileImage)
            .into(this)
}

@BindingAdapter("adapter")
fun AutoCompleteTextView.setAdapter(contents: List<String>) {
    // a simple layout is set for the dropdown items
    val adapter = ArrayAdapter<String>(this.context, R.layout.dropdown_plain_item, contents)
    this.setAdapter(adapter)
}

fun Fragment.showToast(stringResource: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(),getString(stringResource),length).show()
}