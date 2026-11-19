package com.learntospell.myapplication;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static DictionaryApi api;

    public static DictionaryApi getApi() {
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl("https://api.dictionaryapi.dev/api/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DictionaryApi.class);
        }
        return api;
    }
}
