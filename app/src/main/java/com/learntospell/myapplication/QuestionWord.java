package com.learntospell.myapplication;

public class QuestionWord {
    public final String word;
    public final String partOfSpeech;
    public final String definition;
    public final String example; //boleh null
    public final String synonym; //boleh null
    public final String audioUrl; //boleh null

    public QuestionWord (String word, String partOfSpeech, String definition, String example, String synonym, String audioUrl) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.definition = definition;
        this.example = example;
        this.synonym = synonym;
        this.audioUrl = audioUrl;
    }
}
