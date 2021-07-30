module JavaLiveEditor.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires janino;
    requires commons.compiler;
    requires org.fxmisc.richtext;
    requires wellbehavedfx;
    exports com.github.tiokr.javalive;
    opens com.github.tiokr.javalive to javafx.fxml;
}