package com.example.applemusicui.data

import androidx.compose.ui.graphics.Color

/** Warna gradien dipakai sebagai pengganti cover art (data dummy, tanpa jaringan). */
data class Song(val title: String, val artist: String, val art: List<Color>)

data class Album(val title: String, val subtitle: String, val art: List<Color>)

data class Mood(val title: String, val subtitle: String, val base: Color, val art: List<Color>)

object DummyData {

    val songs = listOf(
        Song("It's You", "Sezairi", listOf(Color(0xFFEDEDED), Color(0xFFB5B5B5))),
        Song("Kiss the Sky (EP Version)", "Cash Cash", listOf(Color(0xFF1B2A49), Color(0xFF3C5A99))),
        Song("Shower", "Becky G", listOf(Color(0xFFD93A2B), Color(0xFFF08A5D))),
        Song("Thunderclouds (feat. Sia, Diplo & Labrinth)", "LSD", listOf(Color(0xFFF2C94C), Color(0xFF56CCF2))),
        Song("Siapa Di Hatimu", "Maulana Ardiansyah", listOf(Color(0xFF3A2A22), Color(0xFFB07A4F))),
        Song("Suka Sama Kamu", "Maulana Ardiansyah", listOf(Color(0xFFE8E4DC), Color(0xFF8A7F73))),
        Song("Neon Nights", "Nova", listOf(Color(0xFF3B1E8C), Color(0xFFE040FB))),
        Song("Midnight Drive", "Aurora Lane", listOf(Color(0xFF0F2027), Color(0xFF2C5364))),
    )

    /** Satu halaman = 4 lagu (kolom horizontal di bagian Recents). */
    val recentPages: List<List<Song>> = songs.chunked(4)

    val albums = listOf(
        Album("Siapa Di Hatimu", "Single • Maulana Ardiansyah", listOf(Color(0xFF2B2B2B), Color(0xFFB07A4F))),
        Album("Suka Sama Kamu", "Single • Maulana Ardiansyah", listOf(Color(0xFFE8E4DC), Color(0xFF5E5E5E))),
        Album("Disco Breaks", "Album • Various Artists", listOf(Color(0xFF1A0B4D), Color(0xFFFF2D95))),
        Album("Neon Nights", "Album • Nova", listOf(Color(0xFF3B1E8C), Color(0xFFE040FB))),
        Album("Midnight Drive", "EP • Aurora Lane", listOf(Color(0xFF0F2027), Color(0xFF2C5364))),
    )

    private fun mood(title: String, subtitle: String, base: Long) = Mood(
        title = title,
        subtitle = subtitle,
        base = Color(base),
        art = listOf(Color(base).copy(alpha = 0.55f), Color.Black.copy(alpha = 0.55f)),
    )

    val moods = listOf(
        mood("Bepergian", "Feel-Good Hip Hop", 0xFFD9A400),
        mood("Fokus", "Lofi Loft", 0xFF9E9E9E),
        mood("Gaming", "Gaming Hits", 0xFF5A5A5A),
        mood("Merasa senang", "Sunshine Indie", 0xFF6FCF7F),
        mood("Olah Raga", "Beast Mode", 0xFFE07B2D),
        mood("Pesta", "Musik Pesta Indonesia", 0xFF9B6BE0),
        mood("Romantis", "Sweetheart Pop", 0xFFB00000),
        mood("Santai", "Hits Cafe", 0xFF7FA3E0),
        mood("Sedih", "Burn the Photos", 0xFF808080),
        mood("Semangat!", "Pump-Up Pop", 0xFFD8C45A),
        mood("Tidur", "Classical for Sleeping", 0xFF7B3FD0),
        mood("Afrika", "Afro Hits", 0xFF0F7A1F),
    )
}
