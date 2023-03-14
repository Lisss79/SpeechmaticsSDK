package com.lisss79.speechmaticssdk;

import java.util.Locale;

public enum Diarization {
    NONE("none", "нет", "none"),
    SPEAKER("speaker", "спикеры", "speaker");

    private final String name;
    private final String code;
    Diarization(String code, String nameRu, String nameEn) {
        this.code = code;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        int length = Diarization.values().length;
        String[] names = new String[length];
        for(int i = 0; i < length; i++) {
            names[i] = Diarization.values()[i].getName();
        }
        return names;
    }

    public String getCode() {
        return code;
    }

    public static String[] getAllCodes() {
        int length = Diarization.values().length;
        String[] codes = new String[length];
        for(int i = 0; i < length; i++) {
            codes[i] = Diarization.values()[i].getCode();
        }
        return codes;
    }

    public static Diarization getDiarization(String code) {
        Diarization diar = Diarization.NONE;
        for(Diarization diarization: Diarization.values()) {
            if(diarization.getCode().equals(code)) {
                diar = diarization;
                break;
            }
        }
        return diar;
    }

}
