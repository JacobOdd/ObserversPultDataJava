import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable {

    @FXML
    public Label labelHost;

    @FXML
    public TextField tfHost;

    @FXML
    public ImageView indicatorImage;

    @FXML
    public Button btnOnIndicator;

    @FXML
    public Button btnOffIndicator;

    @FXML
    public Label labelStatus;

    @FXML
    public Label labelRequestSizeAll;

    @FXML
    public Label labelRequestSizeSuccess;

    @FXML
    public Label labelTimer;

    @FXML
    public TextField tfPHP;

    @FXML
    public Label labelPHP;

    @FXML
    public Label label_no_change;

    @FXML
    public Button btnHost;

    @FXML
    public Label labelStatusDeleteScript;

    // CONST STRING
    private final String TXT_WORKING = "Working";
    private final String TXT_ERROR = "Error";
    private final String TXT_STATUS_SCRIPT_DELETE = "The status of the 'delete' script:";
    private final String TXT_WAIT = "Wait";
    private final String TXT_SECONDS = "seconds";
    private final String TXT_OFF = "Turned off";
    private final String TXT_NO_HOST = "No host";
    private final String TXT_NO_PHP_FILE_SELECTED = "No php file selected";
    private final String TXT_SECONDS_NEXT_REQUEST = "Seconds to the next. request:";
    private final String TXT_LOADING = "loading...";
    private final String TXT_REQUEST_TOTAL_ZA_SESSION = "Total requests per session:";
    private final String TXT_REQUEST_SUCCESS = "Success request:";
    private final String TXT_ERROR_QUOTA_EXPIRED = "Error: Quota expired SOS";
    private final String TXT_ERROR_WEAK_NETWORK = "Error: Weak internet connection or not at all";
    private final String TXT_ERROR_NOT_RIGHT_URL_HOST = "Error: Wrong URL-Host SOS";
    private final String TXT_ERROR_APACHE = "Error: Apache server is not responding or weak internet connection SOS";
    private final String TXT_ERROR_NOT_WORKING_QUOTA_OR_SERVER = "Error: Doesn't work (quota expired or click 'enable')";
    private final String TXT_ERROR_NOT_WORKING_REFRESH_BUTTON = "Error: Doesn't work (click 'enable')";
    private final String TXT_NETWORK_ERROR = "Error: Network Error";
    private final String TXT_ERROR_NOT_RIGHT_NAME_FILE = "Error: No such file:";

    private boolean isIndicator = false;

    private int sizeRequestSuccess = 0;
    private int sizeRequestAll = 0;

    private Dialog<ButtonType> dialog = null;
    private final String FILE_NAME_HOST = "settingsHOST.txt";
    private final String FILE_NAME_PHP = "settingsPHP.txt";

    private ScheduledExecutorService run_db_executor = null;
    private ScheduledExecutorService run_delete_old_document = null;

    private final int RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE = 10;

    private final AtomicInteger RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST = new AtomicInteger(RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE);
    private final Timeline RUN_DB_EXECUTOR_TIMELINE_PERIOD_REQUEST = new Timeline(
            new KeyFrame(
                    Duration.millis(1000), //1000 мс
                    ae -> {
                        RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST.getAndDecrement();
                        Platform.runLater(() -> {
                            if(RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST.get() <= 1) {
                                labelTimer.setText(TXT_SECONDS_NEXT_REQUEST + " " + TXT_LOADING + " " + TXT_WAIT);
                            } else {
                                labelTimer.setText(TXT_SECONDS_NEXT_REQUEST + " " + RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST);
                            }
                        });
                    }
            )
    );

    private final int RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE = 120;

    private final AtomicInteger RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST = new AtomicInteger(RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE);
    private final Timeline RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST = new Timeline(
            new KeyFrame(
                    Duration.millis(1000), //1000 мс
                    ae -> {
                        RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST.getAndDecrement();
                        Platform.runLater(() -> {
                            if(RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST.get() <= 1) {
                                labelStatusDeleteScript.setText(TXT_STATUS_SCRIPT_DELETE + " " + TXT_LOADING);
                            } else {
                                labelStatusDeleteScript.setText(TXT_STATUS_SCRIPT_DELETE + " " + TXT_WAIT + " " + RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST + " " + TXT_SECONDS);
                            }
                        });
                    }
            )
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeUI();
        readSettingsHOST();
        readSettingsPHP();
    }

    @FXML
    public void saveSettingsHOST() {
        saveSettingsToFile(tfHost,FILE_NAME_HOST);
        readSettingsHOST();
    }

    @FXML
    public void saveSettingsPHP() {
        saveSettingsToFile(tfPHP,FILE_NAME_PHP);
        readSettingsPHP();
    }

    @FXML
    public void onIndicator() {
        if(isIndicator) {
            return;
        }
        run_db_executor = Executors.newSingleThreadScheduledExecutor();

        setIsIndicator(true,false);
        labelStatus.setText(TXT_WAIT + " " +RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE + " " + TXT_SECONDS);
        onTimerStart(false);

        Runnable periodTask = () -> {
            HttpURLConnection conn = null;
            try {
                sizeRequestAll++;

                Platform.runLater(()-> labelRequestSizeAll.setText(TXT_REQUEST_TOTAL_ZA_SESSION + " " + sizeRequestAll));

                URL url = new URL("http://"+labelHost.getText()+"/"+labelPHP.getText());
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setReadTimeout(19000);
                conn.setConnectTimeout(19000);
                conn.setRequestMethod("POST");

                OutputStreamWriter writer = new OutputStreamWriter(
                        conn.getOutputStream());

                String phpPOST = URLEncoder.encode("200", "UTF-8");
                writer.write("values=" + phpPOST);
                writer.close();

                onTimerStart(true);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String str = responseData(conn);
                    final Pattern p200 = Pattern.compile("\"http_code\":\"200\"");
                    final Matcher m200 = p200.matcher(str);
                    final Pattern p500 = Pattern.compile("\"http_code\":\"500\"");
                    final Matcher m500 = p500.matcher(str);

                    if(m200.find()) {
                        System.out.println(conn.getResponseCode());
                        System.out.println("VIPOLNIS URL: " + conn.getURL().toString());
                        RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST.play();
                        onRequestSuccess();
                    } else if(m500.find()) {
                        onRequestError(TXT_ERROR_QUOTA_EXPIRED);
                    } else {
                        onRequestError(TXT_ERROR_WEAK_NETWORK);
                    }
                } else {
                    onRequestCode(conn.getResponseCode());
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                onRequestError(TXT_ERROR_NOT_RIGHT_URL_HOST);
            } catch (Exception e) {
                e.printStackTrace();
                onRequestError(TXT_ERROR_APACHE);
            } finally {
                if(conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ex) {
                       ex.printStackTrace();
                    }
                }
            }
        };
        run_db_executor.scheduleAtFixedRate(periodTask, RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE, RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE, TimeUnit.SECONDS);

        run_delete_old_document = Executors.newSingleThreadScheduledExecutor();
        labelStatusDeleteScript.setText(TXT_STATUS_SCRIPT_DELETE + " " + TXT_WAIT + " " + RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST + " " + TXT_SECONDS);
        onTimeStartDelete(false);

        Runnable periodTaskDelete = () -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + labelHost.getText() + "/run_delete_old_document.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("POST");

                OutputStreamWriter writer = new OutputStreamWriter(
                        conn.getOutputStream());

                String phpPOST = URLEncoder.encode("200", "UTF-8");
                writer.write("values=" + phpPOST);
                writer.close();

                onTimeStartDelete(true);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String str = responseData(conn);
                    final Pattern p200 = Pattern.compile("\"http_code\":\"200\"");
                    final Matcher m200 = p200.matcher(str);
                    final Pattern p500 = Pattern.compile("\"http_code\":\"500\"");
                    final Matcher m500 = p500.matcher(str);

                    if(m200.find()) {
                        Platform.runLater(() -> labelStatusDeleteScript.setText(TXT_STATUS_SCRIPT_DELETE + " " + TXT_WORKING));
                    } else if(m500.find()) {
                        Platform.runLater(() -> labelStatusDeleteScript.setText(TXT_ERROR_QUOTA_EXPIRED));
                    } else {
                        Platform.runLater(() -> labelStatusDeleteScript.setText(TXT_ERROR_WEAK_NETWORK));
                    }
                } else {
                    onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_ERROR_NOT_WORKING_QUOTA_OR_SERVER);
                    if(run_delete_old_document != null) {
                        run_delete_old_document.shutdownNow();
                        run_delete_old_document = null;
                    }
                }
                //System.out.println("VIPOLNIS ZAPOSIK UDALITY");
            } catch (Exception e) {
                e.printStackTrace();
               // onTimeStopDelete("Статус скрипта 'удаления': Не работает.(закончилась квота или повторите нажатие кнопки включить)");
                onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_ERROR_NOT_WORKING_REFRESH_BUTTON);
                if(run_delete_old_document != null) {
                    run_delete_old_document.shutdownNow();
                    run_delete_old_document = null;
                }
            } finally {
                if(conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        run_delete_old_document.scheduleAtFixedRate(periodTaskDelete,
                RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE,
                RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE,
                TimeUnit.SECONDS
        );
    }

    @FXML
    public void offIndicator() {
        if(!isIndicator) {
            return;
        }

        setIsIndicator(false,false);

        labelStatus.setText(TXT_OFF);
        try {
            onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_OFF);
            onTimerStop();
            if(run_db_executor != null) {
                run_db_executor.shutdownNow();
                run_db_executor = null;
            }
            if(run_delete_old_document != null) {
                run_delete_old_document.shutdownNow();
                run_delete_old_document = null;
            }
        }catch (Exception e) {
            onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_OFF);
            onTimerStop();
            if(run_db_executor != null) {
                run_db_executor.shutdownNow();
                run_db_executor = null;
            }
            if(run_delete_old_document != null) {
                run_delete_old_document.shutdownNow();
                run_delete_old_document = null;
            }
            e.printStackTrace();
        }
    }

    private void readSettingsHOST() {
        readSettingsFile(labelHost,tfHost,FILE_NAME_HOST, TXT_NO_HOST);
    }

    private void readSettingsPHP() {
        readSettingsFile(labelPHP,tfPHP,FILE_NAME_PHP,TXT_NO_PHP_FILE_SELECTED);
    }

    private void readSettingsFile(Label label, TextField textField, String FILE_NAME, String TXT_ERROR) {
        try {
            String result = "";
            String [] splitted;
            Scanner sc = new Scanner(new File(FILE_NAME));
            if (sc.hasNext()) {
                do {
                    splitted = sc.nextLine().split(" ");
                    result = splitted[0];
                    break;
                } while (sc.hasNext());
            }
            if(result.isEmpty()) {
                label.setText(TXT_ERROR);
            } else {
                label.setText(result);
                textField.setText(result);
            }
            sc.close();
           // inputStream.close();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSettingsToFile(TextField tfField,String FILE_NAME) {
        try {
            File folder = new File(FILE_NAME);
            FileWriter writer = new FileWriter(folder, false);
            writer.write(tfField.getText());
            writer.flush();
            writer.close();
            System.out.println(FILE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        GridPane.setMargin(indicatorImage, new Insets(10,0,0,0));
        GridPane.setMargin(btnOnIndicator, new Insets(5,0,0,0));
        GridPane.setMargin(btnOffIndicator, new Insets(10,0,10,0));
        GridPane.setMargin(label_no_change,new Insets(0,0,0,0));
        GridPane.setMargin(labelHost,new Insets(0,0,0,5));
        GridPane.setMargin(btnHost,new Insets(0,0,10,0));
        labelTimer.setFont(Font.getDefault());

        labelTimer.setText(TXT_SECONDS_NEXT_REQUEST + " " + RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST);
        labelHost.setMaxWidth(200);
        labelPHP.setMaxWidth(200);
        tfHost.setPrefWidth(300);
        tfHost.setPrefHeight(50);
        tfHost.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        tfPHP.setPrefWidth(300);
        tfPHP.setPrefHeight(50);
        tfPHP.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        setIsIndicator(false,false);
    }

    private void setIsIndicator(boolean isIndicator,boolean isOnIndicatorImage) {
        this.isIndicator = isIndicator;
        btnOnIndicator.setDisable(isIndicator);
        btnOffIndicator.setDisable(!isIndicator);
        if(isOnIndicatorImage) {
            indicatorImage.setImage(new Image(Main.class.getResourceAsStream("on_indicator.png")));
        } else {
            indicatorImage.setImage(new Image(Main.class.getResourceAsStream("off_indicator.png")));
        }
    }

    private void onRequestSuccess() {
        Platform.runLater(()->{
            sizeRequestSuccess++;
            labelRequestSizeSuccess.setText(TXT_REQUEST_SUCCESS + " " + sizeRequestSuccess);
            labelStatus.setText(TXT_WORKING);
            setIsIndicator(true,true);
        });
    }


    private void onRequestError(String s) {
        Platform.runLater(()-> {
            setIsIndicator(false,false);
            if(run_db_executor != null) {
                run_db_executor.shutdownNow();
                run_db_executor = null;
            }
            if(run_delete_old_document != null) {
                run_delete_old_document.shutdownNow();
                run_delete_old_document = null;
            }
            onTimerStop();
            onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_OFF);

            labelStatus.setText(s);
            onMediaOnDialog(s);
        });
    }

    private void onRequestCode(int responseCode) {
        Platform.runLater(()-> {
            setIsIndicator(false,false);
            if(run_db_executor != null) {
                run_db_executor.shutdownNow();
                run_db_executor = null;
            }
            if(run_delete_old_document != null) {
                run_delete_old_document.shutdownNow();
                run_delete_old_document = null;
            }
            onTimerStop();
            onTimeStopDelete(TXT_STATUS_SCRIPT_DELETE + " " + TXT_OFF);

            String str;
            if(responseCode == 404) {
                labelStatus.setText(TXT_NETWORK_ERROR + ": " + TXT_ERROR_NOT_RIGHT_NAME_FILE + " " + responseCode);
                str =  TXT_NETWORK_ERROR + " " + TXT_ERROR_NOT_RIGHT_NAME_FILE + " " +  responseCode;
            } else {
                labelStatus.setText(TXT_NETWORK_ERROR + ": " + responseCode);
                str = TXT_NETWORK_ERROR + ": "  + responseCode;
            }

            onMediaOnDialog(str);
        });
    }



    private void onMediaOnDialog(String str) {
        String mediaURL = getClass().getResource("/sirena.mp3").toExternalForm();
        System.out.println(mediaURL);

        Media media = new Media(mediaURL);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();

        dialog = dialogBuild(str);
        Optional<ButtonType> buttonType = dialog.showAndWait();

        buttonType.ifPresent(type -> {
            if(type.getText().equals("Ok")) {
                mediaPlayer.stop();
                dialogClose();
            }
        });
    }

    private void dialogClose() {
        if(dialog != null)
            dialog.close();
    }

    private void onTimeStartDelete(boolean isNotStart) {
        if(isNotStart) {
            try {
                RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST.stop();
                Platform.runLater(() -> labelStatusDeleteScript.setText(TXT_STATUS_SCRIPT_DELETE + " " + TXT_LOADING));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST.set(RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE); //Ограничим число повторений
        RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST.setCycleCount(RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE);
        RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST.play(); //Запускаем
    }

    private void onTimeStopDelete(String text) {
        try {
            RUN_DELETE_OLD_DOCUMENT_TIMELINE_PERIOD_REQUEST.stop();
        }catch (Exception e) {
            e.printStackTrace();
        }
        RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST.set(RUN_DELETE_OLD_DOCUMENT_INTEGER_PERIOD_REQUEST_VALUE);
        Platform.runLater(() -> labelStatusDeleteScript.setText(text));
    }

    private void onTimerStart(boolean isNotStart) {
        if(isNotStart) {
            try {
                RUN_DB_EXECUTOR_TIMELINE_PERIOD_REQUEST.stop();
                Platform.runLater(() -> labelTimer.setText(TXT_SECONDS_NEXT_REQUEST + " " + TXT_LOADING));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST.set(RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE); //Ограничим число повторений
        RUN_DB_EXECUTOR_TIMELINE_PERIOD_REQUEST.setCycleCount(RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE);
        RUN_DB_EXECUTOR_TIMELINE_PERIOD_REQUEST.play(); //Запускаем
    }

    private void onTimerStop() {
        try {
            RUN_DB_EXECUTOR_TIMELINE_PERIOD_REQUEST.stop();
        }catch (Exception e) {
            e.printStackTrace();
        }
        RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST.set(RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST_VALUE);
        labelTimer.setText(TXT_SECONDS_NEXT_REQUEST + " " + RUN_DB_EXECUTOR_INTEGER_PERIOD_REQUEST);
    }

    private String responseData(HttpURLConnection c) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private Dialog<ButtonType> dialogBuild(String headerText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(TXT_ERROR);
        dialog.setHeaderText(headerText);
        ButtonType loginButtonType = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label label = new Label();
        label.setFont(Font.font("Verdana", FontWeight.BOLD, 25));
        label.setText(headerText);
        grid.add(label,0,0);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(false);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(Event::consume);
        dialog.setResultConverter(dialogButton -> {
            if(dialogButton == loginButtonType) {
                return new ButtonType("Ok");
            }
            return null;
        });

        return dialog;
    }

}
