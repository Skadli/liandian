package com.liandian.app.engine

import com.liandian.app.model.GestureEvent
import com.liandian.app.model.TaskMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EngineState {
    IDLE, RECORDING, PLAYING
}

class GestureEngine {

    private val _state = MutableStateFlow(EngineState.IDLE)
    val state: StateFlow<EngineState> = _state

    private val recorder = GestureRecorder()
    private val player = GesturePlayer()
    private var playJob: Job? = null
    private var recordedEvents: List<GestureEvent> = emptyList()

    var onGestureDispatched: ((Float, Float) -> Unit)?
        get() = player.onGestureDispatched
        set(value) { player.onGestureDispatched = value }

    fun startRecording() {
        if (_state.value != EngineState.IDLE) return
        recorder.start()
        _state.value = EngineState.RECORDING
    }

    fun stopRecording() {
        if (_state.value != EngineState.RECORDING) return
        recordedEvents = recorder.stop()
        _state.value = EngineState.IDLE
    }

    fun getRecorder(): GestureRecorder = recorder

    fun getRecordedEvents(): List<GestureEvent> = recordedEvents

    fun startPlaying(mode: TaskMode, scope: CoroutineScope) {
        if (_state.value != EngineState.IDLE) return
        _state.value = EngineState.PLAYING
        playJob = scope.launch(Dispatchers.Default) {
            try {
                player.play(mode)
            } finally {
                _state.value = EngineState.IDLE
            }
        }
    }

    fun stopPlaying() {
        playJob?.cancel()
        playJob = null
    }
}
