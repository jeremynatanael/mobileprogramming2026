package com.learntospell.myapplication;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class QuestionPool {

    public interface Listener {
        void onProgress(int ke, int total);
        void onSelesai(boolean berhasil, List<WordEntry> valid);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public void build(List<String> kandidat, Listener listener) {
        executor.execute(() -> {
            List<WordEntry> valid = new ArrayList<>();
            boolean berhasil = true;
            for (int i = 0; i < kandidat.size(); i++) {
                try {
                    Response<List<WordEntry>> res =
                            ApiClient.getApi().getWord(kandidat.get(i)).execute();
                    if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                        valid.addAll(res.body());   // 404 otomatis dilewati
                    }
                } catch (IOException e) {
                    berhasil = false;                   // internet mati -> mode offline
                    break;
                }
                int ke = i + 1;
                main.post(() -> listener.onProgress(ke, kandidat.size()));
            }
            boolean hasil = berhasil;
            main.post(() -> listener.onSelesai(hasil, valid));
        });
    }
    public void batal() {
        executor.shutdownNow();
    }
}
