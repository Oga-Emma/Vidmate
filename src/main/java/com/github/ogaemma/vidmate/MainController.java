package com.github.ogaemma.vidmate;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.awt.*;
import java.io.IOException;

public class MainController {
    @FXML
    public TabPane mainTabView;

    private Tab addTabButton;

    @FXML
    private void initialize() {

        addTabButton = new Tab("+");
        addTabButton.setClosable(false);

        mainTabView.getTabs().add(addTabButton);
        loadNewTab(false);

        addTabButton.setOnSelectionChanged(event -> {
            if (addTabButton.isSelected()) {
                loadNewTab(true);
            }
        });

    }

    private void loadNewTab(boolean closable){
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
