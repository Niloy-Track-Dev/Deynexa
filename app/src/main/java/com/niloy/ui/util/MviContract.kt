package com.niloy.ui.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Base contract interfaces for MVI architecture.
 */
interface UiState
interface UiIntent
interface UiSideEffect

/**
 * Helper delegate or base class for side effects.
 */
class SideEffectEffectHandler<SE : UiSideEffect> {
    private val _effectChannel = Channel<SE>(Channel.BUFFERED)
    val effectFlow: Flow<SE> = _effectChannel.receiveAsFlow()

    suspend fun sendEffect(effect: SE) {
        _effectChannel.send(effect)
    }

    fun trySendEffect(effect: SE) {
        _effectChannel.trySend(effect)
    }
}
