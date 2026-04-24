package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class MainViewModel {

    ObservableList<FileDto> fileList;

    private File directory;

    public MainViewModel() {
        fileList = FXCollections.observableArrayList();
    }

    public void setDirectory(File selectedDirectory) {
        directory = selectedDirectory;

        var files = Arrays.stream(Objects.requireNonNull(directory.listFiles())).map(FileDto::new).toList();
        fileList.removeAll();
        fileList.addAll(files);
    }
}
