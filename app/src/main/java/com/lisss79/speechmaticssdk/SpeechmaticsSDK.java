package com.lisss79.speechmaticssdk;

import static com.lisss79.speechmaticssdk.JsonKeysValues.ID;
import static com.lisss79.speechmaticssdk.JsonKeysValues.JOBS;
import static com.lisss79.speechmaticssdk.JsonKeysValues.SUMMARY;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Основной класс для взаимодействия с API сервера Speechmatics
 */
public class SpeechmaticsSDK {

    // Идентификаторы задач для handler'а
    private final int CHECK_AUTHORIZATION = 10;
    private final int GET_ALL_JOBS_DETAILS = 11;
    private final int GET_JOB_DETAILS = 12;
    private final int DELETE_JOB = 13;
    private static final int GET_STATISTICS = 14;
    private static final int GET_THE_TRANSCRIPT = 15;
    private static final int GET_THE_ALIGNMENT = 16;
    private static final int SUBMIT_JOB = 17;
    private static final int JOB_SUBMITTING = 18;

    // Константы для http запросов
    public final static int NO_DATA = -1;
    private final String baseUrl = "https://asr.api.speechmatics.com/v2/jobs";
    private final String statUrl = "https://asr.api.speechmatics.com/v2/usage";
    private final String TWO_HYPHENS = "--";
    private final String CR = System.lineSeparator();
    private static String AUTH_TOKEN = "";
    private final static String GET = "GET";
    private final static String DEL = "DELETE";
    private final static int TIMEOUT = 5000;

    // Значения по умолчанию для параметров, публичные
    public final static Language defLanguage = Language.RU;
    public final static Diarization defDiarization = Diarization.NONE;
    public final static OperatingPoint defOperatingPoint = OperatingPoint.ENHANCED;
    public final static JobType defJobType = JobType.TRANSCRIPTION;

    // Ключи для форматирования ответов
    private final static String DATE_ISO8601_PATTERN = "yyyy-MM-dd";
    private final static String USER_CREATED_PATTERN = "d MMM yy, HH:mm:ss";
    private static String USER_DURATION_PATTERN_SECOND;
    private static String USER_DURATION_PATTERN_MINUTE;
    private static String USER_DURATION_PATTERN_HOUR;
    private static String USER_DURATION_HOUR_PATTERN_HOUR;
    private static String USER_DURATION_HOUR_PATTERN_DAY;

    // Приватные глобальные переменные для работы sdk
    private HttpURLConnection connection;
    private final ExecutorService service;
    private final Handler uiHandler;
    private final SpeechmaticsListener listener;
    private final Context context;

    //Публичные глобальные переменные - данные аудиофайла и работы
    public String fileName = "";
    public int fileSize = NO_DATA;
    public String jobId = "";
    public JobType jobType = defJobType;
    public JobStatus jobStatus = JobStatus.NONE;
    public FileStatus fileStatus = FileStatus.NOT_SELECTED;
    public int duration = 0;

    // Поддерживаемые расширения файлов
    private final String[] supportedExtension =
            {"wav", "mp3", "aac", "ogg", "mpeg", "amr", "m4a", "mp3", "flac"};
    // Максимальная длина файла
    private final int maxFileSize = 1024 * 1024 * 1024;

    /**
     * Пустой конструктор для тестов
     */
    public SpeechmaticsSDK() {
        service = null;
        listener = null;
        context = null;
        uiHandler = null;
    }

