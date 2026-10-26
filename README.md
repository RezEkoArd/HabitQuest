# HabitQuest

HabitQuest adalah aplikasi Android untuk mencatat kebiasaan harian, melihat konsistensi, dan mengumpulkan XP. Aplikasi berjalan **offline**, tanpa akun atau server. Data disimpan di perangkat dan dapat dipindahkan menggunakan file backup.

Dokumen ini menjelaskan penggunaan fitur yang tersedia pada versi saat ini. Bagian pengembangan project tersedia setelah panduan penggunaan.

## Daftar isi

- [Mulai menggunakan aplikasi](#mulai-menggunakan-aplikasi)
- [Mengenal tiga halaman utama](#mengenal-tiga-halaman-utama)
- [Membuat habit dan memilih hari](#membuat-habit-dan-memilih-hari)
- [Mencatat progres hari ini](#mencatat-progres-hari-ini)
- [Mengoreksi progres](#mengoreksi-progres)
- [Menulis dan menghapus catatan](#menulis-dan-menghapus-catatan)
- [Mengedit habit](#mengedit-habit)
- [Mengatur urutan habit](#mengatur-urutan-habit)
- [Mengarsipkan dan memulihkan habit](#mengarsipkan-dan-memulihkan-habit)
- [Mengatur pengingat dan snooze](#mengatur-pengingat-dan-snooze)
- [Melihat kalender dan riwayat](#melihat-kalender-dan-riwayat)
- [Membaca ringkasan mingguan dan bulanan](#membaca-ringkasan-mingguan-dan-bulanan)
- [Memahami XP, level, streak, dan badge](#memahami-xp-level-streak-dan-badge)
- [Mengganti nama dan melihat profil](#mengganti-nama-dan-melihat-profil)
- [Membuat backup dan memulihkan data](#membuat-backup-dan-memulihkan-data)
- [Pertanyaan dan kendala umum](#pertanyaan-dan-kendala-umum)
- [Penyimpanan lokal](#penyimpanan-lokal)
- [Membuka dan menjalankan project](#membuka-dan-menjalankan)
- [Verifikasi](#verifikasi)
- [Batasan fitur saat ini](#belum-tersedia)

## Mulai menggunakan aplikasi

1. Pasang dan buka HabitQuest pada perangkat Android 8.0 atau lebih baru.
2. Di halaman **Kita kenalan dulu.**, masukkan **Nama panggilan**, maksimal 30 karakter.
3. Tekan **Mulai perjalanan**.
4. Aplikasi membuka **Hari Ini** dengan daftar habit kosong, 0 XP, dan level 1.
5. Tekan **Buat habit pertama** untuk memulai.

Nama tersimpan dan digunakan pada sapaan serta Profil. Saat aplikasi dibuka kembali, kamu tidak perlu mengisi nama lagi. Tidak ada habit atau progres contoh yang dibuat otomatis.

Jika sudah memiliki file backup dari perangkat sebelumnya, pilih **Pulihkan dari backup** di halaman sambutan. Ikuti langkah pada bagian [backup dan pemulihan](#membuat-backup-dan-memulihkan-data).

## Mengenal tiga halaman utama

Gunakan navigasi di bagian bawah aplikasi.

| Halaman | Kegunaan |
| --- | --- |
| **Hari Ini** | Melihat habit yang dijadwalkan hari ini, mencatat aktivitas, mengoreksi progres, dan menulis catatan |
| **Progres** | Melihat ringkasan semua habit, memilih habit, dan meninjau kalender serta riwayat per tanggal |
| **Profil** | Melihat XP, level, badge, statistik, mengganti nama, mengelola habit, dan membuat/memulihkan backup |

Deretan tanggal mingguan pada **Hari Ini** menunjukkan minggu berjalan dan menandai tanggal hari ini. Untuk meninjau tanggal lain, gunakan kalender pada **Progres**.

## Membuat habit dan memilih hari

1. Buka **Hari Ini**.
2. Tekan **Buat habit pertama** atau **Buat habit baru**.
3. Isi **Nama habit**, maksimal 60 karakter.
4. Pilih kategori: **Olahraga**, **Belajar**, **Kreativitas**, atau **Personal**.
5. Isi **Target harian** dengan angka bulat 1–10.000 dan **Satuan**, maksimal 20 karakter.
6. Di **Hari pelaksanaan**, pilih minimal satu hari. Tekan **Setiap hari** jika ingin memilih seluruh hari.
7. Jika ingin diingatkan, aktifkan **Jam pengingat**, lalu isi jam dengan format 24 jam, misalnya `07:00`.
8. Tekan **Buat habit**. Izinkan notifikasi jika diminta Android.

Contoh pengaturan:

| Nama habit | Kategori | Target dan satuan | Hari | Pengingat |
| --- | --- | --- | --- | --- |
| Workout pagi | Olahraga | 30 menit | Senin, Rabu, Jumat | 07:00 |
| Baca buku | Belajar | 15 halaman | Setiap hari | 20:00 |
| Buat konten | Kreativitas | 1 konten | Senin–Jumat | 19:00 |

Memilih kategori pada form pembuatan mengisi satuan awal secara otomatis; sesuaikan satuannya sebelum menyimpan. Kategori tidak mengubah jumlah XP.

Habit muncul di **Hari Ini** hanya pada hari pelaksanaannya. Jika hari ini tidak dipilih, habit tetap tersimpan dan dapat ditemukan melalui **Profil → Kelola habit dan arsip** atau pemilih habit di **Progres**.

## Mencatat progres hari ini

1. Buka **Hari Ini**, lalu cari habit yang dikerjakan.
2. Tekan tombol **+** pada kartu habit.
3. Masukkan **jumlah tambahan** aktivitas yang baru selesai. Tombol **+1**, **+5**, dan **+10** mengisi angka tambahan pada form.
4. Tekan **Simpan progres**.
5. Periksa angka progres dan batang pencapaian pada kartu.

Contoh: progres membaca sudah **5/15 halaman**. Kamu membaca 4 halaman lagi. Masukkan **4**, sehingga hasilnya menjadi **9/15 halaman**.

Pencatatan dapat dilakukan beberapa kali sehari. Jumlah akhir dibatasi sampai target: memasukkan 10 halaman ketika sudah 9/15 menghasilkan 15/15. Saat target tercapai, kartu ditandai selesai dan mendapat **20 XP**. Menyelesaikan target yang sama berulang kali pada tanggal yang sama tidak menggandakan XP.

Saat berganti tanggal, progres harian berikutnya dimulai dari nol. Catatan hari sebelumnya tetap tersedia pada kalender.

## Mengoreksi progres

Gunakan koreksi jika salah memasukkan jumlah atau ingin melengkapi catatan progres tanggal lampau.

**Untuk hari ini:** buka kartu habit di **Hari Ini**, lalu tekan **Koreksi progres**.

**Untuk tanggal lampau:** buka **Progres**, pilih habit, pilih tanggal pada kalender, lalu tekan **Koreksi progres tanggal ini**. Tanggal harus sudah tiba, tidak mendahului pembuatan habit, dan dijadwalkan atau masih memiliki progres tersimpan.

1. Isi **Jumlah akhir** yang benar, dari 0 sampai target tanggal tersebut.
2. Tekan **Simpan koreksi**.
3. Periksa kembali progres serta total XP.

Contoh: progres saat ini **12/15**, tetapi jumlah yang benar **8 halaman**. Masukkan **8**, bukan selisihnya. Ini berbeda dari **Catat progres**, yang menambahkan jumlah baru.

Koreksi menjadi selesai memberi 20 XP pada tanggal tersebut. Koreksi dari selesai menjadi belum selesai mengurangi kembali 20 XP; level dan badge juga dapat berubah mengikuti riwayat terbaru. Target tanggal lama tetap memakai target historisnya.

## Menulis dan menghapus catatan

**Catatan hari ini:** buka **Hari Ini → Catatan** atau **Edit catatan** pada kartu habit.

**Catatan tanggal lain:** buka **Progres**, pilih habit dan tanggal kalender, lalu tekan **Tambah catatan tanggal ini** atau **Edit catatan tanggal ini**.

1. Tulis detail pada **Catatan aktivitas**, misalnya judul buku, jenis latihan, atau kendala hari itu.
2. Tekan **Simpan catatan**.
3. Buka kembali tombol catatan jika ingin mengubahnya.

Catatan maksimal 2.000 karakter dan tersimpan terpisah untuk setiap habit dan tanggal. Kamu dapat menulis pada hari istirahat atau riwayat habit arsip, mulai tanggal pembuatan habit sampai hari ini. Catatan singkat muncul pada kartu Hari Ini; catatan tanggal terpilih juga dapat dibaca pada Progres.

Untuk menghapus, kosongkan seluruh teks lalu tekan **Simpan catatan**. Menulis atau menghapus catatan tidak mengubah progres, XP, atau streak. Catatan ikut disimpan dalam backup.

## Mengedit habit

Buka editor melalui salah satu akses berikut:

- **Hari Ini → Edit habit** pada kartu.
- **Progres → pilih habit → Edit habit**.
- **Profil → Kelola habit dan arsip → tekan nama habit**.

1. Ubah nama, kategori, target, atau hari pelaksanaan.
2. Pastikan minimal satu hari dipilih.
3. Tekan **Simpan perubahan**.

**Nama dan kategori langsung diperbarui. Target dan jadwal baru berlaku mulai besok.** Riwayat tetap memakai versi target dan jadwal yang berlaku pada tanggalnya.

Contoh: target membaca hari ini 15 halaman, lalu kamu menggantinya menjadi 20. Target hari ini tetap 15, sedangkan mulai besok menjadi 20 pada hari yang dijadwalkan.

Satuan tidak dapat diubah setelah habit dibuat, agar total riwayat tetap dapat dijumlahkan. Jam pengingat diubah melalui ikon lonceng atau pengaturan pengingat, bukan melalui editor ini.

## Mengatur urutan habit

1. Buka **Profil → Kelola habit dan arsip**.
2. Pilih tab **Aktif**.
3. Tekan **↑ Naik** atau **↓ Turun** di bawah habit yang ingin dipindahkan.
4. Ulangi sampai urutannya sesuai rutinitas, lalu tekan **Tutup**.

Perubahan langsung tersimpan dan dipakai pada daftar **Hari Ini** serta pemilih habit di **Progres**. Tombol naik dinonaktifkan pada habit aktif pertama, dan tombol turun pada habit aktif terakhir. Urutan tetap ada setelah aplikasi ditutup dan ikut dalam backup. Habit baru ditempatkan di akhir urutan.

## Mengarsipkan dan memulihkan habit

### Mengarsipkan

1. Buka **Profil → Kelola habit dan arsip → Aktif**.
2. Tekan **Arsipkan** pada habit.
3. Baca konfirmasinya, lalu tekan **Arsipkan** lagi.

Habit langsung berhenti muncul di Hari Ini. Pengingat dan snooze dihentikan. Progres, catatan, dan XP tetap tersimpan; riwayatnya masih bisa dipilih pada Progres. Editor habit juga menyediakan akses **Arsipkan habit**.

### Memulihkan habit

1. Buka **Profil → Kelola habit dan arsip → Arsip**.
2. Tekan **Pulihkan** pada habit.
3. Tekan **Pulihkan** pada konfirmasi.

Jadwal diaktifkan kembali **mulai besok**, sesuai hari pelaksanaannya. Hari selama masa arsip tidak menambah atau memutus streak. Tidak ada penghapusan permanen habit pada versi ini.

## Mengatur pengingat dan snooze

### Mengubah atau mematikan jam pengingat

1. Pada kartu **Hari Ini**, tekan ikon lonceng atau **Atur pengingat**. Akses pengaturan juga tersedia pada bagian pengingat di **Progres** untuk habit yang dipilih.
2. Aktifkan pengingat dan isi jam `HH:mm`, misalnya `07:00` atau `20:30`.
3. Tekan **Simpan pengingat**.
4. Untuk mematikan, nonaktifkan sakelar lalu tekan **Simpan pengingat**.

Pengingat mengikuti **jam lokal perangkat dan hari pelaksanaan**, bukan 24 jam setelah terakhir mencatat aktivitas. Misalnya, mencatat aktivitas hari ini pukul 07:15 tidak menggeser pengingat besok dari jam 07:00 yang sudah diatur.

### Kondisi pengiriman

| Kondisi | Perilaku |
| --- | --- |
| Hari terjadwal dan target belum selesai | Pengingat otomatis dapat dikirim pada jam yang diatur |
| Progres baru sebagian | Notifikasi menampilkan sisa target |
| Target sudah selesai | Pengingat dilewati; notifikasi yang masih tampil dihapus |
| Hari istirahat atau habit diarsipkan | Tidak ada pengingat untuk habit tersebut |
| Notifikasi digeser/dihapus | Pengingat otomatis tidak dikirim ulang pada tanggal yang sama |
| Jam diubah setelah pengingat hari ini terkirim | Tidak menghasilkan pengingat otomatis kedua pada hari itu |
| Izin notifikasi ditolak atau kanal diblokir | Habit dan jam tetap tersimpan, tetapi notifikasi tidak dikirim |

Ketuk isi notifikasi untuk membuka pencatatan progres habit yang bersangkutan. Notifikasi sendiri tidak memberi XP.

### Menunda dengan snooze

1. Saat notifikasi muncul, perluas notifikasinya jika tombol aksi belum terlihat.
2. Tekan **Tunda 10 menit** atau **Tunda 30 menit**.
3. Notifikasi ditutup dan pengingat tambahan dijadwalkan sesuai pilihanmu.

Snooze adalah pengingat tambahan atas permintaanmu. Snooze dibatalkan jika target selesai, habit diarsipkan, jam pengingat diubah/dimatikan, atau izin notifikasi tidak tersedia. Snooze hanya berlaku pada tanggal yang sama; pilihan yang melewati tengah malam tidak dijadwalkan.

Pengingat menggunakan alarm Android biasa. Penghemat baterai dapat membuat notifikasi terlambat, termasuk snooze. Menutup aplikasi biasa tetap memungkinkan alarm berjalan. Setelah **Paksa berhenti**, buka aplikasi lagi untuk memasang ulang jadwal. Jadwal diperiksa kembali saat aplikasi aktif, perangkat restart, APK diperbarui, atau jam/zona waktu berubah.

## Melihat kalender dan riwayat

1. Buka **Progres**.
2. Di bawah ringkasan semua habit, buka pemilih habit dan pilih yang ingin ditinjau, termasuk habit arsip.
3. Gunakan panah pada kalender untuk berpindah bulan. Kalender tidak dapat maju melewati bulan berjalan.
4. Tekan tanggal untuk melihat status dan jumlah aktivitas tanggal tersebut.
5. Pada tanggal yang memenuhi syarat, gunakan tombol koreksi atau catatan di bagian rincian tanggal.

| Status | Arti |
| --- | --- |
| Selesai | Jumlah aktivitas telah mencapai target tanggal itu |
| Sebagian | Sudah ada progres, tetapi belum mencapai target |
| Terlewat | Hari terjadwal sudah berlalu tanpa progres |
| Belum dikerjakan | Hari ini dijadwalkan, tetapi belum ada progres |
| Tidak dijadwalkan | Hari istirahat habit |
| Diarsipkan | Tanggal berada pada masa habit dijeda melalui arsip |
| Belum dimulai / Belum tiba | Sebelum tanggal pembuatan / tanggal mendatang |

Jika ada progres yang sudah tersimpan pada hari habit diarsipkan, progres itu tetap ditampilkan. Halaman ini juga menampilkan persentase pencapaian bulanan, total aktivitas dalam satuan habit, milestone menuju 30 hari selesai, dan streak terbaik.

## Membaca ringkasan mingguan dan bulanan

1. Buka **Progres** dan cari kartu **Ringkasan semua habit** di bagian atas.
2. Pilih **Mingguan** atau **Bulanan**.
3. Gunakan **‹** untuk periode sebelumnya dan **›** untuk kembali menuju periode sekarang.
4. Baca persentase konsistensi, jumlah target selesai, hari aktif, dan XP periode tersebut.

Ringkasan mencakup seluruh habit, termasuk riwayat habit arsip; mengganti habit pada pemilih di bawahnya tidak membatasi ringkasan ini. Navigasi tersedia sampai 120 periode sebelumnya. Minggu dimulai Senin.

**Target selesai** dihitung per habit per tanggal. Menyelesaikan 3 habit dalam 1 hari berarti 3 target selesai dan 1 hari aktif. Hari aktif berarti tanggal yang memiliki progres lebih dari nol, walaupun targetnya belum selesai.

Persentase konsistensi adalah jumlah target selesai dibagi jumlah target terjadwal yang sudah tiba. Hari istirahat, tanggal sebelum habit dibuat, dan tanggal mendatang tidak masuk pembagi. Aktivitas yang tersimpan pada hari pengarsipan tetap diperhitungkan.

Periode berjalan dibandingkan dengan jumlah hari yang sama pada periode sebelumnya. Periode yang sudah selesai dibandingkan secara utuh, sehingga panjang bulan dapat berbeda. Selisih ditampilkan dalam **poin persentase**: perubahan dari 40% menjadi 60% berarti +20 poin. Jika salah satu periode tidak memiliki jadwal, aplikasi tidak menampilkan perbandingan.

## Memahami XP, level, streak, dan badge

### XP dan level

- Setiap target habit yang selesai pada satu tanggal mendapat **20 XP**.
- Progres sebagian tidak mendapat XP. Catatan, notifikasi, dan snooze tidak memberi XP.
- Total XP dipertahankan saat habit diarsipkan dan saat aplikasi ditutup.
- Setiap **100 XP** menaikkan 1 level. Mulai dari level 1; saat ini tidak ada batas level yang ditetapkan.
- Melewatkan hari tidak mengurangi XP. Koreksi dari selesai menjadi belum selesai dapat menurunkan XP dan level.

| Level | Total XP untuk mencapai level |
| --- | --- |
| 1 | 0 |
| 2 | 100 |
| 3 | 200 |
| 4 | 300 |
| 5 | 400 |
| 10 | 900 |

Rumus level adalah `total XP / 100`, dibulatkan ke bawah, lalu ditambah 1.

### Streak

Streak menghitung penyelesaian **hari terjadwal berturut-turut pada habit yang sama**. Hari istirahat dan masa arsip dilewati. Melewatkan satu hari terjadwal memutus rangkaian.

Contoh: workout dijadwalkan Senin–Rabu–Jumat. Menyelesaikan ketiganya menghasilkan streak 3 hari terjadwal, meskipun Selasa dan Kamis tidak beraktivitas. Statistik yang ditampilkan adalah **streak terbaik** dari riwayat, sehingga tidak otomatis menjadi nol ketika rangkaian terbaru terputus.

### Badge dan milestone

Buka **Profil → Koleksi pencapaian**, lalu tekan badge untuk membaca syarat dan progresnya.

| Badge | Syarat |
| --- | --- |
| Langkah awal | Menyelesaikan satu target harian pertama |
| 7 hari konsisten | Menyelesaikan habit yang sama selama 7 hari terjadwal berturut-turut |
| 30 hari tumbuh | Menyelesaikan habit yang sama pada 30 hari; tidak harus berturut-turut |

Milestone 30 hari pada Progres memakai jumlah hari selesai untuk habit yang dipilih. Badge dihitung dari riwayat terkini, sehingga koreksi progres dapat mengubah statusnya.

## Mengganti nama dan melihat profil

1. Buka **Profil → Edit nama profil**.
2. Masukkan nama panggilan baru, maksimal 30 karakter.
3. Tekan **Simpan nama**.

Sapaan dan inisial avatar diperbarui tanpa mengulang onboarding. Habit, progres, catatan, dan XP tetap tersimpan.

Profil juga menampilkan level, total XP, XP menuju level berikutnya, koleksi badge, hari aktif bulan ini, streak terbaik, dan jumlah habit aktif. **Habit aktif** mengecualikan arsip, tetapi dapat mencakup habit yang tidak dijadwalkan hari ini.

## Membuat backup dan memulihkan data

### Menyimpan backup

1. Buka **Profil → Buat backup offline**.
2. Pilih folder tujuan dan nama file melalui pemilih dokumen Android.
3. Simpan file JSON dan tunggu pesan **Backup berhasil disimpan.**
4. Simpan salinan file di tempat yang dapat diakses setelah pindah HP atau instal ulang, misalnya komputer atau penyimpanan eksternal.

Backup berisi nama, habit aktif/arsip, urutan, versi target dan jadwal, progres, XP, catatan harian, serta catatan pengingat yang telah dikirim. File maksimal 10 MB dan berupa teks JSON yang tidak dienkripsi. Snooze yang sedang menunggu dan izin Android tidak ikut dipindahkan.

### Memulihkan backup

1. Di perangkat tujuan, buka **Profil → Pulihkan backup**. Jika masih di halaman sambutan, pilih **Pulihkan dari backup**.
2. Pilih file backup HabitQuest.
3. Periksa pratinjau nama profil, jumlah habit/arsip, serta catatan progres harian.
4. Tekan **Ganti dan pulihkan** untuk menerapkan isi file, atau **Batal** untuk mempertahankan data perangkat.
5. Tunggu pesan **Backup berhasil dipulihkan.** lalu tekan **Mengerti**.
6. Periksa data dan pastikan izin notifikasi perangkat tujuan aktif jika ingin menerima pengingat.

**Restore mengganti seluruh data perangkat dengan isi file, bukan menggabungkannya.** Buat backup data yang sekarang terlebih dahulu jika masih diperlukan. File tidak valid, terlalu besar, atau versi tidak didukung ditolak sebelum penggantian data.

Backup baru memakai format versi 2. File versi 1 tetap didukung, tanpa catatan harian dan dengan urutan awal. Setelah restore, jadwal pengingat dipasang kembali sesuai izin perangkat tujuan.

Menghapus data aplikasi atau uninstall menghapus database lokal. Pemulihan setelah itu membutuhkan file backup yang sudah disimpan. Tidak tersedia sinkronisasi cloud atau backup otomatis.

## Pertanyaan dan kendala umum

| Kendala | Yang dapat dilakukan |
| --- | --- |
| Habit tidak muncul di Hari Ini | Periksa hari pelaksanaan dan tab Arsip di Kelola habit. Perubahan jadwal dan pemulihan arsip baru berlaku mulai besok. |
| Target baru belum terlihat hari ini | Perubahan target berlaku besok; target hari ini dan riwayat lama tetap memakai versi sebelumnya. |
| Salah memasukkan angka | Gunakan Koreksi progres dan isi jumlah akhir yang benar. |
| XP atau level turun | Periksa koreksi progres atau backup yang baru dipulihkan. Target yang dikoreksi menjadi belum selesai tidak lagi menyumbang 20 XP. |
| Pengingat tidak muncul | Periksa jam, hari pelaksanaan, status arsip, dan apakah target sudah selesai. Gunakan Atur izin notifikasi di Hari Ini atau pengaturan notifikasi Android; buka kembali aplikasi setelah Paksa berhenti. |
| Pengingat terlambat | Waktu alarm dapat bergeser karena penghemat baterai Android. |
| Snooze menjelang tengah malam tidak muncul | Snooze tidak dijadwalkan jika melewati tanggal target. |
| Tidak bisa mengisi progres tanggal mendatang | Pencatatan/koreksi hanya untuk tanggal yang sudah tiba, mulai tanggal pembuatan habit. |
| Tombol atau rincian belum terlihat | Gulir halaman atau isi dialog; sejumlah pengaturan berada di bagian bawah. |
| Backup ditolak | Pilih file JSON backup HabitQuest versi yang didukung dan berukuran maksimal 10 MB. Data saat ini dipertahankan jika validasi gagal. |
| Data lokal belum dapat dibuka | Tekan Coba lagi pada halaman kesalahan. Hindari menghapus data aplikasi sebelum memiliki backup. |

## Penyimpanan lokal

Database `habitquest.db` menggunakan Room 2.8.5 dan memiliki tabel:

| Tabel | Isi |
| --- | --- |
| `user_profile` | Nama panggilan; keberadaan profil menandakan onboarding sudah selesai |
| `habits` | Nama, kategori, target, satuan, jam pengingat, dan tanggal mulai |
| `daily_progress` | Habit, tanggal, target pada hari tersebut, progres, dan XP yang diberikan |
| `reminder_deliveries` | Habit dan tanggal pengingat yang sudah dikirim, untuk mencegah pengiriman ganda |
| `habit_plans` | Versi target, satuan, dan hari pelaksanaan beserta tanggal mulai berlakunya |
| `habit_notes` | Catatan teks per habit dan tanggal, terpisah dari progres |
| `reminder_snoozes` | Pengingat tunda yang sedang menunggu, dengan tanggal dan waktu pemicu |

Kunci unik progres adalah **habit + tanggal**. Perubahan progres dan pemberian XP dilakukan dalam satu transaksi. Total XP dihitung dari catatan tersimpan sehingga tidak kembali nol ketika aplikasi ditutup.

Hari baru membaca catatan tanggal baru (atau progres nol jika belum ada), tanpa menghapus riwayat sebelumnya. Kalender dan statistik dihitung dari catatan yang sama. Tanggal sebelum pembuatan habit dan tanggal mendatang tidak masuk pembagi persentase.

Skema versi 1–4 diekspor di `app/schemas/` dan perlu ikut disimpan di Git. Migrasi 1 → 2 menambahkan catatan pengingat; 2 → 3 menambahkan arsip dan versi jadwal; 3 → 4 menambahkan urutan, catatan harian, dan snooze. Habit lama tetap berjadwal setiap hari dan berurutan menurut ID sampai pengguna mengaturnya. Nama, habit, progres, dan XP dipertahankan. Proyek ini tidak menggunakan destructive migration.

Data tetap ada setelah aplikasi ditutup atau build baru dipasang sebagai pembaruan. Menghapus data aplikasi atau uninstall akan menghapus database; data dapat dipulihkan jika file backup sudah disimpan sebelumnya. Sinkronisasi belum tersedia. Tidak ada akun, server, atau izin internet di aplikasi. Izin notifikasi diminta saat pengguna menyimpan pengingat pertama; izin boot dipakai untuk memasang jadwal kembali setelah restart.

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

- Build APK dan 27 unit test berhasil. Lint selesai dengan 0 error, 14 warning, dan 2 hint.
- 23 pengujian Android berhasil di emulator: penyimpanan lokal, migrasi 1 → 2 → 3 → 4, pengingat/snooze, edit/arsip, koreksi XP, jadwal hari, nama profil, catatan, urutan, serta backup/restore dan penolakan backup tidak valid.
- Uji antarmuka mencakup menyimpan catatan, pilihan ringkasan mingguan/bulanan, edit nama, ekspor file backup, pembatalan restore, dan pemulihan beserta catatan. Pemilih dokumen menerima URI file cache pengujian; tidak menyentuh penyimpanan dokumen pengguna.
- Pengujian menggunakan database sementara atau habit sementara yang dibersihkan setelah pengujian; tidak membuat data contoh permanen.
- Screenshot halaman sambutan: [welcome.png](docs/screenshots/welcome.png).
- Screenshot tampilan awal kosong: [today-empty.png](docs/screenshots/today-empty.png).
- Uji alur onboarding sebelumnya lulus: mengisi nama, masuk ke Hari Ini kosong, memeriksa Progres/Profil bernilai nol, membuka form habit pertama, dan membuka ulang proses aplikasi tanpa mengulang onboarding.
- Screenshot lama `today.png`, `progress.png`, dan `profile.png` mendokumentasikan prototipe sebelum data contoh dihapus.

## Belum tersedia

- Penghapusan permanen habit; gunakan arsip untuk mempertahankan riwayat.
- Sinkronisasi cloud, backup otomatis, jadwal berupa jumlah bebas per minggu, dan perubahan satuan habit lama.
