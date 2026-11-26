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

    public static Map<String, List<QuestionWord>> buatKosong() {
        Map<String, List<QuestionWord>> kelompok = new LinkedHashMap<>();
        for (String j : JENIS_DIPAKAI) kelompok.put(j, new ArrayList<>());
        return kelompok;
    }

    public static void tambah(WordEntry entry, Map<String, List<QuestionWord>> kelompok, Set<String> sudahAda, int target) {
        if (entry.word == null || entry.meanings == null) return;
        String audio = cariAudio(entry);

        for (Meaning m : entry.meanings) {
            if (m.partOfSpeech == null) continue;
            String jenis = m.partOfSpeech.toLowerCase();
            List<QuestionWord> daftar = kelompok.get(jenis);
            if (daftar == null || daftar.size() >= target) continue; //jenis lain diabaikan

            String kunci = entry.word.toLowerCase() + " | " + jenis;
            if (sudahAda.contains(kunci)) continue; //hindari duplikat

            QuestionWord q = buatSoal(entry.word, jenis, audio, m);
            if (q != null) {
                daftar.add(q);
                sudahAda.add(kunci);
            }
        }
    }

    public static boolean semuaPenuh(Map<String, List<QuestionWord>> kelompok, int target) {
        for (List<QuestionWord> daftar : kelompok.values()) {
            if (daftar.size() < target) return false;
        }
        return true;
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
