package com.example.android.musicplayer.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Song(
        val id: Long,
        val title: String,
        val artist: String,
        val albumName: String,
        val albumCoverUri: Uri,
        val durationMillis: Int
) : Parcelable