package com.github.livingwithhippos.unchained.utilities

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * A [NestedScrollView] that never takes d-pad focus itself.
 *
 * NestedScrollView calls setFocusable(true) in its constructor, overriding any android:focusable
 * attribute set in the layout. On Android TV this makes the scroll container itself a focus
 * target: it sits directly next to the toolbar and the navigation bar, so FocusFinder routes the
 * d-pad onto it instead of the buttons inside, and since a focused scroll view has no visual
 * focus state the screen looks like it cannot be navigated at all (see issue #376).
 *
 * Focused children still scroll into view through requestChildFocus, so scrolling with the d-pad
 * keeps working as long as the content contains at least one focusable view.
 */
class NonFocusableNestedScrollView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.core.R.attr.nestedScrollViewStyle,
) : NestedScrollView(context, attrs, defStyleAttr) {

    init {
        isFocusable = false
    }
}
