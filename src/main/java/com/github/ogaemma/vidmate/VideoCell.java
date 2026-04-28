package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

import java.io.IOException;

public class VideoCell extends ListCell<FileDto> {

    private FXMLLoader loader;
    private Parent root;
    private VideoCellController controller;

    @Override
    protected void updateItem(FileDto item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        if (loader == null) {
            loader = new FXMLLoader(getClass().getResource("video-cell.fxml"));
            try {
                root = loader.load();
                controller = loader.getController();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        controller.setData(item);
        setGraphic(root);
    }
}
