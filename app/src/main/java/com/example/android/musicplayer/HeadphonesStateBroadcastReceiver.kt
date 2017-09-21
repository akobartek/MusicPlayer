package com.example.android.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

class HeadphonesStateBroadcastReceiver(private val player: MediaPlayer) : BroadcastReceiver() {

    override fun onReceive(p0: Context?, intent: Intent) {
        val state = intent.getIntExtra(Intent.ACTION_HEADSET_PLUG, -1)

        when (state) {
            0 -> player.stop()
        }
    }


}