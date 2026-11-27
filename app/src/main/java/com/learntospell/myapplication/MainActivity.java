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

    // Simpan hasil persiapan soal di field static agar putar layar tidak mengulang request
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

        tombolBerikutnya.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                letsPlay();
            }
        });

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

    // Disesuaikan dengan QuestionPool terbaru milik Jeremy
    private void siapkanSoal() {
        hentikanTimer();
        if (pembentukSoal != null) pembentukSoal.batal();

        textSkor.setVisibility(View.GONE);
        textWaktu.setVisibility(View.GONE);
        layoutOfflineBanner.setVisibility(View.GONE);
        layoutGame.setVisibility(View.GONE);
        radioGroupLevel.setVisibility(View.GONE);
        tombolBerikutnya.setVisibility(View.GONE);
        layoutLoadingAwal.setVisibility(View.VISIBLE);
        textLoadingProgres.setText("Menyiapkan soal...\nNoun: 0/15 | Verb: 0/15 | Adj: 0/15");

        List<String> kandidat = new ArrayList<>();
        kandidat.addAll(kataEasy);
        kandidat.addAll(kataMedium);
        kandidat.addAll(kataDifficult);
        Collections.shuffle(kandidat);

        pembentukSoal = new QuestionPool();
        pembentukSoal.build(kandidat, new QuestionPool.Listener() {
            @Override
            public void onProgress(Map<String, Integer> jumlahPerJenis) {
                int n = jumlahPerJenis.getOrDefault("noun", 0);
                int v = jumlahPerJenis.getOrDefault("verb", 0);
                int a = jumlahPerJenis.getOrDefault("adjective", 0);
                int target = QuestionPool.TARGET_PER_KELOMPOK;
                textLoadingProgres.setText("Menyiapkan soal...\nNoun: " + n + "/" + target
                        + " | Verb: " + v + "/" + target
                        + " | Adj: " + a + "/" + target);
            }

            @Override
            public void onSelesai(boolean berhasil, Map<String, List<QuestionWord>> kelompok, int jumlahRequest) {
                layoutLoadingAwal.setVisibility(View.GONE);
                textSkor.setVisibility(View.VISIBLE);
                textWaktu.setVisibility(View.VISIBLE);
                layoutGame.setVisibility(View.VISIBLE);
                radioGroupLevel.setVisibility(View.VISIBLE);
                tombolBerikutnya.setVisibility(View.VISIBLE);

                // Bersihkan kelompok yang kosong (< 5 kata atau 0 kata)
                //Map<String, List<QuestionWord>> kelompokValid = new HashMap<>();
                //if (kelompok != null) {
                    //for (Map.Entry<String, List<QuestionWord>> entry : kelompok.entrySet()) {
                        //if (entry.getValue() != null && entry.getValue().size() >= 5) {
                            //kelompokValid.put(entry.getKey(), entry.getValue());
                        //}
                    //}
                //}

                if (kelompok != null) {
                    for (Map.Entry<String, List<QuestionWord>> e : kelompok.entrySet()) {
                        Log.d("TES_KELOMPOK", e.getKey() + ": " + e.getValue().size() + " kata (request=" + jumlahRequest + ")");
                        Log.d("TES_KELOMPOK", "berhasil: " + berhasil + ", request:" + jumlahRequest);
                    }
                }

                if (berhasil && kelompok != null) {
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

        radioEasy.setText("Noun");
        radioEasy.setVisibility(View.VISIBLE);

        radioMedium.setText("Verb");
        radioMedium.setVisibility(View.VISIBLE);

        radioDifficult.setText("Adjective");
        radioDifficult.setVisibility(View.VISIBLE);

        if (!dariRotasi) {
            radioEasy.setChecked(true);
        }

        pilihLevel(radioGroupLevel.getCheckedRadioButtonId());
        mulaiSesiBaru();
    }

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

        JUMLAH_SOAL = Math.min(15, kata.size());
    }

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

        if (kata == null || kata.isEmpty()) {
            hentikanTimer();
            textSoal.setText("-");
            textHasil.setText("Kategori ini belum punya kata. Pilih kategori lain");
            adapter.setOpsi(new ArrayList<>(), -1);
            return;
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