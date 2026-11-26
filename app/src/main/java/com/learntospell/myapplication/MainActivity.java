package com.learntospell.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private HashMap<String, String> kamusEasy, kamusMedium, kamusDifficult, kamus;
    private ArrayList<String> kataEasy, kataMedium, kataDifficult, kata;
    int JUMLAH_OPSI=5;
    private RecyclerView rvOpsi;
    private OpsiAdapter adapter;
    private TextView textHasil;
    private String soal;
    private ArrayList<String> opsiJawaban;
    private static final String TAG_LC = "Lifecycle";
    private TextView textSkor;
    private int skorBenar = 0;
    private int skorSalah = 0;
    int JUMLAH_SOAL = 3;
    private int soalKe = 0;

    private static final String TAG_TIMER = "Timer";
    private static final long DURASI_SOAL = 15000;

    private TextView textWaktu;
    private RadioGroup radioGroupLevel;
    private CountDownTimer timer;
    private long sisaMs = DURASI_SOAL;
    private boolean sudahDijawab = false;
    private static final String KEY_SKOR_BENAR = "skor_benar";
    private static final String KEY_SKOR_SALAH = "skor_salah";
    private static final String KEY_SOAL = "soal";
    private static final String KEY_OPSI = "opsi";
    private static final String KEY_SISA_MS = "sisa_ms";
    private static final String KEY_LEVEL = "level";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private QuestionPool pembentukSoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG_LC, "onCreate, savedInstanceState " + (savedInstanceState == null ? "null" : "ada"));


        // pilihLevel(radioGroupLevel.getCheckedRadioButtonId());


        textHasil = findViewById(R.id.hasil);
        rvOpsi = findViewById(R.id.opsi);
        rvOpsi.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OpsiAdapter(new OpsiAdapter.OnOpsiClickListener() {
            @Override
            public void onOpsiClick(int position) {
                periksaJawaban(position);
                Log.d("VocabQuiz", "Opsi diklik: posisi " + position);
            }
        });
        rvOpsi.setAdapter(adapter);
        Button tombolBerikutnya = findViewById(R.id.tombolBerikutnya);
        tombolBerikutnya.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                letsPlay();
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
        kamus = kamusEasy;
        kata = kataEasy;
        textSkor = findViewById(R.id.skor);
        updateSkor();
        textWaktu = findViewById(R.id.waktu);

        radioGroupLevel = findViewById(R.id.radioGroupLevel);


        radioGroupLevel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                pilihLevel(checkedId);
                letsPlay();
            }
        });

        if (savedInstanceState != null)
            Log.d(TAG_LC, "onCreate, Grouplevel " + savedInstanceState.getInt(KEY_LEVEL));


        if (savedInstanceState != null) {
            skorBenar = savedInstanceState.getInt(KEY_SKOR_BENAR);
            skorSalah = savedInstanceState.getInt(KEY_SKOR_SALAH);
            radioGroupLevel.check(savedInstanceState.getInt(KEY_LEVEL));
            updateSkor();

        }


        tesApi();
        tesPembentukSoal();
        letsPlay();
    }

    private String kodeLevel() {
        int id = radioGroupLevel.getCheckedRadioButtonId();
        if (id == R.id.radioEasy) return "Easy";
        if (id == R.id.radioMedium) return "Medium";
        return "Difficult";
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
        //tampilkanRingkasan();
    }

    private void tampilkanRingkasan() {
        int nilai = skorBenar * 100 / JUMLAH_SOAL;
        new AlertDialog.Builder(this)
            .setTitle("Sesi selesai")
            .setMessage("Level: " + kodeLevel() + "Nilai: " + nilai)
                .setPositiveButton("Mau main lagi?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mulaiSesiBaru();
                    }
                }).show();
    }

    private void pilihLevel(int checkedId) {
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

    }

    private void letsPlay() {
        if (soalKe <= JUMLAH_SOAL) {
            soalKe++;
            opsiJawaban = new ArrayList<>();
            soal = null;
            textHasil.setText("");


            ArrayList<String> acak = new ArrayList<>(kata);
            Collections.shuffle(acak);
            soal= acak.get(0);

            for (int i = 0; i < JUMLAH_OPSI; i++) {
                opsiJawaban.add(kamus.get(acak.get(i)));
            }
            Collections.shuffle(opsiJawaban);

            TextView textSoal = findViewById(R.id.vocab);
            textSoal.setText(soal);
            // adapter.setOpsi(opsiJawaban);
            textHasil.setText("");
            adapter.setOpsi(opsiJawaban, opsiJawaban.indexOf(kamus.get(soal)));

            sisaMs = DURASI_SOAL;
            mulaiTimer(sisaMs);
        } else {
            selesaiSesi();
        }
    }

    private void periksaJawaban(int position) {
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
        textSkor.setText("Soal no: " + (soalKe+1) + " Benar: " + skorBenar + "  Salah: " + skorSalah);
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
        List<String> terpilih = new ArrayList<>(kandidat.subList(0, Math.min(40, kandidat.size())));

        pembentukSoal = new QuestionPool();
        pembentukSoal.build(terpilih, new QuestionPool.Listener() {
            @Override
            public void onProgress(int ke, int total) {
                Log.d("TES_KELOMPOK", "Progres " + ke + "/" + total);
            }

            @Override
            public void onSelesai(boolean berhasil, List<WordEntry> valid) {
                Log.d("TES_KELOMPOK", "berhasil=" + berhasil + ", entry valid=" + valid.size());
                Map<String, List<QuestionWord>> kelompok = WordGrouper.kelompokkan(valid);
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
        Log.d(TAG_LC, "onResume");
        if (!sudahDijawab && sisaMs > 0) {
            mulaiTimer(sisaMs);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG_LC, "onPause");
        hentikanTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG_LC, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pembentukSoal != null) pembentukSoal.batal();
        Log.d(TAG_LC, "onDestroy, isFinishing=" + isFinishing()
                + ", isChangingConfigurations=" + isChangingConfigurations());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SKOR_BENAR, skorBenar);
        outState.putInt(KEY_SKOR_SALAH, skorSalah);

        outState.putString(KEY_SOAL, soal);
        outState.putStringArrayList(KEY_OPSI, opsiJawaban);
        outState.putLong(KEY_SISA_MS, sisaMs);
        outState.putInt(KEY_LEVEL, radioGroupLevel.getCheckedRadioButtonId() );
        Log.d(TAG_LC, "onSaveInstanceState, skor=" + skorBenar + "/" + skorSalah
        + radioGroupLevel.getCheckedRadioButtonId());
    }

}