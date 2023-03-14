package com.lisss79.speechmaticssdk;

import java.util.ArrayList;

/**
 * Интерфейс с callback'ами, вызываемыми по завершении обращений к серверу
 */
public interface SpeechmaticsListener {

    /**
     * Callback после проверки авторизации AuthorizationCheck
     * @param responseCode ответ сервера, 200 - успешно
     */
    void onAuthorizationCheckFinished(int responseCode);

    /**
     * Callback после получения деталей работы GetJobDetails
     * @param responseCode код ответа сервера, 200 - успешно
     * @param jobDetails ответ сервера
     */
    void onGetJobDetailsFinished(int responseCode, JobDetails jobDetails);

    /**
     * Callback после получения деталей всех работ GetAllJobsDetails
     * @param responseCode код ответа сервера, 200 - успешно
     * @param jobDetailsList массив с ответом сервера
     */
    void onGetAllJobsDetailsFinished(int responseCode, ArrayList<JobDetails> jobDetailsList);

    /**
     * Callback после получения расшифровки GetTheTranscript
     * @param responseCode код ответа сервера, 200 - успешно
     * @param response расшифрованный текст
     */
    void onGetTheTranscriptFinished(int responseCode, String response, int requestCode);

    /**
     * Callback после получения выравнивания GetTheAlignment
     * @param responseCode код ответа сервера, 200 - успешно
     * @param response расшифрованный текст
     */
    void onGetTheAlignmentFinished(int responseCode, String response, int requestCode);

    /**
     * Callback после удаления работы DeleteJob
     * @param responseCode код ответа сервера, 200 - успешно
     * @param jobDetails карта данных удаленного элемента
     */
    void onDeleteJobFinished(int responseCode, JobDetails jobDetails);

    /**
     * Callback после получения статистики GetStatistics
     * @param responseCode ответ сервера, 200 - успешно
     * @param summaryStatistics данных статистики
     */
    void onGetStatisticsFinished(int responseCode, SummaryStatistics summaryStatistics, int requestCode);

    /**
     * Callback после создания работы SubmitJob
     * @param responseCode ответ сервера, 201 - успешно
     * @param id идентификатор созданной работы
     */
    void onSubmitJobFinished(int responseCode, String id);

    /**
     * Callback в процессе выполнения отправки работы
     * @param percent проценты выполнения
     */
    default void onJobSubmitting(int percent) {}

}
