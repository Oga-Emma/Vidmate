package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ContentController {
    @FXML
    public TextField searchTextField;

    @FXML
    public Button selectFolderButton;

    @FXML
    private TreeView<File> fileTreeView;

    @FXML
    private ListView<FileDto> contentListView;

    @FXML
    private TableView<FileDto> tableView;

    @FXML
    private TableColumn<FileDto, String> nameColumn;

    @FXML
    private TableColumn<FileDto, String> dateColumn;

    @FXML
    private TableColumn<FileDto, String> typeColumn;

    File directory;

    ObservableList<FileDto> tableFileList;
    List<FileDto> files;

    @FXML
    private void initialize() {

        searchTextField.setOnAction(event -> {
            filterResult();
        });

        initTable();
        initContentListView();
        initContentFileTree();
    }

    private void initContentFileTree() {
        fileTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        fileTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName().isEmpty() ? item.getPath() : item.getName());
                }
            }
        });

        fileTreeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        File folder = newVal.getValue();
                        if (folder.isDirectory()) {
                            setDirectory(folder);
                        }

                        fileTreeView.getSelectionModel().select(newVal);
                    }
                }
        );
    }

    private void initContentListView() {
        contentListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        contentListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleSelected(contentListView.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void initTable() {
        tableFileList = FXCollections.observableArrayList();

        resizeColumns();

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().dateModified())
        );

        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(Arrays.stream(cellData.getValue().getPath().split("\\.")).toList().getLast())
        );

        tableView.setItems(tableFileList);
        tableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadNestedFiles(newSelection);
                    }
                }
        );


        tableView.setRowFactory(tv -> {
            TableRow<FileDto> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    var file = row.getItem();
                    handleSelected(file);
                }
            });

            var rowMenu = new ContextMenu();
            var showOpenAction = new MenuItem("Launch");
            showOpenAction.setOnAction(e -> {
                FileDto fileDto = row.getItem();
                if (fileDto != null) {
                    showInFileManager(new File(fileDto.getPath()));
                }
            });

            var showInFinderAction = new MenuItem("Show in finder");
            showInFinderAction.setOnAction(e -> {
                FileDto fileDto = row.getItem();
                if (fileDto != null) {
                    showInFileManager(new File(fileDto.getPath()));
                }
            });

            var showEnclosingFolderAction = getMenuItem(row);

            rowMenu.getItems().addAll(showOpenAction, showInFinderAction, showEnclosingFolderAction);
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            return row;
        });
    }

    private MenuItem getMenuItem(TableRow<FileDto> row) {
        var showEnclosingFolderAction = new MenuItem("Enclosing finder");
        showEnclosingFolderAction.setOnAction(e -> {
            FileDto fileDto = row.getItem();
            if (fileDto == null) {
                return;
            }

            var folder = new File(fileDto.getPath()).getParentFile();
            if (folder == null || folder.getParentFile() == null ) {
                return;
            }

            setDirectory(folder.getParentFile());
        });
        return showEnclosingFolderAction;
    }

    private void resizeColumns() {
        nameColumn.setMinWidth(200);
        nameColumn.setMaxWidth(Double.MAX_VALUE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        typeColumn.setMinWidth(150);
        dateColumn.setMinWidth(100);
    }

    @FXML
    protected void handleSelectFolderClicked(ActionEvent ae) {
        var source = (Node) ae.getSource();
        var stage = source.getScene().getWindow();

        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder File");

        var selectedDirectory = directoryChooser.showDialog(stage);
        setDirectory(selectedDirectory);
    }

    @FXML
    protected void handleClearSearch() {
        searchTextField.clear();
        filterResult();
    }


    private void setDirectory(File selectedDirectory) {
        directory = selectedDirectory;
        this.files = getFileDtoList(directory);
        filterResult();
        updateTreeView();
        resetSelection();
    }

    private void updateTreeView() {
        File parent = directory.getParentFile();

        if (parent == null) return;

        TreeItem<File> rootItem = createNode(parent);
        rootItem.setExpanded(true);

        fileTreeView.setRoot(rootItem);
    }

    private TreeItem<File> createNode(File parent) {
        TreeItem<File> item = new TreeItem<>(parent.getParentFile());

        if (parent.isDirectory()) {
            item.getChildren().add(new TreeItem<>());

            File[] children = parent.listFiles();

            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        item.getChildren().add(new TreeItem<>(child));
                    }
                }
            }
            /*if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        item.getChildren().add(createNode(child));
                    }
                }
            }*/
        }

        return item;
    }

    private List<FileDto> getFileDtoList(File directory) {
        var result = Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(it -> !it.getName().startsWith("."));
        return result.map(FileDto::new).toList();
    }

    private void filterResult() {
        if(this.files == null || this.files.isEmpty()) return;

        var search = searchTextField.getText().trim().toLowerCase();

        var result = search.isBlank() ? this.files : this.files.stream().filter(it -> it.getName().toLowerCase().contains(search)).toList();

        tableFileList.clear();
        tableFileList.addAll(result);

        resizeColumns();
    }

    private void resetSelection() {
        contentListView.setItems(FXCollections.observableArrayList());
    }

    private void loadNestedFiles(FileDto newSelection) {
        var file = new File(newSelection.getPath());

        if (file.isDirectory()) {
            File[] nested = file.listFiles();

            if (nested != null) {
                contentListView.setItems(FXCollections.observableArrayList(getFileDtoList(file)));
            } else {
                contentListView.setItems(FXCollections.emptyObservableList());
            }
        } else {
            // If it's a file, maybe clear or show just that file
            contentListView.setItems(FXCollections.observableArrayList(newSelection));
        }
    }

    private void handleSelected(FileDto fileDto) {
        File selectedFile = new File(fileDto.getPath());

        if (selectedFile.isFile()) {
            openFile(selectedFile);
        } else if (selectedFile.isDirectory()) {
            setDirectory(selectedFile);
        }
    }

    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showInFileManager(File file) {
        try {
            String path = file.getAbsolutePath();

            new ProcessBuilder("open", "-R", path).start();
            /*String path = file.getAbsolutePath();

            if (isMac()) {
                new ProcessBuilder("open", "-R", path).start();

            } else if (isWindows()) {
                new ProcessBuilder("explorer.exe", "/select,", path).start();

            } else {
                // Linux → open parent folder
                new ProcessBuilder("xdg-open", file.getParent()).start();
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
