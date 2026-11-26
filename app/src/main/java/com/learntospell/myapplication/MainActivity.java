package com.learntospell.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private HashMap<String, String> kamusEasy, kamusMedium, kamusDifficult, kamus;
    private ArrayList<String> kataEasy, kataMedium, kataDifficult, kata;
    private HashMap<String, QuestionWord> infoSoalOnline = new HashMap<>();

    int JUMLAH_OPSI = 5;
    int JUMLAH_SOAL = 15;
    private int soalKe = 0;

    private RecyclerView rvOpsi;
    private OpsiAdapter adapter;
    private TextView textHasil, textSkor, textWaktu, textSoal;
    private RadioGroup radioGroupLevel;
    private RadioButton radioEasy, radioMedium, radioDifficult;
    private Button tombolBerikutnya;

    // UI Loading, Offline, dan Panel Penjelasan
    private LinearLayout layoutOfflineBanner, layoutLoadingAwal, panelPenjelasan;
    private ConstraintLayout layoutGame;
    private TextView textOfflineInfo, textLoadingProgres, textPenjelasanKata, textContoh;
    private Button btnCobaOnline;

    private String soal;
    private ArrayList<String> opsiJawaban;
    private int skorBenar = 0;
    private int skorSalah = 0;

    private static final String TAG_LC = "Lifecycle";
    private static final String TAG_TIMER = "Timer";
    private static final long DURASI_SOAL = 15000;

    private CountDownTimer timer;
    private long sisaMs = DURASI_SOAL;
    private boolean sudahDijawab = false;

    private static final String KEY_SKOR_BENAR = "skor_benar";
    private static final String KEY_SKOR_SALAH = "skor_salah";
    private static final String KEY_SOAL = "soal";
    private static final String KEY_OPSI = "opsi";
    private static final String KEY_SISA_MS = "sisa_ms";
    private static final String KEY_LEVEL = "level";

    // Poin 10: Simpan hasil persiapan soal di field static agar putar layar tidak mengulang 40 request
    private static Map<String, List<QuestionWord>> cacheKelompokOnline = null;
    private static Boolean cacheModeOffline = null;

    private boolean modeOffline = false;
    private QuestionPool pembentukSoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG_LC, "onCreate, savedInstanceState " + (savedInstanceState == null ? "null" : "ada"));

        textSoal = findViewById(R.id.vocab);
        textHasil = findViewById(R.id.hasil);
        textSkor = findViewById(R.id.skor);
        textWaktu = findViewById(R.id.waktu);
        rvOpsi = findViewById(R.id.opsi);
        radioGroupLevel = findViewById(R.id.radioGroupLevel);
        radioEasy = findViewById(R.id.radioEasy);
        radioMedium = findViewById(R.id.radioMedium);
        radioDifficult = findViewById(R.id.radioDifficult);
        tombolBerikutnya = findViewById(R.id.tombolBerikutnya);

        layoutOfflineBanner = findViewById(R.id.layoutOfflineBanner);
        textOfflineInfo = findViewById(R.id.textOfflineInfo);
        btnCobaOnline = findViewById(R.id.btnCobaOnline);
        layoutLoadingAwal = findViewById(R.id.layoutLoadingAwal);
        textLoadingProgres = findViewById(R.id.textLoadingProgres);
        layoutGame = findViewById(R.id.layoutGame);

        panelPenjelasan = findViewById(R.id.panelPenjelasan);
        textPenjelasanKata = findViewById(R.id.textPenjelasanKata);
        textContoh = findViewById(R.id.textContoh);

        rvOpsi.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OpsiAdapter(new OpsiAdapter.OnOpsiClickListener() {
            @Override
            public void onOpsiClick(int position) {
                if (!sudahDijawab) {
                    periksaJawaban(position);
                }
            }
        });
        rvOpsi.setAdapter(adapter);

        // Poin 11: Tombol Next tidak perlu membatalkan request karena tidak ada request per soal
        tombolBerikutnya.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                letsPlay();
            }
        });

        // Poin 8: Tombol "Coba online lagi" memanggil siapkanSoal() lagi
        btnCobaOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cacheKelompokOnline = null;
                cacheModeOffline = null;
                siapkanSoal();
            }
        });

        kamus = new HashMap<>();
        kata = new ArrayList<>();
        kamusEasy = new HashMap<>();
        kataEasy = new ArrayList<>();
        kamusMedium = new HashMap<>();
        kataMedium = new ArrayList<>();
        kamusDifficult = new HashMap<>();
        kataDifficult = new ArrayList<>();

        readFromFile(R.raw.vocab_easy, kamusEasy, kataEasy);
        readFromFile(R.raw.vocab_medium, kamusMedium, kataMedium);
        readFromFile(R.raw.vocab_difficult, kamusDifficult, kataDifficult);

        radioGroupLevel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                pilihLevel(checkedId);
                mulaiSesiBaru();
            }
        });

        if (savedInstanceState != null) {
            skorBenar = savedInstanceState.getInt(KEY_SKOR_BENAR);
            skorSalah = savedInstanceState.getInt(KEY_SKOR_SALAH);
            radioGroupLevel.check(savedInstanceState.getInt(KEY_LEVEL));
            updateSkor();
        }

        // Poin 1 & 10: Cek cache static saat rotasi; jika belum ada, panggil siapkanSoal()
        if (cacheModeOffline != null) {
            if (!cacheModeOffline && cacheKelompokOnline != null && !cacheKelompokOnline.isEmpty()) {
                masukModeOnline(savedInstanceState != null);
            } else {
                masukModeOffline("Mode offline: memakai data bawaan.", savedInstanceState != null);
            }
        } else {
            siapkanSoal();
        }
    }

    // Poin 1, 2, 3: Panggil pembentuk soal saat game dibuka dengan layar loading
    private void siapkanSoal() {
        hentikanTimer();
        if (pembentukSoal != null) pembentukSoal.batal();

        // Poin 2 + Revisi: Sembunyikan skor, waktu, dan seluruh tampilan game selama loading
        textSkor.setVisibility(View.GONE);
        textWaktu.setVisibility(View.GONE);
        layoutOfflineBanner.setVisibility(View.GONE);
        layoutGame.setVisibility(View.GONE);
        radioGroupLevel.setVisibility(View.GONE);
        tombolBerikutnya.setVisibility(View.GONE);
        layoutLoadingAwal.setVisibility(View.VISIBLE);
        textLoadingProgres.setText("Menyiapkan soal 0/40");

        List<String> kandidat = new ArrayList<>();
        kandidat.addAll(kataEasy);
        kandidat.addAll(kataMedium);
        kandidat.addAll(kataDifficult);
        Collections.shuffle(kandidat);
        List<String> terpilih = new ArrayList<>(kandidat.subList(0, Math.min(40, kandidat.size())));

        pembentukSoal = new QuestionPool();
        pembentukSoal.build(terpilih, new QuestionPool.Listener() {
            @Override
            public void onProgress(int ke, int total) {
                textLoadingProgres.setText("Menyiapkan soal " + ke + "/" + total);
            }

            @Override
            public void onSelesai(boolean berhasil, List<WordEntry> valid) {
                // Poin 2 + Revisi: Tampilkan kembali skor, waktu, dan tampilan game setelah onSelesai
                layoutLoadingAwal.setVisibility(View.GONE);
                textSkor.setVisibility(View.VISIBLE);
                textWaktu.setVisibility(View.VISIBLE);
                layoutGame.setVisibility(View.VISIBLE);
                radioGroupLevel.setVisibility(View.VISIBLE);
                tombolBerikutnya.setVisibility(View.VISIBLE);

                // Poin 3: Menentukan mode online atau offline
                Map<String, List<QuestionWord>> kelompok = WordGrouper.kelompokkan(valid);
                if (berhasil && !kelompok.isEmpty()) {
                    cacheKelompokOnline = kelompok;
                    cacheModeOffline = false;
                    masukModeOnline(false);
                } else {
                    cacheKelompokOnline = null;
                    cacheModeOffline = true;
                    masukModeOffline("Gagal memuat soal online. Memakai data bawaan.", false);
                }
            }
        });
    }

    private void masukModeOnline(boolean dariRotasi) {
        modeOffline = false;
        layoutLoadingAwal.setVisibility(View.GONE);
        layoutOfflineBanner.setVisibility(View.GONE);
        textSkor.setVisibility(View.VISIBLE);
        textWaktu.setVisibility(View.VISIBLE);
        layoutGame.setVisibility(View.VISIBLE);
        radioGroupLevel.setVisibility(View.VISIBLE);
        tombolBerikutnya.setVisibility(View.VISIBLE);

        // Poin 4: Ubah teks radio ke Noun / Verb / Adjective & sembunyikan yang tidak ada kelompoknya
        radioEasy.setText("Noun");
        radioEasy.setVisibility(cacheKelompokOnline.containsKey("noun") ? View.VISIBLE : View.GONE);

        radioMedium.setText("Verb");
        radioMedium.setVisibility(cacheKelompokOnline.containsKey("verb") ? View.VISIBLE : View.GONE);

        radioDifficult.setText("Adjective");
        radioDifficult.setVisibility(cacheKelompokOnline.containsKey("adjective") ? View.VISIBLE : View.GONE);

        if (!dariRotasi) {
            if (cacheKelompokOnline.containsKey("noun")) {
                radioEasy.setChecked(true);
            } else if (cacheKelompokOnline.containsKey("verb")) {
                radioMedium.setChecked(true);
            } else if (cacheKelompokOnline.containsKey("adjective")) {
                radioDifficult.setChecked(true);
            }
        }

        pilihLevel(radioGroupLevel.getCheckedRadioButtonId());
        mulaiSesiBaru();
    }

    // Poin 8: Mode offline mengembalikan radio ke Easy / Medium / Difficult dan memakai .txt
    private void masukModeOffline(String pesan, boolean dariRotasi) {
        modeOffline = true;
        layoutLoadingAwal.setVisibility(View.GONE);
        layoutOfflineBanner.setVisibility(View.VISIBLE);
        textOfflineInfo.setText(pesan);
        panelPenjelasan.setVisibility(View.GONE);

        textSkor.setVisibility(View.VISIBLE);
        textWaktu.setVisibility(View.VISIBLE);
        layoutGame.setVisibility(View.VISIBLE);
        radioGroupLevel.setVisibility(View.VISIBLE);
        tombolBerikutnya.setVisibility(View.VISIBLE);

        radioEasy.setVisibility(View.VISIBLE);
        radioEasy.setText("Easy");
        radioMedium.setVisibility(View.VISIBLE);
        radioMedium.setText("Medium");
        radioDifficult.setVisibility(View.VISIBLE);
        radioDifficult.setText("Difficult");

        if (!dariRotasi) {
            radioEasy.setChecked(true);
        }

        pilihLevel(radioGroupLevel.getCheckedRadioButtonId());
        mulaiSesiBaru();
    }

    // Poin 5: Mengisi kata dan kamus dari kelompok yang dipilih sehingga letsPlay() hampir tidak berubah
    private void pilihLevel(int checkedId) {
        if (modeOffline) {
            if (checkedId == R.id.radioEasy) {
                kamus = kamusEasy;
                kata = kataEasy;
            } else if (checkedId == R.id.radioMedium) {
                kamus = kamusMedium;
                kata = kataMedium;
            } else if (checkedId == R.id.radioDifficult) {
                kamus = kamusDifficult;
                kata = kataDifficult;
            }
        } else if (cacheKelompokOnline != null) {
            String jenis = "noun";
            if (checkedId == R.id.radioMedium) jenis = "verb";
            else if (checkedId == R.id.radioDifficult) jenis = "adjective";

            List<QuestionWord> daftar = cacheKelompokOnline.get(jenis);
            kamus = new HashMap<>();
            kata = new ArrayList<>();
            infoSoalOnline = new HashMap<>();

            if (daftar != null) {
                for (QuestionWord qw : daftar) {
                    kamus.put(qw.word, qw.definition);
                    kata.add(qw.word);
                    infoSoalOnline.put(qw.word, qw);
                }
            }
        }

        // Poin 6: Atur JUMLAH_SOAL = Math.min(15, kata.size())
        JUMLAH_SOAL = Math.min(15, kata.size());
    }

    // Poin 9: Perbarui kodeLevel() agar RingkasanHasil menampilkan jenis kata di mode online & level di mode offline
    private String kodeLevel() {
        int id = radioGroupLevel.getCheckedRadioButtonId();
        if (modeOffline) {
            if (id == R.id.radioEasy) return "Easy";
            if (id == R.id.radioMedium) return "Medium";
            return "Difficult";
        } else {
            if (id == R.id.radioEasy) return "Noun";
            if (id == R.id.radioMedium) return "Verb";
            return "Adjective";
        }
    }

    private void mulaiSesiBaru() {
        soalKe = 0;
        skorBenar = 0;
        skorSalah = 0;
        letsPlay();
    }

    private void selesaiSesi() {
        hentikanTimer();
        sudahDijawab = true;
        Intent intent = new Intent(this, RingkasanHasil.class);
        intent.putExtra("JUMLAH_BENAR", skorBenar);
        intent.putExtra("JUMLAH_SALAH", skorSalah);
        intent.putExtra("JUMLAH_SOAL", JUMLAH_SOAL);
        intent.putExtra("LEVEL", kodeLevel());
        startActivity(intent);

        mulaiSesiBaru();
    }

    // Poin 5 & 6: Kondisi diperbaiki menjadi soalKe < JUMLAH_SOAL dan logika inti tetap sama
    private void letsPlay() {
        if (soalKe < JUMLAH_SOAL) {
            sudahDijawab = false;
            opsiJawaban = new ArrayList<>();
            soal = null;
            textHasil.setText("");
            panelPenjelasan.setVisibility(View.GONE);

            ArrayList<String> acak = new ArrayList<>(kata);
            Collections.shuffle(acak);
            soal = acak.get(0);

            int jumlahOpsiTampil = Math.min(JUMLAH_OPSI, acak.size());
            for (int i = 0; i < jumlahOpsiTampil; i++) {
                opsiJawaban.add(kamus.get(acak.get(i)));
            }
            Collections.shuffle(opsiJawaban);

            soalKe++;
            updateSkor();

            textSoal.setText(soal);
            adapter.setOpsi(opsiJawaban, opsiJawaban.indexOf(kamus.get(soal)));

            sisaMs = DURASI_SOAL;
            mulaiTimer(sisaMs);
        } else {
            selesaiSesi();
        }
    }

    private void periksaJawaban(int position) {
        sudahDijawab = true;
        hentikanTimer();
        adapter.tandaiJawaban(position);
        String benar = kamus.get(soal);
        String diklik = opsiJawaban.get(position);
        if (diklik.equals(benar)) {
            skorBenar++;
            textHasil.setText("Benar!");
        } else {
            skorSalah++;
            textHasil.setText("Salah. Jawaban benar: " + benar);
        }
        updateSkor();
        tampilkanPanelPenjelasan();
    }

    private void waktuHabis() {
        sudahDijawab = true;
        skorSalah++;
        updateSkor();
        adapter.tandaiWaktuHabis();
        textHasil.setText("Waktu habis. Jawaban benar: " + kamus.get(soal));
        tampilkanPanelPenjelasan();
    }

    // Poin 7 & 8: Tampilkan kata, jenis kata, dan contoh kalimat di mode online; sembunyikan di mode offline
    private void tampilkanPanelPenjelasan() {
        if (modeOffline) {
            panelPenjelasan.setVisibility(View.GONE);
            return;
        }

        QuestionWord qw = infoSoalOnline.get(soal);
        if (qw != null) {
            panelPenjelasan.setVisibility(View.VISIBLE);
            textPenjelasanKata.setText(qw.word + " (" + qw.partOfSpeech + ")");

            if (qw.example != null && !qw.example.trim().isEmpty()) {
                textContoh.setVisibility(View.VISIBLE);
                textContoh.setText("Contoh: \"" + qw.example + "\"");
            } else {
                textContoh.setVisibility(View.GONE);
            }
        }
    }

    // Helper untuk Orang C (Alfa) mengambil QuestionWord soal yang sedang berjalan
    public QuestionWord getQuestionWordSaatIni() {
        return infoSoalOnline.get(soal);
    }

    private void readFromFile(int resourceId, HashMap<String, String> kamus, ArrayList<String> kata) {
        Scanner scan = new Scanner(getResources().openRawResource(resourceId));
        while (scan.hasNextLine()) {
            String[] t = scan.nextLine().split("\t");
            if (t.length >= 2) {
                kamus.put(t[0].trim(), t[1].trim());
                kata.add(t[0].trim());
            }
        }
        scan.close();
    }

    private void updateSkor() {
        textSkor.setText("Soal: " + soalKe + "/" + JUMLAH_SOAL + "  Benar: " + skorBenar + "  Salah: " + skorSalah);
    }

    private void mulaiTimer(long durasi) {
        hentikanTimer();
        timer = new CountDownTimer(durasi, 1000) {
            @Override
            public void onTick(long ms) {
                sisaMs = ms;
                textWaktu.setText((ms + 999) / 1000 + " detik");
                Log.d(TAG_TIMER, "tick " + (ms + 999) / 1000
                        + " dari activity " + Integer.toHexString(MainActivity.this.hashCode()));
            }

            @Override
            public void onFinish() {
                sisaMs = 0;
                textWaktu.setText("0 detik");
                waktuHabis();
            }
        }.start();
    }

    private void hentikanTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void waktuHabis() {
        skorSalah++;
        updateSkor();
        adapter.tandaiWaktuHabis();
        textHasil.setText("Waktu habis. Jawaban benar: " + kamus.get(soal));
    }

    //DIHAPUS KETIKA LANJUT KE TAHAP B, HANYA UNTUK UJI APAKAH CODE TAHAP A BERHASIL
    //Hasil : seluruh code tahap A berhasil, berhasil memanggil API beserta requestnya, dan berhasil mendapat responses
    //Hasil : crosscheck soal .txt ke API sudah berhasil, dengan mekanisme diambil secara acak tiap run, sebanyak 40 kata
    //Hasil : pembentuk soal sudah berhasil, tinggal pembuatan layout/pemanggilan id soal di layout nya saja untuk menampilkan soalnya
    private void tesApi() {
        ApiClient.getApi().getWord("hello").enqueue(new retrofit2.Callback<List<WordEntry>>() {
            @Override
            public void onResponse(Call<List<WordEntry>> call, Response<List<WordEntry>> response) {
                Log.d("TES_API", "Kode status: " + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WordEntry entry = response.body().get(0);
                    Log.d("TES_API", "Kata: " + entry.word);
                    Log.d("TES_API", "Fonetik: " + entry.phonetic);
                    if (entry.meanings != null && !entry.meanings.isEmpty()
                            && entry.meanings.get(0).definitions != null
                            && !entry.meanings.get(0).definitions.isEmpty()) {
                        Log.d("TES_API", "Definisi: "
                                + entry.meanings.get(0).definitions.get(0).definition);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<WordEntry>> call, Throwable t) {
                Log.e("TES_API", "Gagal: " + t.getMessage());
            }
        });
    }
    private void tesPembentukSoal() {
        List<String> kandidat = new ArrayList<>();
        kandidat.addAll(kataEasy);
        kandidat.addAll(kataMedium);
        kandidat.addAll(kataDifficult);
        Collections.shuffle(kandidat);

        pembentukSoal = new QuestionPool();
        pembentukSoal.build(kandidat, new QuestionPool.Listener() {
            @Override
            public void onProgress(Map<String, Integer> jumlah) {
                Log.d("TES_KELOMPOK", "Progres " + jumlah);
            }

            @Override
            public void onSelesai(boolean berhasil, Map<String, List<QuestionWord>> kelompok, int jumlahRequest) {
                Log.d("TES_KELOMPOK", "berhasil=" + berhasil + ", request=" + jumlahRequest);
                for (Map.Entry<String, List<QuestionWord>> e : kelompok.entrySet()) {
                    Log.d("TES_KELOMPOK", e.getKey() + ": " + e.getValue().size() + " kata");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG_LC, "onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG_LC, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sudahDijawab && sisaMs > 0 && layoutGame.getVisibility() == View.VISIBLE) {
            mulaiTimer(sisaMs);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hentikanTimer();
    }

    // Poin 11: Panggil pembentukSoal.batal() di onDestroy()
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pembentukSoal != null) pembentukSoal.batal();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SKOR_BENAR, skorBenar);
        outState.putInt(KEY_SKOR_SALAH, skorSalah);
        outState.putString(KEY_SOAL, soal);
        outState.putStringArrayList(KEY_OPSI, opsiJawaban);
        outState.putLong(KEY_SISA_MS, sisaMs);
        outState.putInt(KEY_LEVEL, radioGroupLevel.getCheckedRadioButtonId());
    }
}