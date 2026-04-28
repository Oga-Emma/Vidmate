package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;

public class MainController implements TabManager {
    @FXML
    public TabPane mainTabView;

    private Tab addTabButton;

    @FXML
    private void initialize() {

        addTabButton = new Tab("+");
        addTabButton.setClosable(false);

        mainTabView.getTabs().add(addTabButton);
        loadNewTab(false, null);

        addTabButton.setOnSelectionChanged(event -> {
            if (addTabButton.isSelected()) {
                loadNewTab(true, null);
            }
        });

    }

    private void loadNewTab(boolean closable, File file){
        try {
            String tabName = "New Tab " + mainTabView.getTabs().size();
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

            if(file != null && file.isDirectory()){
                controller.setDirectory(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openNewTab(File file) {
        if(file != null && file.isDirectory()){
            loadNewTab(true, file);
        }
    }
}
