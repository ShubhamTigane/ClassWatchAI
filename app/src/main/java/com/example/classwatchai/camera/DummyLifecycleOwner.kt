package com.example.classwatchai.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class DummyLifecycleOwner : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this).apply {
        currentState = Lifecycle.State.STARTED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
