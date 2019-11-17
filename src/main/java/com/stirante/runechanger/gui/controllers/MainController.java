package com.stirante.runechanger.gui.controllers;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.stirante.runechanger.gui.ProgressDialog;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.log.LogRequest;
import com.stirante.runechanger.util.AsyncTask;
import com.stirante.runechanger.util.LangHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainController {
    private final Stage stage;

    public TextField search;
    public ImageView back;
    public Pane sidebar;
    public Button report;
    public Pane fullContentPane;
    public Pane contentPane;
    public Pane container;

    public Button settings;
    //    public Button gameSettings;
//    public Button otherSettings;
//    public Button loot;
    public Button[] sidebarButtons;

    private Consumer<Champion> searchHandler;

    private double xOffset;
    private double yOffset;

    public MainController(Stage stage) {
        this.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"), LangHelper.getLang());
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AutoCompletionBinding<String> autoCompletion =
                TextFields.bindAutoCompletion(search, (AutoCompletionBinding.ISuggestionRequest param) -> {
                    if (param.getUserText().isEmpty()) {
                        return new ArrayList<>();
                    }
                    return FuzzySearch
                            .extractSorted(param.getUserText(), Champion.values(), Champion::getName, 3)
                            .stream()
                            .map(championBoundExtractedResult -> championBoundExtractedResult.getReferent().getName())
                            .collect(Collectors.toList());
                });
        autoCompletion.setOnAutoCompleted(this::onSearch);
        autoCompletion.prefWidthProperty().bind(search.widthProperty());
        back.setVisible(false);
        sidebarButtons = new Button[]{settings/*, gameSettings, otherSettings, loot*/};
        report.getTooltip().setShowDelay(Duration.ZERO);
    }

    public void setFullContent(Node node) {
        fullContentPane.getChildren().clear();
        fullContentPane.getChildren().add(node);
        fullContentPane.setVisible(true);
        contentPane.setVisible(false);
        sidebar.setVisible(false);
        report.setVisible(false);
        back.setVisible(true);
    }

    public void setContent(Node node) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(node);
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
        sidebar.setVisible(true);
        report.setVisible(true);
        back.setVisible(false);
    }

    @FXML
    public void onBugReport(ActionEvent actionEvent) {
        ProgressDialog progressDialog = new ProgressDialog(LangHelper.getLang().getString("logs_progress"));
        progressDialog.getDialogStage().show();
        new AsyncTask<Void, Void, String>() {
            @Override
            public String doInBackground(Void[] params) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                for (Logger logger : context.getLoggerList()) {
                    for (Iterator<Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index.hasNext(); ) {
                        Appender<ILoggingEvent> appender = index.next();
                        if (appender instanceof FileAppender) {
                            try {
                                byte[] encoded =
                                        Files.readAllBytes(Paths.get(((FileAppender<ILoggingEvent>) appender).getFile()));
                                return new LogRequest(new String(encoded)).submit();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public void onPostExecute(String code) {
                progressDialog.getDialogStage().close();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                ButtonType copyBtn = new ButtonType(LangHelper.getLang().getString("copy_code"));
                if (code != null) {
                    alert.setTitle(LangHelper.getLang().getString("logs_sent"));
                    alert.setContentText(
                            String.format(LangHelper.getLang().getString("logs_sent_msg"), code));
                    alert.getButtonTypes().add(copyBtn);
                }
                else {
                    alert.setTitle(LangHelper.getLang().getString("logs_failed"));
                    alert.setContentText(LangHelper.getLang().getString("logs_failed"));
                    alert.setAlertType(Alert.AlertType.ERROR);
                }
                alert.setHeaderText(null);
                Optional<ButtonType> buttonType = alert.showAndWait();
                if (buttonType.isPresent() && buttonType.get() == copyBtn) {
                    StringSelection stringSelection = new StringSelection(code);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                }
            }
        }.execute();
    }

    @FXML
    public void onTabSelect(ActionEvent actionEvent) {
        if (actionEvent.getTarget() == settings) {
            setFullContent(new SettingsController(stage).container);
        }
//        Button clicked = (Button) actionEvent.getTarget();
//        KeyValue[] startValues = new KeyValue[sidebarButtons.length];
//        KeyValue[] endValues = new KeyValue[sidebarButtons.length];
//        int i = 0;
//        for (Button button : sidebarButtons) {
//            if (button == clicked) continue;
//            startValues[i] = new KeyValue(button.translateXProperty(), button.translateXProperty().doubleValue(), Interpolator.EASE_OUT);
//            endValues[i] = new KeyValue(button.translateXProperty(), 0, Interpolator.EASE_OUT);
//            i++;
//        }
//        startValues[i] = new KeyValue(clicked.translateXProperty(), clicked.translateXProperty().doubleValue(), Interpolator.EASE_OUT);
//        endValues[i] = new KeyValue(clicked.translateXProperty(), 20, Interpolator.EASE_OUT);
//        KeyFrame start = new KeyFrame(Duration.ZERO, startValues);
//        KeyFrame end = new KeyFrame(Duration.millis(100), endValues);
//        Timeline timeline = new Timeline(start, end);
//        timeline.play();
    }

    @FXML
    public void onHandlePress(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    public void onHandleDrag(MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    public void onClose(MouseEvent event) {
        System.exit(0);
    }

    @FXML
    public void onMinimize(MouseEvent event) {
        stage.hide();
    }

    public void setOnChampionSearch(Consumer<Champion> handler) {
        this.searchHandler = handler;
    }

    public void onSearch(AutoCompletionBinding.AutoCompletionEvent event) {
        if (searchHandler != null) {
            searchHandler.accept(Champion.getByName((String) event.getCompletion()));
        }
        search.setText("");
        search.getParent().requestFocus();
    }

    @FXML
    public void onBack(MouseEvent mouseEvent) {
        fullContentPane.getChildren().clear();
        contentPane.setVisible(true);
        fullContentPane.setVisible(false);
        sidebar.setVisible(true);
        report.setVisible(true);
        back.setVisible(false);
    }
}
