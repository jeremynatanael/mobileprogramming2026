package com.learntospell.myapplication;

public class WordInfo {
    public String definition;
    public String example;
    public String audioUrl;

    //Ambil data penting dari JSON. Setiap bagian bisa null (tidak error)
    public static WordInfo from (WordEntry entry) {
        WordInfo info = new WordInfo();
        if (entry.meanings != null) {
            for (Meaning m : entry.meanings) {
                if (m.definitions == null) continue;
                for (Definition d : m.definitions) {
                    if (info.definition == null && d.definition != null) info.definition = d.definition;
                    if (info.example ==  null && d.example != null) info.example = d.example;
                }
            }
        }
        if (entry.phonetics != null) {
            for (Phonetic p : entry.phonetics) {
                if (p.audio != null && p.audio.isEmpty()) {
                    info.audioUrl = p.audio;
                    break;
                }
            }
        }
        return info;
    }
}
