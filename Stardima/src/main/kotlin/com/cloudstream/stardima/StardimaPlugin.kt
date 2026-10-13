package com.cloudstream.stardima

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class StardimaPlugin : Plugin() {
    override fun load(context: Context) {
        // Register the Stardima Arabic cartoon provider
        registerMainAPI(StardimaProvider())
    }
}
