package com.github.ogaemma.vidmate;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class MainController implements TabManager {
    @FXML
    public TabPane mainTabView;

    private Tab addTabButton;

    @FXML
    private void initialize() {

        loadDrives();

        addTabButton = new Tab("+");
        addTabButton.setClosable(false);

        mainTabView.getTabs().add(addTabButton);
        loadNewTab(false, null, null, null);

        addTabButton.setOnSelectionChanged(event -> {
            if (addTabButton.isSelected()) {
                loadNewTab(true, null, null, null);
            }
        });

    }

    private void loadDrives() {
        Path volumes = Paths.get("/Volumes");

        try (Stream<Path> paths = Files.list(volumes)) {
            paths.filter(Files::isDirectory)
                    .forEach(path -> {
                        System.out.println("Drive: " + path);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadNewTab(boolean closable, List<File> roots, String query, String tabNameStr){
        try {
            String tabName = Objects.isNull(tabNameStr) || tabNameStr.isBlank() ?  "New Tab " + mainTabView.getTabs().size() : tabNameStr;


            Tab tab = new Tab(tabName);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("content-view-table.fxml")
            );

            Parent content = loader.load();

            tab.setContent(content);
            tab.setClosable(closable);

            mainTabView.getTabs().add(mainTabView.getTabs().size() - 1, tab);
            mainTabView.getSelectionModel().select(tab);

            var controller = ((ContentTableController) loader.getController());
            controller.setTabController(this);

            if(roots != null && !roots.isEmpty()){
                controller.setSelectedDirectory(roots);
                if(Objects.nonNull(query) && !query.isBlank()){
                    controller.performSearchDeepSearch(query);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openNewTab(File file) {
        if(file != null){
            var folder = file.isDirectory() ? file : file.getParentFile();
            loadNewTab(true, List.of(folder), null, file.getName());
        }
    }

    @Override
    public void openNewDeepSearchTab(List<File> rootDirectories, String query) {
        if(!rootDirectories.isEmpty()){
            loadNewTab(true, rootDirectories, query, query);
        }
    }

    @Override
    public void updateNewTabName(String name) {

    }
}
