package com.example.teamozy.core.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AppEvent {
    data object Unauthorized : AppEvent()
}

object AppStateManager {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun emitUnauthorized() {
        _events.tryEmit(AppEvent.Unauthorized)
    }
}
