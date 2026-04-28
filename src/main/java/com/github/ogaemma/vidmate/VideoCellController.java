package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class VideoCellController {

    @FXML private ImageView thumbnail;
    @FXML private Label title;
    @FXML private Label duration;
    @FXML private Label size;
    @FXML private Label format;
    @FXML private Label resolution;

    private FileDto file;

    public void setData(FileDto file) {
        this.file = file;

        title.setText(file.getName());
/*        duration.setText(file.getDuration());
        size.setText(file.getSize());
        format.setText(file.getFormat());
        resolution.setText(file.getResolution());*/

//        thumbnail.setImage(new Image("file:placeholder.png"));
    }

    @FXML
    private void onPlay() {
        if (file != null) {
            // play video
        }
    }

    @FXML
    private void onShowInFinder() {
        if (file != null) {
            // open folder
        }
    }
}