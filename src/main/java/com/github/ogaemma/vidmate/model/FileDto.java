package com.github.ogaemma.vidmate.model;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FileDto {
    private final String fileName;
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

        var n = name.get().toLowerCase();

        if(n.contains(".xxx")){
            this.fileName = n.substring(0, n.indexOf(".xxx"));
        }else if(n.contains(".prt")){
            this.fileName = n.substring(0, n.indexOf(".prt"));
        } else if (n.contains(".")) {
            this.fileName = n.substring(0, n.lastIndexOf("."));
        }else {
            this.fileName = n;
        }
    }

    public String getName() { return name.get(); }
    public String getPath() { return path.get(); }
    public String dateModified() { return dateModified.get(); }


    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileDto fileDto = (FileDto) o;
        return Objects.equals(fileName, fileDto.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileName);
    }
}
