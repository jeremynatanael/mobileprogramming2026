package com.learntospell.myapplication;

public class UiState {
    public enum Status {IDLE, LOADING, SUCCESS, ERROR}

    public final Status status;
    public final WordInfo info; //terisi jika SUCCESS
    public final String message; //terisi jika ERROR

    private UiState(Status status, WordInfo info, String message) {
        this.status = status;
        this.info = info;
        this.message = message;
    }

    public static UiState idle() {
        return new UiState(Status.IDLE, null, null);
    }
    public static UiState LOADING() {
        return new UiState (Status.LOADING, null, null);
    }
    public static UiState SUCCESS(WordInfo i) {
        return new UiState(Status.SUCCESS, i, null);
    }
    public static UiState ERROR(String msg) {
        return new UiState(Status.ERROR, null, msg);
    }

}
