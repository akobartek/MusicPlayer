package com.example.android.musicplayer

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.BaseColumns
import android.provider.MediaStore
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.example.android.musicplayer.model.Song
import java.io.IOException
import java.util.*
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter

const val START_MUSIC_ACTION = "com.example.android.musicplayer.startmusic"
const val STOP_MUSIC_ACTION = "com.example.android.musicplayer.stopmusic"
const val NOTIFICATION_ID = 0

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private lateinit var receiver: HeadphonesStateBroadcastReceiver
    private val filter = IntentFilter().apply {
        addAction(AudioManager.ACTION_HEADSET_PLUG)
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    }


    private lateinit var player: MediaPlayer

    private var songList: MutableList<Song> = mutableListOf()
    private var currentSongPosition = 3

    private val binder = MusicBinder()

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("service", "onStartCommand")
        if (intent?.action == START_MUSIC_ACTION) playMusic()
        if (intent?.action == STOP_MUSIC_ACTION) stopMusic()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("service", "onCreate")

        player = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setOnPreparedListener(this@MusicService)
            setOnErrorListener(this@MusicService)
            setOnCompletionListener(this@MusicService)
        }

        retrieveDeviceSongList()

        receiver = HeadphonesStateBroadcastReceiver(player)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        unregisterReceiver(receiver)
        stopForeground(true)
        super.onDestroy()
    }

    private fun retrieveDeviceSongList() {
        songList = mutableListOf()

        val musicResolver = contentResolver
        val musicCursor = musicResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            val titleColumn = musicCursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
            val idColumn = musicCursor.getColumnIndex(BaseColumns._ID)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
            val albumCoverColumn = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)
            val albumNameColumn = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
            val durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
            do {
                val thisId = musicCursor.getLong(idColumn)
                val songTitle = musicCursor.getString(titleColumn)
                Log.d("service", songTitle)
                val songArtist = musicCursor.getString(artistColumn)
                val songAlbum = musicCursor.getString(albumNameColumn)
                val albumCoverId = musicCursor.getLong(albumCoverColumn)
                val albumCoverUriPath = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(albumCoverUriPath, albumCoverId)
                val songDuration = musicCursor.getLong(durationColumn)
                songList.add(Song(thisId, songTitle, songArtist, songAlbum, albumArtUri, songDuration.toInt()))
            } while (musicCursor.moveToNext())
        }
        musicCursor?.close()
        Collections.sort<Song>(songList) { lhs, rhs -> lhs.title.compareTo(rhs.title) }
    }

    fun playMusic() {
        player.reset()
        val currentlyPlayedSong = songList[currentSongPosition]
        val currentSongId = currentlyPlayedSong.id
        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSongId)

        try {
            player.setDataSource(applicationContext, trackUri)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        createNotification(currentlyPlayedSong)

        player.prepareAsync()
    }

    private fun createNotification(currentlyPlayedSong: Song) {

        val intent = Intent(this, MusicService::class.java).apply {
            action = START_MUSIC_ACTION
        }
        val actionOnePendingIntent = PendingIntent.getService(this, 100, intent, 0)

        val intent2 = Intent(this, MusicService::class.java).apply {
            action = STOP_MUSIC_ACTION
        }
        val actionTwoPendingIntent = PendingIntent.getService(this, 101, intent2, 0)

        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(currentlyPlayedSong.title)
                .setAutoCancel(false)
                .addAction(R.mipmap.ic_launcher, getString(R.string.start), actionOnePendingIntent)
                .addAction(R.mipmap.ic_launcher, getString(R.string.stop), actionTwoPendingIntent)

        Log.d("notifcation", currentlyPlayedSong.title)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun stopMusic() {
        player.pause()
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) = Unit

    override fun onError(mediaPlayer: MediaPlayer, what: Int, extra: Int) = false

    override fun onPrepared(mediaPlayer: MediaPlayer) = player.start()


    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}