    /**
     * Конструктор
     * @param auth_token токен для авторизации
     * @param listener интерфейс для получения ответов
     */
    public SpeechmaticsSDK(@NonNull Context context,
                           String auth_token, @NonNull SpeechmaticsListener listener) {
        this.context = context;
        AUTH_TOKEN = auth_token;
        this.listener = listener;
        service = Executors.newCachedThreadPool();
        uiHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                sendToListener(msg);
            }
        };
    }

    private void sendToListener(@NonNull Message msg) {
        int task = msg.arg1;
        switch(task) {
            case CHECK_AUTHORIZATION:
                listener.onAuthorizationCheckFinished(msg.what);
                break;
            case GET_ALL_JOBS_DETAILS:
                listener.onGetAllJobsDetailsFinished(msg.what, (ArrayList<JobDetails>) msg.obj);
                break;
            case GET_JOB_DETAILS:
                listener.onGetJobDetailsFinished(msg.what, (JobDetails) msg.obj);
                break;
            case DELETE_JOB:
                listener.onDeleteJobFinished(msg.what, (JobDetails) msg.obj);
                break;
            case GET_STATISTICS:
                listener.onGetStatisticsFinished(msg.what, (SummaryStatistics) msg.obj, msg.arg2);
                break;
            case GET_THE_TRANSCRIPT:
                listener.onGetTheTranscriptFinished(msg.what, (String) msg.obj, msg.arg2);
                break;
            case GET_THE_ALIGNMENT:
                listener.onGetTheAlignmentFinished(msg.what, (String) msg.obj, msg.arg2);
                break;
            case SUBMIT_JOB:
                listener.onSubmitJobFinished(msg.what, (String) msg.obj);
                break;
            case JOB_SUBMITTING:
                listener.onJobSubmitting(msg.arg2);
                break;
            default:
                break;
        }

    }

    /**
     * Проверка корректности авторизации.
     * По окончании - вызов onAuthorizationCheckFinished.
     */
    public void checkAuthorization() {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = CHECK_AUTHORIZATION;
            int responseCode = doQuery(baseUrl, GET);
            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Отправка текущего файла на сервер для расшифровки:
     * выполнение запроса POST и отправление данных из файла.
     * По окончании - вызов onSubmitJobFinished.
     * @param uri ссылка на файл с данными
     * @param jobConfig конфигурация работы
     */
    public void submitJob(@Nullable Uri uri, @Nullable JobConfig jobConfig) {

        // Создание конфигурации по умолчанию в случае null или использвание заданной
        JobConfig jc;
        if(jobConfig != null) jc = jobConfig;
        else jc = new JobConfig();

        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = SUBMIT_JOB;
            Message submittingMsg;
            int responseCode = NO_DATA;
            String data;
            int nextByte;

            // Случайный разделитель разделов тела запроса POST
            String boundary = UUID.randomUUID().toString();

            if(setFile(uri)) {

                // Входящий поток из Uri
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    URL requestURL = new URL(baseUrl);

                    // Начало процесса, 0%
                    submittingMsg = getProgressMessage(0);
                    uiHandler.sendMessage(submittingMsg);

                    connection = (HttpURLConnection) requestURL.openConnection();
                    connection.setReadTimeout(TIMEOUT);
                    connection.setConnectTimeout(TIMEOUT);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
                    // Указание типа данных и разделителя
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    // Получение исходящего потока для отправления тела запроса
                    connection.setDoOutput(true);
                    DataOutputStream os = new DataOutputStream(connection.getOutputStream());

                    // Начало тела запроса
                    os.writeBytes(TWO_HYPHENS + boundary + CR);
                    // Заголовок раздела с файлом. Сюда вписываем имя файла
                    String con_dis_file = String.format
                            ("Content-Disposition: form-data; name=\"data_file\"; filename=\"%s\"", fileName);
                    String con_type_file = "Content-Type: application/octet-stream";
                    os.writeBytes(con_dis_file + CR);
                    os.writeBytes(con_type_file + CR + CR);

                    // Считывааем байт из входящего потока (файл) и записываем в исходящий (тело http запроса)
                    int i = 0;
                    int j = 0;
                    int step = fileSize / 9;
                    do {
                        nextByte = is.read();
                        if(nextByte != -1) os.write(nextByte);
                        if(i % step == 0) {
                            /// Текущее значение прогресса
                            submittingMsg = getProgressMessage(j);
                            uiHandler.sendMessage(submittingMsg);
                            j += 10;
                        }
                        i++;
                    } while (nextByte != -1);

                    os.writeBytes(CR);

                    // Заголовок раздела когнфигурации
                    os.writeBytes(TWO_HYPHENS + boundary + CR);
                    String con_dis_config = "Content-Disposition: form-data; name=\"config\"";
                    String con_config = jc.toString();

                    os.writeBytes(con_dis_config + CR + CR);
                    os.writeBytes(con_config + CR);
                    os.writeBytes(TWO_HYPHENS + boundary + TWO_HYPHENS + CR);
                    // Конец тела запроса
                    os.flush();

                    // Ожидаем ответ сервера
                    submittingMsg = getProgressMessage(99);
                    uiHandler.sendMessage(submittingMsg);
                    connection.connect();
                    responseCode = connection.getResponseCode();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String response = getServerResponse(responseCode);
                if(responseCode == HttpURLConnection.HTTP_CREATED) {
                    data = getValue(response, ID);
                    jobId = data;
                    jobType = jc.getJobType();
                    msg.obj = data;
                    jobStatus = JobStatus.RUNNING;
                    fileStatus = FileStatus.SENT;
                } else {
                    fileStatus = FileStatus.SENDING_ERROR;
                    msg.obj = response;
                }
            }

            // Окончание процесса, 100%
            submittingMsg = getProgressMessage(100);
            uiHandler.sendMessage(submittingMsg);

            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });
    }

    private Message getProgressMessage(int percent) {
        Message message = new Message();
        message.arg1 = JOB_SUBMITTING;
        message.arg2 = percent;
        return message;
    }

    /**
     * Удаление работы с сервера.
     * По окончании - вызов onDeleteJobFinished.
     * @param id идентификатор работы
     */
    public void deleteJob(@NonNull String id) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = DELETE_JOB;
            JobDetails jobDetails;
            String urlWithId = baseUrl + "/" + id;
            int responseCode = doQuery(urlWithId, DEL);
            String response = getServerResponse(responseCode);
            msg.what = responseCode;
            if(responseCode == HttpURLConnection.HTTP_OK) {
                jobDetails = new JobDetails(response);
                msg.obj = jobDetails;
            }
            uiHandler.sendMessage(msg);
        });
    }


    /**
     * Получение деталей работы с сервера.
     * По окончании - вызов onGetJobDetailsFinished.
     * @param id идентификатор работы
     */
    public void getJobDetails(@NonNull String id) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_JOB_DETAILS;
            JobDetails jobDetails;
            int responseCode;
            String response = "";
            if(!id.isEmpty()) {
                String urlWithId = baseUrl + "/" + id;
                responseCode = doQuery(urlWithId, GET);
                response = getServerResponse(responseCode);
            } else responseCode = HttpURLConnection.HTTP_NOT_FOUND;
            msg.what = responseCode;
            if(responseCode == HttpURLConnection.HTTP_OK) {
                jobDetails = new JobDetails(response);
                msg.obj = jobDetails;
                if(id.equals(jobId)) {
                    jobStatus = jobDetails.getStatus();
                    jobType = jobDetails.getJobConfig().getJobType();
                }
            }
            if(responseCode == NO_DATA && id.equals(jobId)) jobStatus = JobStatus.UNKNOWN;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получения деталей всех работ с сервера.
     * По окончании - вызов onGetAllJobsDetailsFinished.
     */
    public void getAllJobsDetails(boolean includeDeleted) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_ALL_JOBS_DETAILS;
            JobDetails jobDetails;
            ArrayList<JobDetails> jobDetailsList = new ArrayList<>();
            String urlAllJobs = baseUrl + "?include_deleted=" + includeDeleted + "&limit=100";
            int responseCode = doQuery(urlAllJobs, GET);
            msg.what = responseCode;
            String response = getServerResponse(responseCode);
            if(responseCode == HttpURLConnection.HTTP_OK) {
                JSONArray jsonArray;
                try {
                    jsonArray = new JSONObject(response).getJSONArray(JOBS);

                    // Перебираем все работы в массиве и добавляем в список
                    for(int i = 0;  i < jsonArray.length(); i++) {
                        jobDetails = new JobDetails(jsonArray.getJSONObject(i));
                        jobDetailsList.add(jobDetails);
                    }
                    msg.obj = jobDetailsList;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(urlAllJobs);
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получение общей статистики.
     * По окончании - вызов onGetStatisticsFinished.
     * @param monthly true - за месяц, false - за весь срок
     */
    public void getStatistics(boolean monthly, int requestCode) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_STATISTICS;
            int count = 0;
            float duration_hours = 0;
            String url = statUrl;
            if (monthly) {
                Calendar date = Calendar.getInstance();
                date.set(Calendar.DAY_OF_MONTH, 1);
                String formattedDate = new SimpleDateFormat(DATE_ISO8601_PATTERN)
                        .format(date.getTime());
                url = statUrl + "?since=" + formattedDate;
            }
            int responseCode = doQuery(url, GET);
            msg.what = responseCode;
            String response = getServerResponse(responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONArray jsonArray;
                JSONObject jsonObject;
                SummaryStatistics summaryStatistics;
                try {
                    jsonArray = new JSONObject(response).getJSONArray(SUMMARY);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        jsonObject = jsonArray.getJSONObject(i);
                        summaryStatistics = new SummaryStatistics(jsonObject);
                        JobType type = summaryStatistics.getType();
                        if (type.equals(JobType.TRANSCRIPTION)) {
                            count += summaryStatistics.getCount();
                            duration_hours += summaryStatistics.getDuration_hrs();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            SummaryStatistics data = new SummaryStatistics();
            data.setType(JobType.TRANSCRIPTION);
            data.setCount(count);
            data.setDuration_hrs(duration_hours);
            msg.obj = data;
            msg.arg2 = requestCode;
            uiHandler.sendMessage(msg);
            //listener.onGetStatisticsFinished(responseCode, data);
        });
    }

    /**
     * Получение расшифровки с сервера.
     * По окончании - вызов onGetTheTranscriptFinished.
     * @param id идентификатор работы
     * @param requestCode код запроса, передается в ответ для идентификации
     */
    public void getTheTranscript(@NonNull String id, int requestCode) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_THE_TRANSCRIPT;
            String urlForTranscript = baseUrl + "/" + id + "/transcript?format=txt";
            int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
            String response = "";
            if(!id.isEmpty()) {
                responseCode = doQuery(urlForTranscript, GET);
                response = getServerResponse(responseCode);
            }
            msg.what = responseCode;
            msg.obj = response;
            msg.arg2 = requestCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получение выравнивания с сервера.
     * По окончании - вызов onGetTheAlignmentFinished.
     * @param id идентификатор работы
     * @param requestCode код запроса, передается в ответ для идентификации
     */
    public void getTheAlignment(@NonNull String id, int requestCode) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_THE_ALIGNMENT;
            String urlForTranscript = baseUrl + "/" + id + "/alignment?tags=one_per_line";
            int responseCode = doQuery(urlForTranscript, GET);
            msg.what = responseCode;
            String response = getServerResponse(responseCode);
            msg.obj = response;
            msg.arg2 = requestCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получение значения ключа из JSON
     * @param jsonString
     * @param key
     * @return
     */
    private String getValue(String jsonString, String key) {
        JSONObject jsonObject;
        String value = "";
        try {
            jsonObject = new JSONObject(jsonString);
            value = jsonObject.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * Выполняет запрос
     * @param url адрес для запроса
     * @param query - запрос (GET, DELETE)
     * @return код ответа
     */
    private int doQuery(String url, String query) {
        int responseCode = NO_DATA;
        try {
            URL requestURL = new URL(url);
            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestMethod(query);
            connection.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseCode;
    }

    /**
     * Возвращает ответ сервера
     * @param responseCode код ответа от сервера
     * @return ответ сервера
     */
    @NonNull
    private String getServerResponse(int responseCode) {
        String response = "";

        // Если код ответ сервера CREATED (для POST) или OK (для остальных), получить ответ
        if(responseCode == HttpURLConnection.HTTP_OK ||
                responseCode == HttpURLConnection.HTTP_CREATED) {
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseSB = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseSB.append(responseLine.trim()).append(CR);
                }
                if (responseSB.length() != 0) {
                    response = responseSB.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Если другой код ответ сервера - получить текст ошибки
        else if(responseCode != NO_DATA && connection != null) {
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseSB = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseSB.append(responseLine.trim()).append(CR);
                }
                if (responseSB.length() != 0) {
                    response = responseSB.toString();
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    /**
     * Сохранение текста расшифровки в файл
     * @param uri ссылка на содержимое файла
     * @param text текст, который пишется в файл
     * @return успешно ли записан текст в файл
     */
    public boolean saveFile(@Nullable Uri uri, @Nullable String text) {
        boolean result = false;

        // Проверка на пустой или null Uri
        if(uri == null) return false;
        if(uri.toString().isEmpty()) return false;

        // Проверка на тип файла
        String mimeType = context.getContentResolver().getType(uri);
        if(!mimeType.startsWith("text") || text == null) {
            return false;
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
            byte[] tempBytesAudio = text.getBytes(StandardCharsets.UTF_16);
            int tempFileSize = tempBytesAudio.length;
            os.write(tempBytesAudio, 0, tempFileSize);
            os.flush();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Сохранение текста расшифровки в буфер обмена
     * @param text текст, который пишется в файл
     */
    public void copyToClipboard(String text) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Передача файла для дальнейшей обработки и проверка на корректность
     * @param uri ссылка на содержимое файла
     * @return успешно ли прочитаны данные из файла
     */
    public boolean setFile(@Nullable Uri uri) {
        fileSize = 0;

        // Проверка на пустой или null Uri
        if(uri == null) return false;
        if(uri.toString().isEmpty()) return false;

        // Проверка на тип файла
        String mimeType = context.getContentResolver().getType(uri);
        if(!mimeType.startsWith("audio") && !mimeType.startsWith("video")) {
            fileName = uri.getLastPathSegment();
            fileStatus = FileStatus.WRONG_FILE_TYPE;
            return false;
        }

        try {

            // Получение имени и длинны файла из Uri
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
                cursor.moveToFirst();
                fileName = cursor.getString(nameIndex);
                long tempFileSizeL = cursor.getLong(sizeIndex);

                // Проверка на поддерживаемое расширение и длинну файла
                boolean ok = false;
                if(tempFileSizeL < maxFileSize) {
                    ok = true;
                    fileSize = (int) tempFileSizeL;
                }
                String ext = fileName.
                        substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                for(String e: supportedExtension) {
                    if (ext.equals(e)) {
                        ok = true;
                        break;
                    }
                }
                if(!ok) {
                    fileStatus = FileStatus.WRONG_FILE_TYPE;
                    return false;
                }

            }
            // Если не удалось прочитать данные из Uri
            catch (SecurityException e) {
                e.printStackTrace();
                fileName = uri.getLastPathSegment();
                fileStatus = FileStatus.LOADING_ERROR;
                return false;
            }

            // Получение данных из файла в массив, если данные доступны
            if(fileSize > 0) {

                // Получение продолжительности аудиофайла (если тип не подходит - ошибка)
                String durationStr = "";
                try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
                    mmr.setDataSource(context, uri);
                    durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    try {
                        duration = Integer.parseInt(durationStr) / 1000;
                        fileStatus = FileStatus.SELECTED;
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                        duration = 0;
                        fileStatus = FileStatus.LOADING_ERROR;
                        return false;
                    }
                } catch (RuntimeException | IOException e) {
                    e.printStackTrace();
                    duration = 0;
                    fileStatus = FileStatus.LOADING_ERROR;
                    return false;
                }

            } else {
                duration = 0;
                fileStatus = FileStatus.LOADING_ERROR;
                return false;
            }
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            fileStatus = FileStatus.LOADING_ERROR;
            return false;
        }
        fileStatus = FileStatus.SELECTED;
        return true;
    }

    /**
     * Инициализация значений ключей для статических методов в зависимости от языка
     */
    private static void initKeys() {
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        String DAY_SYMBOL;
        String HOUR_SYMBOL;
        String MINUTE_SYMBOL;
        String SECOND_SYMBOL;
        if (langRu) {
            DAY_SYMBOL = "дн";
            HOUR_SYMBOL = "ч";
            MINUTE_SYMBOL = "м";
            SECOND_SYMBOL = "с";
        } else {
            DAY_SYMBOL = "d";
            HOUR_SYMBOL = "h";
            MINUTE_SYMBOL = "m";
            SECOND_SYMBOL = "s";
        }
        USER_DURATION_PATTERN_SECOND = "%02d" + SECOND_SYMBOL;
        USER_DURATION_PATTERN_MINUTE = "%02d" + MINUTE_SYMBOL + " " + USER_DURATION_PATTERN_SECOND;
        USER_DURATION_PATTERN_HOUR = "%d" + HOUR_SYMBOL + " " + USER_DURATION_PATTERN_MINUTE;
        USER_DURATION_HOUR_PATTERN_HOUR = "%02d" + HOUR_SYMBOL + " " + USER_DURATION_PATTERN_MINUTE;
        USER_DURATION_HOUR_PATTERN_DAY = "%d" + DAY_SYMBOL + " " + USER_DURATION_HOUR_PATTERN_HOUR;
    }

    /**
     * Преобразует полученное от сервера значение общей длительности записей в часах
     * в удобный формат
     * @param durationHrs ответ от сервера в текстовом формате
     * @return строка, удобная для чтения пользователем
     */
    @SuppressLint("DefaultLocale")
    public static String durationHoursToString(String durationHrs) {
        initKeys();
        String result = durationHrs != null ? durationHrs : "";
        try {
            Objects.requireNonNull(durationHrs);
            float dur = Float.parseFloat(durationHrs) * 3600;
            if(dur < 0) dur = 0;
            int d = (int) Math.floor(dur / 86400);
            int h = (int) Math.floor((dur - d * 86400) / 3600);
            int m = (int) (Math.floor((dur - d * 86400 - h * 3600) / 60));
            int s = (int) (dur - d * 86400 - h * 3600 - m * 60);
            if(dur < 60) result = String.format(USER_DURATION_PATTERN_SECOND, s);
            else if(dur < 3600) result = String.format(USER_DURATION_PATTERN_MINUTE, m, s);
            else if (dur < 86400) result = String.format(USER_DURATION_HOUR_PATTERN_HOUR, h, m, s);
            else result = String.format(USER_DURATION_HOUR_PATTERN_DAY, d, h, m, s);
        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Преобразует полученное от сервера значение длительности записи c секундах
     * в удобный формат
     * @param duration ответ от сервера в текстовом формате
     * @return строка, удобная для чтения пользователем
     */
    @SuppressLint("DefaultLocale")
    public static String durationToString(String duration) {
        initKeys();
        String result = duration != null ? duration : "";
        try {
            Objects.requireNonNull(duration);
            float dur = Float.parseFloat(duration);
            if(dur < 0) dur = 0;
            int h = (int) Math.floor(dur / 3600);
            int m = (int) (Math.floor((dur - h * 3600) / 60));
            int s = (int) (dur - h * 3600 - m * 60);
            if(dur < 60) result = String.format(USER_DURATION_PATTERN_SECOND, s);
            else if(dur < 3600) result = String.format(USER_DURATION_PATTERN_MINUTE, m, s);
            else result = String.format(USER_DURATION_PATTERN_HOUR, h, m, s);
        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Преобразует полученное от сервера значение даты/времени создания записи
     * в удобный формат
     * @param createdAt ответ от сервера в текстовом формате
     * @param locale локаль для формирования даты
     * @return строка, удобная для чтения пользователем
     */
    public static String createdToString(String createdAt, Locale locale) {
        initKeys();
        String result = createdAt != null ? createdAt : "";
        try {
            Objects.requireNonNull(createdAt);
            Instant instant = Instant.parse(createdAt);
            Date date = Date.from(instant);
            result = new SimpleDateFormat(USER_CREATED_PATTERN, locale).format(date);
        } catch (DateTimeException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }

}
