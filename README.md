# HabitQuest

Proyek awal Android native untuk tracker habit harian dengan gamification ringan.

## Membuka proyek

1. Buka Android Studio lalu pilih **Open** dan folder `HabitQuest` ini.
2. Gunakan **Gradle JDK: jbr (Embedded JDK)** di Settings > Build, Execution, Deployment > Build Tools > Gradle.
3. Tunggu Gradle Sync selesai. Unduhan dependency pertama membutuhkan internet.
4. Pilih emulator Pixel 8 atau ponsel dengan USB debugging, lalu klik Run.

## Stack

- Kotlin bawaan Android Gradle Plugin 9.3.3.
- Jetpack Compose + Material 3, Compose compiler plugin 2.2.10.
- Gradle Wrapper 9.5.0, Java target 17, Android SDK 36.
- Minimum Android 8.0 (API 26).
- ViewModel + StateFlow; dependency disediakan melalui Google Maven dan Maven Central.

Kotlin dikelola oleh Gradle; compiler Kotlin terpisah tidak diperlukan.
AGP 9 menggunakan Kotlin bawaan, sehingga plugin `org.jetbrains.kotlin.android` tidak diterapkan lagi.

## Build lewat PowerShell

```powershell
.\build.ps1
.\build.ps1 -Tasks 'testDebugUnitTest','lintDebug'
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.
`build.ps1` menggunakan JDK Android Studio untuk proses shell tersebut tanpa mengubah environment Windows permanen.

## Yang sudah tersedia

- Navigasi bawah Hari Ini, Progres, dan Profil; pilihan tab bertahan saat rotasi.
- Halaman Hari Ini mengikuti desain yang disetujui: warna hijau, kalender minggu berjalan, kartu XP, dan kartu habit.
- Tanggal mengikuti perangkat; kalender menyorot hari ini tanpa mengarang riwayat hari lainnya.
- Dialog pencatatan progres dengan jumlah yang bisa diisi dan pilihan cepat +1, +5, +10.
- XP sekali saat target tercapai, notifikasi singkat hasil pencatatan, dan indikator habit selesai.
- Form habit baru: nama, kategori, target, satuan, dan contoh jam pengingat; jadwal setiap hari.
- Tiga contoh habit dan 460 XP awal untuk memperlihatkan kondisi desain.
- Halaman Progres: pemilih habit, kalender bulanan Senin–Minggu, rincian saat tanggal diketuk, dan navigasi bulan.
- Ringkasan pencapaian dan total satuan per bulan, milestone 30 hari selesai sejak mulai, streak terbaik, dan ringkasan jadwal pengingat.
- Progres hari ini terhubung langsung ke kalender; tanggal sebelum pembuatan habit dan tanggal mendatang tidak masuk pembagi persentase.
- Pilihan bulan, tanggal, habit, serta posisi scroll tetap tersedia saat berpindah tab.
- Halaman Profil sesuai desain: avatar/nama demo, level, total XP, sisa XP menuju level berikutnya, tiga badge, dan ringkasan aktivitas.
- Badge bisa diketuk untuk melihat syarat dan progres. Langkah awal memerlukan satu target selesai; badge 7 hari memerlukan streak satu habit; badge 30 hari memerlukan 30 tanggal selesai pada satu habit.
- Hari aktif menghitung tanggal unik dengan progres positif pada bulan berjalan; streak terbaik dihitung per habit, tanpa menjumlahkan streak beberapa habit.
- Pemisahan model domain, state UI, dan Activity; preview Compose.
- Unit test progres parsial serta pencegahan XP ganda.

## Batas versi awal

Ini adalah fondasi proyek yang dapat dibangun, belum aplikasi tracker lengkap.
Data contoh ada di memori ViewModel dan akan hilang ketika proses aplikasi berakhir.
Belum ada penyimpanan database, edit/hapus habit, atau notifikasi pengingat.
Riwayat bulanan tiga habit bawaan memakai data contoh sejak awal bulan perangkat; keterangan demo ditampilkan di halaman Progres. Habit baru tidak memiliki riwayat buatan dan mulai dihitung sejak tanggal pembuatannya.
Ketika tanggal perangkat berganti, progres sebelumnya diarsipkan di memori dan progres tanggal baru ditampilkan. Seluruh riwayat ini tetap hilang saat proses aplikasi berakhir.
Target dicatat bersama setiap entri riwayat agar hasil lama tidak bergantung pada target yang digunakan di hari lain.
Profil menggunakan nama/avatar demo Rezek; pengubahan identitas belum tersedia.
Jam pengingat saat membuat habit hanya disimpan sebagai data sesi dan ditampilkan pada kartu, belum menjadwalkan notifikasi.

Urutan pengembangan berikutnya:

1. Room: Habit, jadwal, DailyProgress dengan kunci habit + tanggal, serta ledger XP.
2. Edit/hapus habit, jenis target lainnya, dan pemilihan hari jadwal.
3. Pengingat lokal: izin notifikasi, penjadwalan AlarmManager, penjadwalan ulang setelah reboot/perubahan zona waktu, dan penanganan akses alarm.
4. Pengubahan identitas profil, dukungan streak untuk jadwal selain harian, dan pengaturan DataStore.

Jangan memperlakukan XP demo sebagai ledger produksi sebelum penyimpanan harian ditambahkan.

## Verifikasi halaman Hari Ini — 25 Oktober 2026

- `assembleDebug`: berhasil menghasilkan APK debug.
- `testDebugUnitTest`: 2 test lulus, tanpa kegagalan.
- `lintDebug`: lulus dengan 0 error dan 8 warning.
- APK berhasil dipasang dan dibuka pada emulator Pixel 8 (`emulator-5554`).
- Pergantian ketiga tab, pencatatan membaca 10 → 15 halaman, kenaikan XP 460 → 480, dan progres setelah berpindah tab berhasil diperiksa di emulator.
- Form habit baru berhasil membuat dan menampilkan kartu habit contoh “Lari”.
- Screenshot halaman Hari Ini: [today.png](docs/screenshots/today.png).

## Verifikasi halaman Progres — 25 Oktober 2026

- `assembleDebug` dan `lintDebug`: berhasil; lint tanpa error.
- `testDebugUnitTest`: 7 test lulus, termasuk pembagi sejak tanggal pembuatan, tahun kabisat, tanggal mendatang, progres sebagian, serta streak dan target historis.
- Screenshot tampilan awal: [progress.png](docs/screenshots/progress.png).
- Uji emulator lulus untuk rincian tanggal, pemilih habit, navigasi bulan, dan pemulihan bulan yang dipilih setelah berpindah tab.
- Setelah membaca diselesaikan di Hari Ini, kalender berubah menjadi 21/25 hari selesai, pencapaian 80% → 84%, dan total 310 → 315 halaman.
- Screenshot ringkasan dan milestone setelah pencatatan: [progress-details.png](docs/screenshots/progress-details.png).

## Verifikasi halaman Profil — 25 Oktober 2026

- `assembleDebug`, `testDebugUnitTest`, dan `lintDebug`: berhasil.
- 11 unit test lulus; lint tanpa error. Test profil mencakup tanggal aktif unik, progres sebagian, syarat streak satu habit, dan ambang badge 30 hari tanpa menghitung duplikasi.
- Screenshot tampilan awal: [profile.png](docs/screenshots/profile.png).
- Uji emulator lulus untuk detail ketiga badge, status terkunci, pembaruan XP 460 → 480 → 500 dari Hari Ini, dan kenaikan level 5 → 6. Build dibiarkan terbuka pada tab Profil.
