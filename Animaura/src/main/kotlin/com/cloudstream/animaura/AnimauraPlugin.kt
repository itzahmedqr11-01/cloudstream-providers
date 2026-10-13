package com.cloudstream.animaura

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnimauraPlugin : Plugin() {
    override fun load(context: Context) {
        // Register the Animaura provider into CloudStream
        registerMainAPI(AnimauraProvider())
    }
}
