package com.oliveira.joguinhosdamamae

import android.content.Context

object Progress {
    private fun prefs(c: Context) = c.getSharedPreferences("joguinhos", Context.MODE_PRIVATE)

    fun level(c: Context, game: String): Int = prefs(c).getInt("nivel_" + game, 1)

    fun setLevel(c: Context, game: String, level: Int) {
        prefs(c).edit().putInt("nivel_" + game, level).apply()
    }
}
