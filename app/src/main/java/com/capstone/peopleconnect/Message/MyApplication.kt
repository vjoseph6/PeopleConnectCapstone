package com.capstone.peopleconnect.Message

import android.app.Application
import com.capstone.peopleconnect.R
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable background sync and user presence
        val backgroundSyncEnabled = true
        val userPresence = true

        // Initialize the State plugin configuration
        val statePluginFactory = StreamStatePluginFactory(
            StatePluginConfig(
                backgroundSyncEnabled = backgroundSyncEnabled,
                userPresence = userPresence
            ),
            this
        )

        // Initialize the Offline plugin without additional config
        val offlinePluginFactory = StreamOfflinePluginFactory(
            appContext = this
        )

        // Initialize ChatClient with the plugins
        val client = ChatClient.Builder(getString(R.string.api_key), this)
            .logLevel(ChatLogLevel.ALL)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .withPlugins(statePluginFactory, offlinePluginFactory)
            .build()
    }

}
