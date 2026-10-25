# HabitQuest

Tracker habit Android native dengan Kotlin, Jetpack Compose, dan gamification ringan. Aplikasi berjalan offline dan menyimpan data secara lokal melalui Room.

## Alur pengguna

1. Saat pertama kali dibuka, halaman sambutan meminta nama panggilan (maksimal 30 karakter).
2. Setelah nama tersimpan, pengguna masuk ke Hari Ini dengan 0 habit, 0 XP, level 1, dan tombol **Buat habit pertama**.
3. Nama dan inisial yang sama dipakai pada sapaan, avatar, dan Profil.
4. Saat aplikasi dibuka ulang, halaman sambutan dilewati jika nama sudah tersimpan.

Tidak ada habit, XP, atau riwayat contoh yang otomatis dibuat. Upgrade dari prototipe demo akan masuk ke alur sambutan karena data demo sebelumnya hanya ada di memori.

## Fitur

- **Hari Ini:** kalender minggu berjalan, ringkasan XP, tampilan kosong, form habit baru, dan pencatatan progres.
- **Progres:** pemilih habit, kalender bulanan, rincian tanggal, persentase pencapaian, total aktivitas, milestone 30 hari, dan streak terbaik.
- **Profil:** nama/inisial, level dan XP, tiga badge beserta syaratnya, hari aktif, streak terbaik, dan jumlah habit.
- XP diberikan satu kali saat target habit pada tanggal tersebut selesai.
- Pengingat lokal satu kali per habit per hari, dengan informasi sisa target dan pintasan ke pencatatan progres.
- Jam pengingat dapat diubah atau dimatikan dari kartu Hari Ini dan halaman Progres.
- Pilihan halaman dan posisi scroll dipertahankan saat berpindah tab.

## Penyimpanan lokal

Database `habitquest.db` menggunakan Room 2.8.5 dan memiliki tabel:

| Tabel | Isi |
| --- | --- |
| `user_profile` | Nama panggilan; keberadaan profil menandakan onboarding sudah selesai |
| `habits` | Nama, kategori, target, satuan, jam pengingat, dan tanggal mulai |
| `daily_progress` | Habit, tanggal, target pada hari tersebut, progres, dan XP yang diberikan |
| `reminder_deliveries` | Habit dan tanggal pengingat yang sudah dikirim, untuk mencegah pengiriman ganda |

Kunci unik progres adalah **habit + tanggal**. Perubahan progres dan pemberian XP dilakukan dalam satu transaksi. Total XP dihitung dari catatan tersimpan sehingga tidak kembali nol ketika aplikasi ditutup.

Hari baru membaca catatan tanggal baru (atau progres nol jika belum ada), tanpa menghapus riwayat sebelumnya. Kalender dan statistik dihitung dari catatan yang sama. Tanggal sebelum pembuatan habit dan tanggal mendatang tidak masuk pembagi persentase.

Skema versi 1 dan 2 diekspor di `app/schemas/` dan perlu ikut disimpan di Git. Migrasi 1 → 2 menambahkan catatan pengingat tanpa menghapus nama, habit, progres, atau XP. Proyek ini tidak menggunakan destructive migration.

Data tetap ada setelah aplikasi ditutup atau build baru dipasang sebagai pembaruan. Menghapus data aplikasi atau uninstall akan menghapus database; backup dan sinkronisasi belum tersedia. Tidak ada akun, server, atau izin internet di aplikasi. Izin notifikasi diminta saat pengguna menyimpan pengingat pertama; izin boot dipakai untuk memasang jadwal kembali setelah restart.

## Aturan pengingat

- Jadwal mengikuti jam lokal perangkat, bukan 24 jam setelah pencatatan terakhir.
- Menggunakan alarm biasa (`AlarmManager.setAndAllowWhileIdle`), sehingga tidak memerlukan akses alarm tepat waktu. Penghemat baterai dapat menggeser waktu pengiriman.
- Ketika alarm diterima, aplikasi memeriksa habit, jam terbaru, tanggal, izin, dan progres dari database.
- Target selesai sebelum jadwal: pengingat hari itu dilewati. Target selesai setelah notifikasi muncul: notifikasi dihapus.
- Progres sebagian: notifikasi menampilkan sisa target. Mengetuknya membuka dialog pencatatan habit yang sesuai.
- Menggeser notifikasi tidak mengirim ulang pengingat pada tanggal yang sama. Mengubah jam setelah pengingat terkirim juga tidak menambah pengingat kedua hari itu.
- Mengubah atau mematikan jam membatalkan jadwal lama. Pengingat yang sudah kedaluwarsa tidak diputar ulang.
- Jadwal diperiksa ulang setelah boot, pembaruan APK, perubahan jam/zona waktu, dan saat aplikasi kembali aktif.
- Izin ditolak atau channel diblokir: habit dan jam tetap tersimpan; Hari Ini menampilkan akses untuk mengaktifkan izin. Pencatatan tetap dapat digunakan.
- Menutup aplikasi biasa tetap memungkinkan alarm berjalan. Setelah **Paksa berhenti**, buka aplikasi kembali agar jadwal dipasang ulang.
- Notifikasi tidak memberi XP; XP hanya diberikan ketika target selesai.

## Membuka dan menjalankan

1. Android Studio → **Open** → folder `HabitQuest`.
2. Gunakan Gradle JDK **jbr / Embedded JDK**.
3. Tunggu Gradle Sync; pengunduhan dependency build pertama membutuhkan internet.
4. Pilih emulator atau ponsel dengan USB debugging, lalu klik Run.

Stack: AGP 9.3.3, Gradle 9.5.0, Kotlin bawaan AGP, Compose compiler 2.2.10, KSP 2.3.12, Room 2.8.5. SDK compile/target 36; minimum Android 8.0 (API 26).

```powershell
.\build.ps1
.\build.ps1 -Tasks 'testDebugUnitTest','lintDebug'
.\build.ps1 -Tasks 'connectedDebugAndroidTest','-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. `build.ps1` memakai Java bawaan Android Studio tanpa mengubah environment Windows permanen.

## Verifikasi

- Build APK, lint, dan 17 unit test berhasil.
- 9 pengujian Android berhasil di emulator: penyimpanan lokal, migrasi 1 → 2, pengingat sebagian, target selesai, pengiriman ganda, jadwal berubah/dimatikan, izin ditolak, dan notifikasi Android yang membuka dialog habit.
- Pengujian notifikasi Android menggunakan habit sementara yang dibersihkan setelah pengujian; tidak membuat data contoh permanen.
- Screenshot halaman sambutan: [welcome.png](docs/screenshots/welcome.png).
- Screenshot tampilan awal kosong: [today-empty.png](docs/screenshots/today-empty.png).
- Uji alur emulator lulus: mengisi nama, masuk ke Hari Ini kosong, memeriksa Progres/Profil bernilai nol, membuka form habit pertama, dan membuka ulang proses aplikasi tanpa mengulang onboarding. Emulator dibiarkan di Hari Ini dengan nama Rezek dan tanpa habit.
- Screenshot lama `today.png`, `progress.png`, dan `profile.png` mendokumentasikan prototipe sebelum data contoh dihapus.

## Belum tersedia

- Edit/hapus/arsip habit, perubahan nama profil, jadwal selain setiap hari.
- Backup, ekspor, sinkronisasi cloud, serta pemulihan data setelah uninstall.
