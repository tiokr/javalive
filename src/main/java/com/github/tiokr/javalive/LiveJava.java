package com.github.tiokr.javalive;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.zeroturnaround.exec.stream.NullOutputStream;
import org.zeroturnaround.process.PidProcess;
import org.zeroturnaround.process.Processes;
import org.zeroturnaround.process.WindowsProcess;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class LiveJava extends Application {

    private static final long NANO_TO_MILLIS_RATIO = 1000000L;
    private static final String MAIN_FXML = "/main.fxml";

    private static LiveJava instance;

    private final Map<Long, PidProcess> createdProcesses = new ConcurrentHashMap<>();

    private Thread activeThread = null;
    private boolean readyToRun = false;
    private MainController mainController;
    private Properties properties;
    private static final PrintStream err = System.err;

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.setErr(err);
        instance = this;
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(MAIN_FXML)));
        String version = getProperties().getProperty("version");
        primaryStage.setTitle("Live Java Editor " + version);
        Scene mainScene = new Scene(root);
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setErr(new PrintStream(new NullOutputStream())); // hide annoying javafx warnings on start
        launch();
    }

    public static LiveJava getInstance() {
        return instance;
    }

    private void runEditorCode() {
        if (!readyToRun) {
            return;
        }


        if (activeThread != null) {
            activeThread.interrupt();
        }
        activeThread = new Thread(() -> {
            try {
                var toRemove = new HashSet<Long>();
                for (PidProcess process : createdProcesses.values()) {
                    process.destroyForcefully();
                    toRemove.add((long)process.getPid());
                }
                toRemove.forEach(createdProcesses::remove);

                var start = System.nanoTime();
                var code = mainController.getInput().getText();
                var className = findClassName(code);

                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                var strings = compileAndRunJavaCode(code, className);
                var codeEvaluationTime = System.nanoTime() - start;
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                Platform.runLater(() -> { // update gui elements on the main thread
                    mainController.getRightStatus().setText("Took " + codeEvaluationTime / NANO_TO_MILLIS_RATIO + "ms");
                    mainController.getOutput().clear();
                    strings.forEach(string -> mainController.getOutput().appendText(string + "\n"));
                    mainController.getLeftStatus().setText(className);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    mainController.getOutput().clear();
                    mainController.getOutput().appendText("Error: " + e.getMessage() + "\n");
                });
            } finally {
                Platform.runLater(() -> {
                    mainController.getOutput().setScrollTop(0);
                    mainController.getOutput().selectRange(0, 0);
                });
            }
        });
        activeThread.start();
    }

    private List<String> compileAndRunJavaCode(String text, String className) throws Exception {
        // change stream into text collector
//        TextCollector textCollector = new TextCollector();
        Files.write(Paths.get("./" + className + ".java"), text.getBytes());
//        new SimpleCompiler().cook(new FileReader("./" + className + ".java"));
//        Class<?> aClass = sc.getClassLoader().loadClass(className);

//        var mainMethod = aClass.getMethod("main", String[].class);
//        mainMethod.invoke(null, (Object) null);

// TODO: allow user to choose javac or janino with setting menu
//        runCommand("javac ./" + className + ".java", strings::add);
        var strings = new ArrayList<String>();
//        runCommand("janinoc " + className + ".java", strings::add);

        var name = getProperties().get("name");
        var version = getProperties().get("version");
        var classPath = name + "-" + version + ".jar";
        var resource = getClass().getResource(getClass().getSimpleName() + ".class");
        var path = resource == null ? "" : resource.getPath();
        if (path.contains(".jar!")) {
            int start = path.indexOf("file:");
            if (start == -1) {
                start = 0;
            } else {
                start = 5;
            }
            classPath = path.substring(start, path.indexOf(".jar!") + 4);
        } else {
            int start = path.indexOf("file:");
            if (start == -1) {
                start = 0;
            } else {
                start = 5;
            }
            var tempPath = path.substring(start, path.indexOf("/build/"));
            classPath = tempPath + "/build/libs/" + classPath;
        }

        runCommand("java "
                        + "-cp " + classPath + " "
                        + "org.codehaus.janino.SimpleCompiler "
                        + "./" + className + ".java " + className,
                strings::add);
        return strings;
    }

    private String findClassName(String text) {
        var nameStart = text.indexOf(" class ") + 7;
        var nameEnd = text.indexOf(" ", nameStart);
        return text.substring(nameStart, nameEnd);
    }

    private void runCommand(String command, Consumer<String> textConsumer) throws IOException {
        var commands = command.split(" ");
        var proc = new ProcessBuilder(commands).redirectErrorStream(true).start();
        var in = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        var pidProcess = Processes.newPidProcess((int) proc.pid());
        if (pidProcess instanceof WindowsProcess) {
            ((WindowsProcess) pidProcess).setIncludeChildren(true);
        }
        createdProcesses.put(proc.pid(), pidProcess);

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

    private Properties getProperties() throws IOException {
        if (properties == null) {
            properties = new Properties();
            String propFileName = "version.properties";
            var inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }
        }

        return properties;
    }
}