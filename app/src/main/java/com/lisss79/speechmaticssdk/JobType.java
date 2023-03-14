package com.lisss79.speechmaticssdk;

import java.util.Locale;

public enum JobType {

    //ALIGNMENT("alignment", "выравнивание", "alignment),
    TRANSCRIPTION("transcription", "расшифровка", "transcription");

    private final String name;
    private final String code;
    JobType(String code, String nameRu, String nameEn) {
        this.code = code;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        int length = JobType.values().length;
        String[] names = new String[length];
        for(int i = 0; i < length; i++) {
            names[i] = JobType.values()[i].getName();
        }
        return names;
    }

    public String getCode() {
        return code;
    }

    public static String[] getAllCodes() {
        int length = JobType.values().length;
        String[] codes = new String[length];
        for(int i = 0; i < length; i++) {
            codes[i] = JobType.values()[i].getCode();
        }
        return codes;
    }

    public static JobType getJobType(String code) {
        JobType jt = JobType.TRANSCRIPTION;
        for(JobType jobType: JobType.values()) {
            if(jobType.getCode().equals(code)) {
                jt = jobType;
                break;
            }
        }
        return jt;
    }

}
