package com.guopian.app.media.vlc

import android.content.Context
import org.videolan.libvlc.LibVLC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibVlcHolder @Inject constructor() {
    @Volatile
    private var lib: LibVLC? = null

    fun get(context: Context): LibVLC {
        lib?.let { return it }
        synchronized(this) {
            lib?.let { return it }
            val options = arrayListOf(
                "--aout=opensles",
                "--audio-time-stretch",
                "-vvv",
            )
            return LibVLC(context.applicationContext, options).also { lib = it }
        }
    }
}
