package com.example.android.musicplayer

import android.content.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var service: MusicService? = null
    private lateinit var connection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createConnection()

        bindMusicService()

        setOnClickListeners()
    }

    private fun createConnection() {
        connection = object : ServiceConnection {
            override fun onServiceDisconnected(p0: ComponentName?) = Unit

            override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                service = (binder as MusicService.MusicBinder).getService()
                Log.d("activity", "binded service")
            }
        }
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        stopBtn.isEnabled = true
    }

    private fun setOnClickListeners() {
        startBtn.setOnClickListener {
            service?.playMusic()
            stopBtn.isEnabled = true
        }

        stopBtn.setOnClickListener {
            service?.stopMusic()
            stopBtn.isEnabled = false
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }

}
