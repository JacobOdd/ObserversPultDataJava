package sample;

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

    private ScheduledExecutorService executor = null;
    private boolean isIndicator = false;
    private int sizeRequestSuccess = 0;
    private int sizeRequestAll = 0;

    private final String FILE_NAME_HOST = "settingsHOST.txt";
    private final String FILE_NAME_PHP = "settingsPHP.txt";
    private final int PERIOD_AND_VALUE = 10;
    private final AtomicInteger PERIOD_NUMBER = new AtomicInteger(PERIOD_AND_VALUE);
    private final Timeline TIMELINE = new Timeline(
            new KeyFrame(
                    Duration.millis(1000), //1000 мс * 15 сек = 15 сек
                    ae -> {
                        PERIOD_NUMBER.getAndDecrement();
                        Platform.runLater(() -> {
                            if(PERIOD_NUMBER.get() <= 0) {
                                labelTimer.setText("Секунд до след.запроса: загрузка...");
                            } else {
                                labelTimer.setText("Секунд до след.запроса: " + PERIOD_NUMBER);
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
        executor = Executors.newSingleThreadScheduledExecutor();
        setIsIndicator(true,false);
        labelStatus.setText("Ожидайте " + PERIOD_AND_VALUE + " секунд");

        onTimerStart(false);
        Runnable periodTask = () -> {
            try {
                onCode();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                onRequestError("Неправильный URL-хост");
            } catch (Exception e) {
                e.printStackTrace();
                onRequestError("Ошибка: Упал apache сервер или слабое соединения");
            }
        };
        executor.scheduleAtFixedRate(periodTask, PERIOD_AND_VALUE, PERIOD_AND_VALUE, TimeUnit.SECONDS);
    }

    @FXML
    public void offIndicator() {
        if(!isIndicator) {
            return;
        }

        setIsIndicator(false,false);
        labelStatus.setText("Выключен");

        try {
            onTimerStop();
            executor.shutdown();
            executor = null;
        }catch (Exception e) {
            onTimerStop();
            executor.shutdown();
            executor = null;
            e.printStackTrace();
        }
    }

    private void readSettingsHOST() {
        readSettingsFile(labelHost,tfHost,FILE_NAME_HOST);
    }

    private void readSettingsPHP() {
        readSettingsFile(labelPHP,tfPHP,FILE_NAME_PHP);
    }

    private void readSettingsFile(Label label, TextField textField, String FILE_NAME) {
        try {
           /* InputStream inputStream = InputStream.class.getResourceAsStream("/sample/"+FILE_NAME);

            int i=-1;
            StringBuilder result = new StringBuilder();
            while((i=inputStream.read())!=-1){
                result.append((char) i);
            }*/

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
                label.setText("Нет хоста");
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
      /*      if(!folder.isDirectory()) {
                System.out.println("Create");
                folder.mkdir();
            }*/
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
        labelTimer.setText("Секунд до след.запроса: " + PERIOD_NUMBER);
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
            labelRequestSizeSuccess.setText("Запросов успешно: " + sizeRequestSuccess);
            labelStatus.setText("Работает");
            setIsIndicator(true,true);
            onTimerStart(true);
        });
    }


    private void onRequestError(String s) {
        Platform.runLater(()-> {
            onTimerStop();
            executor.shutdown();
            executor = null;

            labelStatus.setText(s);

            setIsIndicator(false,false);
            onMediaOnDialog(s);
        });
    }

    private void onRequestCode(int responseCode) {
        Platform.runLater(()-> {
            onTimerStop();
            executor.shutdown();
            executor = null;

            setIsIndicator(false,false);

            String str;
            if(responseCode == 404) {
                labelStatus.setText("Сетевая ошибка, неправильное имя php файла: " + responseCode);
                str = "Сетевая ошибка, неправильное имя php файла: " + responseCode;
            } else {
                labelStatus.setText("Сетевая ошибка: " + responseCode);
                str = "Сетевая ошибка: " + responseCode;
            }

            onMediaOnDialog(str);
        });
    }



    private void onMediaOnDialog(String str) {
        String mediaURL = getClass().getResource("/sample/sirena.mp3").toExternalForm();
        System.out.println(mediaURL);

        Media media = new Media(mediaURL);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();

        Dialog<ButtonType> dialog = dialogBuild(str);
        Optional<ButtonType> buttonType = dialog.showAndWait();
        buttonType.ifPresent(type -> {
            if(type.getText().equals("Ok")) {
                mediaPlayer.stop();
                dialog.close();
            }
        });

    }

    private void onTimerStart(boolean isNotStart) {
        if(isNotStart) {
            try {
                TIMELINE.stop();
                Platform.runLater(() -> labelTimer.setText("Секунд до след.запроса: загрузка..."));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PERIOD_NUMBER.set(PERIOD_AND_VALUE); //Ограничим число повторений
        TIMELINE.setCycleCount(PERIOD_AND_VALUE+1);
        TIMELINE.play(); //Запускаем
    }

    private void onTimerStop() {
        try {
            TIMELINE.stop();
        }catch (Exception e) {
            e.printStackTrace();
        }
        PERIOD_NUMBER.set(PERIOD_AND_VALUE);
        labelTimer.setText("Секунд до след.запроса: " + PERIOD_NUMBER);
    }

    private void onCode() throws Exception {
        sizeRequestAll++;

        Platform.runLater(()-> labelRequestSizeAll.setText("Запросов всего за сеанс: " + sizeRequestAll));

        String phpPOST = URLEncoder.encode("200", "UTF-8");
        URL url = new URL("http://"+labelHost.getText()+"/"+labelPHP.getText());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        OutputStreamWriter writer = new OutputStreamWriter(
                conn.getOutputStream());

        writer.write("values=" + phpPOST);
        writer.close();

        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            onRequestSuccess();
        } else {
            onRequestCode(conn.getResponseCode());
        }

        conn.disconnect();
        System.out.println(conn.getResponseCode());
        System.out.println("VIPOLNIS URL: " + conn.getURL().toString());
    }

    private Dialog<ButtonType> dialogBuild(String headerText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("ОШИБКА");
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
