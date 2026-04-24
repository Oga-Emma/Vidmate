package com.github.ogaemma.vidmate.model;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileDto {
    private final SimpleStringProperty name;
    private final SimpleStringProperty path;
    private final SimpleStringProperty dateModified;

    public FileDto(File file) {
        this.name = new SimpleStringProperty(file.getName());
        this.path = new SimpleStringProperty(file.getAbsolutePath());

        String formatted = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FORMATTER);
        this.dateModified = new SimpleStringProperty(formatted);
    }

    public String getName() { return name.get(); }
    public String getPath() { return path.get(); }
    public String dateModified() { return dateModified.get(); }


    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
