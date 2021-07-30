package com.github.tiokr.javalive;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private MenuItem about;
    @FXML
    private CodeArea codeEditor;
    @FXML
    private TextArea output;
    @FXML
    private Label rightStatus;
    @FXML
    private Label leftStatus;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        codeEditor.setParagraphGraphicFactory(LineNumberFactory.get(codeEditor));
        InputMap<KeyEvent> im = InputMap.consume(
                EventPattern.keyPressed(KeyCode.TAB),
                e -> codeEditor.replaceSelection("    ")
        );
        Nodes.addInputMap(codeEditor, im);
        LiveJava.getInstance().controllerLoaded(this);
    }

    public TextArea getOutput() {
        return output;
    }

    public CodeArea getInput() {
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
