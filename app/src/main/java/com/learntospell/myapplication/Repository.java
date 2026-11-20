package com.learntospell.myapplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class Repository {

    public interface WordCallback {
        void onResult(UiState state);
    }

    private final Map<String, WordInfo> cache = new HashMap<>();
    private Call<List<WordEntry>> currentCall;

    public void getWordInfo(String word, WordCallback callback) {
        WordInfo cached = cache.get(word);
        if (cached != null) {
            callback.onResult(UiState.SUCCESS(cached));
            return;
        }

        cancel(); //batalkan request lama saat membuat yang baru
        currentCall = ApiClient.getApi().getWord(word);
        currentCall.enqueue(new retrofit2.Callback<List<WordEntry>>() {
            @Override
            public void onResponse(Call<List<WordEntry>> call, Response<List<WordEntry>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WordInfo info = WordInfo.from(response.body().get(0));
                    cache.put(word, info);
                    callback.onResult(UiState.SUCCESS(info));
                } else if (response.code() == 404) {
                    callback.onResult(UiState.ERROR("Definisi tidak ditemukan"));
                } else {
                    callback.onResult(UiState.ERROR("Layanan bermasalah, coba lagi nanti"));
                }
            }
            @Override
            public void onFailure(Call<List<WordEntry>> call, Throwable t) {
                if (call.isCanceled()) return;
                callback.onResult(UiState.ERROR("Periksa koneksi internet"));
            }
        });
    }
    public void cancel() {
        if (currentCall != null) currentCall.cancel();
    }


}
