package com.github.ogaemma.vidmate.model;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FileDto {
    private final String fileName;
    private final boolean fromSearch;
    private final String name;
    private final String path;
    private final String dateModified;

    public FileDto(File file) {
       this(file, false);
    }

    public FileDto(File file, boolean fromSearch) {
        this.fromSearch = fromSearch;
        this.name = file.getName();
        this.path = file.getAbsolutePath();

        this.dateModified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(FORMATTER);

        if(name.contains(".xxx")){
            this.fileName = name.substring(0, name.indexOf(".xxx"));
        }else if(name.contains(".prt")){
            this.fileName = name.substring(0, name.indexOf(".prt"));
        } else if (name.contains(".")) {
            this.fileName = name.substring(0, name.lastIndexOf("."));
        }else {
            this.fileName = name;
        }
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getDateModified() { return dateModified; }
    public boolean isFromSearch() {return fromSearch;}

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
