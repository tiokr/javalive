module JavaLiveEditor.main {
    requires javafx.controls;
    requires javafx.fxml;
    exports com.github.tiokr.javalive;
    opens com.github.tiokr.javalive to javafx.fxml;
}