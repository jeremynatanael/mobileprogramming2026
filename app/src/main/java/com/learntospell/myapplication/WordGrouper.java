package com.learntospell.myapplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WordGrouper {
    public static final int MIN_PER_KELOMPOK = 5;
    private static final String[] JENIS_DIPAKAI = {"noun", "verb", "adjective"};

    public static final Map<String, List<QuestionWord>> kelompokkan(List<WordEntry> entries) {
        Map<String, List<QuestionWord>> hasil = new LinkedHashMap<>();
        for (String jenis : JENIS_DIPAKAI) {
            hasil.put(jenis, new ArrayList<>());
        }
        Set<String> sudahAda = new HashSet<>(); //kunci = kata + jenis

        for (WordEntry entry : entries) {
            if (entry.word == null || entry.meanings == null) continue;
            String audio = cariAudio(entry);

            for (Meaning m : entry.meanings) {
                if (m.partOfSpeech == null) continue;
                String jenis = m.partOfSpeech.toLowerCase();
                List<QuestionWord> kelompok = hasil.get(jenis);
                if (kelompok == null) continue; //jenis lain diabaikan

                String kunci = entry.word.toLowerCase() + " | " + jenis;
                if (sudahAda.contains(kunci)) continue; //hindari duplikat

                QuestionWord q = buatSoal(entry.word, jenis, audio, m);
                if (q != null) {
                    kelompok.add(q);
                    sudahAda.add(kunci);
                }
            }
        }

        //buang kelompok yang jumlahnya terlalu sedikit
        Iterator<Map.Entry<String, List<QuestionWord>>> it = hasil.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().size() < MIN_PER_KELOMPOK) it.remove();
        }
        return hasil;
    }

    private static QuestionWord buatSoal(String word, String jenis, String audio, Meaning m) {
        String definisi = null;
        String contoh = null;
        String sinonim = pertama(m.synonyms);

        if (m.definitions != null) {
            for (Definition d : m.definitions) {
                if (definisi == null && ada(d.definition)) definisi = d.definition;
                if (contoh == null && ada(d.example)) contoh = d.example;
                if (sinonim == null) sinonim = pertama(d.synonyms);
            }
        }
        if (definisi == null) return null;
        if (contoh == null && sinonim == null) return null; //tanpa hint, dibuang
        return new QuestionWord(word, jenis, definisi, contoh, sinonim, audio);
    }

    private static String cariAudio(WordEntry entry) {
        if (entry.phonetics == null) return null;
        for (Phonetic p : entry.phonetics) {
            if (ada(p.audio)) return p.audio;
        }
        return null;
    }

    private static String pertama(List<String> daftar) {
        if (daftar == null) return null;
        for (String s : daftar) {
            if (ada(s)) return s;
        }
        return null;
    }

    private static boolean ada(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
