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
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.example.android.musicplayer.model.Song
import java.io.IOException
import java.util.*
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.content.Context


class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private lateinit var player: MediaPlayer

    private var songList: MutableList<Song> = mutableListOf()
    private var currentSongPosition = 3

    private val binder = MusicBinder()

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("service", "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
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
        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
        builder.setContentTitle(currentlyPlayedSong.title)
        Log.d("notifcation", currentlyPlayedSong.title)

        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
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