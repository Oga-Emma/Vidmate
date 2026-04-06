module com.github.ogaemma.vidmate {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.github.ogaemma.vidmate to javafx.fxml;
    exports com.github.ogaemma.vidmate;
}