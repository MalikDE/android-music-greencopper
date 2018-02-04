package com.mde.myapplication.utils

import android.support.annotation.IdRes
import android.view.View

fun Any.TAG(): String = javaClass.simpleName

fun <T : View> View.bind(@IdRes res: Int): Lazy<T> {
    return lazy { findViewById<T>(res) }
}