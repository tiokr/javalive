package com.github.tiokr.javalive;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class LiveJava extends Application {

    private static final long NANO_TO_MILLIS_RATIO = 1000000L;
    private static final String MAIN_FXML = "/main.fxml";

    private static LiveJava instance;

    private boolean readyToRun = false;
    private MainController mainController;
    private int latestChangeId = 0;

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(MAIN_FXML)));
        String version = readVersion();
        primaryStage.setTitle("Live Java Editor " + version);
        Scene mainScene = new Scene(root);
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    public static LiveJava getInstance() {
        return instance;
    }

    private void runEditorCode() {
        if (!readyToRun) {
            return;
        }

        latestChangeId++;
        var currentChangeId = latestChangeId; // make sure only the last change updates the gui
        new Thread(() -> {
            try {
                var start = System.nanoTime();
                var code = mainController.getInput().getText();
                var className = findClassName(code);
                if (currentChangeId == latestChangeId) { // if this thread is the latest thread
                    var strings = compileAndRunJavaCode(code, className);
                    var codeEvaluationTime = System.nanoTime() - start;
                    Platform.runLater(() -> { // update gui elements on the main thread
                        if (currentChangeId == latestChangeId) { // if this was still called by the latest thread
                            mainController.getRightStatus().setText("Took " + codeEvaluationTime / NANO_TO_MILLIS_RATIO + "ms");
                            mainController.getOutput().clear();
                            strings.forEach(string -> mainController.getOutput().appendText(string + "\n"));
                            mainController.getLeftStatus().setText(className);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<String> compileAndRunJavaCode(String text, String className) throws IOException {
        List<String> strings = new ArrayList<>();
        Files.write(Paths.get("./" + className + ".java"), text.getBytes());
        runCommand("javac ./" + className + ".java", strings::add);
        runCommand("java " + className, strings::add);
        return strings;
    }

    private String findClassName(String text) {
        var nameStart = text.indexOf(" class ") + 7;
        var nameEnd = text.indexOf(" ", nameStart);
        return text.substring(nameStart, nameEnd);
    }

    private void runCommand(String command, Consumer<String> textConsumer) throws IOException {
        var commands = command.split(" ");
        var builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        var proc = builder.start();
        var in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        in.lines().forEach(textConsumer);
    }

    public void controllerLoaded(MainController controller) {
        mainController = controller;
        readyToRun = true;
        runEditorCode();
        mainController
                .getInput()
                .textProperty()
                .addListener((observable, oldValue, newValue) -> runEditorCode());
    }

    private String readVersion() throws IOException {
        Properties prop = new Properties();
        String propFileName = "version.properties";
        var inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

        if (inputStream != null) {
            prop.load(inputStream);
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }

        return prop.getProperty("version");
    }
}