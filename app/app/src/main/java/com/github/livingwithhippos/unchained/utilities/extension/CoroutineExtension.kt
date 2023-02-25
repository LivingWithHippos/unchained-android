package com.github.livingwithhippos.unchained.utilities.extension

import kotlinx.coroutines.Job

fun Job.cancelIfActive() {
    if (isActive) cancel()
}
