package com.github.ogaemma.vidmate;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Vidmate");
        stage.setScene(scene);

        var screenBounds = Screen.getPrimary().getVisualBounds();
        // Set stage to 80% of screen width and height
        var percentage = 1;

        stage.setMinWidth(screenBounds.getWidth() * percentage);
        stage.setMinHeight(screenBounds.getHeight() * percentage);

        stage.setMaximized(true);
        stage.setResizable(true);

        stage.show();
    }
}
