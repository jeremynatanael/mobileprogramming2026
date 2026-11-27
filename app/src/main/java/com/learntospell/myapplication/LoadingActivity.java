package com.learntospell.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class LoadingActivity extends AppCompatActivity {

    // Disimpan static agar MainActivity langsung memakainya & aman saat rotasi layar
    public static Map<String, List<QuestionWord>> cacheKelompokOnline = null;
    public static boolean cacheModeOffline = false;
    public static String pesanOffline = "";

    private TextView textLoadingProgres;
    private QuestionPool pembentukSoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        textLoadingProgres = findViewById(R.id.textLoadingProgres);
        siapkanSoal();
    }

    private void siapkanSoal() {
        List<String> kandidat = new ArrayList<>();
        bacaKataDariFile(R.raw.vocab_easy, kandidat);
        bacaKataDariFile(R.raw.vocab_medium, kandidat);
        bacaKataDariFile(R.raw.vocab_difficult, kandidat);
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
                Map<String, List<QuestionWord>> kelompokValid = new HashMap<>();
                if (kelompok != null) {
                    for (Map.Entry<String, List<QuestionWord>> entry : kelompok.entrySet()) {
                        // Ganti WordGrouper.MIN_PER_KELOMPOK menjadi angka 5
                        if (entry.getValue() != null && entry.getValue().size() >= 5) {
                            kelompokValid.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                if (berhasil && !kelompokValid.isEmpty()) {
                    cacheKelompokOnline = kelompokValid;
                    cacheModeOffline = false;
                    pesanOffline = "";
                } else {
                    cacheKelompokOnline = null;
                    cacheModeOffline = true;
                    pesanOffline = "Gagal memuat soal online. Memakai data bawaan.";
                }

                pindahKeMainActivity();
            }
        });
    }

    private void bacaKataDariFile(int resourceId, List<String> tujuan) {
        Scanner scan = new Scanner(getResources().openRawResource(resourceId));
        while (scan.hasNextLine()) {
            String[] t = scan.nextLine().split("\t");
            if (t.length >= 2) {
                tujuan.add(t[0].trim());
            }
        }
        scan.close();
    }

    private void pindahKeMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Tutup LoadingActivity agar tombol Back tidak kembali ke layar loading
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pembentukSoal != null) pembentukSoal.batal();
    }
}