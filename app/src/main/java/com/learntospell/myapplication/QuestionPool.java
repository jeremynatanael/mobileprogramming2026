package com.learntospell.myapplication;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class QuestionPool {

    public static final int TARGET_PER_KELOMPOK = 15;
    private static final int MAKS_REQUEST = 120;
    private static final int MAKS_GAGAL_BERUNTUN = 3;

    public interface Listener {
        void onProgress(Map<String, Integer> jumlahPerJenis);
        void onSelesai(boolean berhasil, Map<String, List<QuestionWord>> kelompok, int jumlahRequest);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public void build(List<String> kandidat, Listener listener) {
        executor.execute(() -> {
            Map<String, List<QuestionWord>> kelompok = WordGrouper.buatKosong();
            Set<String> sudahAda = new HashSet<>();
            int jumlahRequest = 0;
            int gagalBeruntun = 0;
            boolean berhasil = true;


            for(String kata : kandidat) {
                if(WordGrouper.semuaPenuh(kelompok, TARGET_PER_KELOMPOK)) break;
                if(jumlahRequest >= MAKS_REQUEST) break;
                jumlahRequest++;

                try {
                    Response<List<WordEntry>> res =
                            ApiClient.getApi().getWord(kata).execute();
                    if (res.isSuccessful() && res.body() != null) {
                        for (WordEntry e : res.body()) {
                            WordGrouper.tambah(e, kelompok, sudahAda, TARGET_PER_KELOMPOK);
                        }
                        gagalBeruntun = 0;
                    } else if (res.code() == 404) {
                        gagalBeruntun = 0;          // kata tidak ada di kamus: wajar
                    } else {
                        gagalBeruntun++;            // 429, 5xx
                    }
                } catch (IOException e) {
                    gagalBeruntun++;
                }

                if (gagalBeruntun >= MAKS_GAGAL_BERUNTUN) { berhasil = false; break; }

                Map<String, Integer> snapshot = new LinkedHashMap<>();
                for (Map.Entry<String, List<QuestionWord>> e : kelompok.entrySet()) {
                    snapshot.put(e.getKey(), e.getValue().size());
                }
                main.post(() -> listener.onProgress(snapshot));
            }

            // Tidak ada satu kata pun yang terkumpul -> anggap gagal (mode offline)
            int total = 0;
            for (List<QuestionWord> d : kelompok.values()) total += d.size();
            boolean hasil = berhasil && total > 0;
            int req = jumlahRequest;
            main.post(() -> listener.onSelesai(hasil, kelompok, req));
        });
    }
    public void batal() {
        executor.shutdownNow();
    }
}
