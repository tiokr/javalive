package com.github.tiokr.javalive;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private MenuItem about;
    @FXML
    private TextArea codeEditor;
    @FXML
    private TextArea output;
    @FXML
    private Label rightStatus;
    @FXML
    private Label leftStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LiveJava.getInstance().controllerLoaded(this);
    }

    public TextArea getOutput() {
        return output;
    }

    public TextArea getInput() {
        return codeEditor;
    }

    public Label getRightStatus() {
        return rightStatus;
    }

    public Label getLeftStatus() {
        return leftStatus;
    }

    public MenuItem getAbout() {
        return about;
    }
}
