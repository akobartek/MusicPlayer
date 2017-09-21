package com.example.android.musicplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var service: MusicService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        startBtn.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            val connection: ServiceConnection = object : ServiceConnection {
                override fun onServiceDisconnected(p0: ComponentName?) = Unit

                override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                    service = (binder as MusicService.MusicBinder).getService()
                    Log.d("activity", "binded service")
                }
            }
            bindService(intent, connection, 0)
            stopBtn.isEnabled = true
        }

        stopBtn.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            service?.stopService(intent)
        }
    }


}
