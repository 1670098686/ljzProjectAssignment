package com.campustrade.android.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun <T> StateFlow<T>.observe(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    observer: (T) -> Unit
) {
    (this as Flow<T>).observe(owner, minActiveState, observer)
}

fun <T> Flow<T>.observe(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    observer: (T) -> Unit
) {
    owner.lifecycleScope.launch {
        owner.lifecycle.repeatOnLifecycle(minActiveState) {
            collect { observer(it) }
        }
    }
}
