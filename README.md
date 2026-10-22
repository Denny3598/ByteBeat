# AppleMusicUI (Jetpack Compose)

Kerangka UI bergaya Apple Music untuk Android: tema gelap, aksen merah `#FA2D48`,
judul besar tebal, mini player melayang, dan tab bar berbentuk pil.

Isi saat ini: **UI saja dengan data dummy** (cover art = gradien warna).
Belum ada pemutaran musik nyata dan belum terhubung ke YouTube Music.

## Cara menjalankan
1. Buka folder ini di **Android Studio** (Koala atau lebih baru), tunggu Gradle sync.
2. Jalankan di emulator atau HP (minSdk 26 / Android 8.0).

## Struktur
```
app/src/main/java/com/example/applemusicui/
├── MainActivity.kt          edge-to-edge + tema
├── data/DummyData.kt        lagu, album, mood dummy
└── ui/
    ├── AppRoot.kt           state tab + mini player + tab bar
    ├── theme/Theme.kt       warna & tema
    ├── components/          Artwork, SongRow, AlbumCard, MiniPlayer, PillTabBar
    └── screens/             ListenNow, Explore, Library, Search
```

## Langkah berikutnya (menyambung ke YouTube Music)
1. **Data**: ganti `DummyData` dengan repository yang memanggil InnerTube
   (mis. lewat library NewPipe Extractor) untuk home, search, dan playlist.
2. **Cover art**: pakai Coil (`AsyncImage`) menggantikan `Artwork` gradien.
3. **Pemutaran**: Media3 ExoPlayer + `MediaSessionService` agar jalan di background
   dan muncul di notifikasi.
4. **Login**: ambil cookie akun lewat WebView login, simpan terenkripsi.
5. **Font**: tambahkan font mirip SF (mis. Inter) ke `res/font` dan set di tema.

> Catatan: memakai API tidak resmi YouTube Music melanggar ToS YouTube, jadi
> sebaiknya dipakai untuk keperluan pribadi dan tidak dipublikasikan ke Play Store.
