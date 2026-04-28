package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentTableController {

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

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private File directory;

    ObservableList<FileDto> tableFileList;
    List<FileDto> files;

    private TabManager tabManager;


    @FXML
    private void initialize() {

        searchTextField.setOnAction(event -> {
            filterResult();
        });

        initTable();
        initContentListView();
        initContentFileTree();
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

    @FXML
    protected void handleDeepSearch() {
        if(directory == null || searchTextField.getText().isEmpty()) return;
//        searchAsync(directory, searchTextField.getText().toLowerCase());
        searchStreaming(directory, searchTextField.getText().toLowerCase());
    }

    @FXML
    protected void handleNavigateUp() {
        if(directory == null) return;

        navigateUp(new FileDto(directory));
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

            var showInNewTabAction = new MenuItem("Show in new tab");
            showInNewTabAction.setOnAction(e -> {
                FileDto fileDto = row.getItem();
                if (fileDto != null) {
                    tabManager.openNewTab(new File(fileDto.getPath()));
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

            rowMenu.getItems().addAll(showOpenAction, showInNewTabAction, showInFinderAction, showEnclosingFolderAction);
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
            navigateUp(row.getItem());
        });
        return showEnclosingFolderAction;
    }

    private void navigateUp(FileDto fileDto) {
        if (fileDto == null) {
            return;
        }

        var folder = new File(fileDto.getPath()).getParentFile();
        if (folder == null || folder.getParentFile() == null ) {
            return;
        }

        setDirectory(folder.getParentFile());
    }

    private void resizeColumns() {
        nameColumn.setMinWidth(200);
        nameColumn.setMaxWidth(Double.MAX_VALUE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        typeColumn.setMinWidth(150);
        dateColumn.setMinWidth(100);
    }

    private Task<List<File>> currentTask;

    private void searchAsync(File root, String query) {

        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        currentTask = new Task<>() {
            @Override
            protected List<File> call() {
                List<File> results = new ArrayList<>();
                searchRecursiveCancelable(root, query, results);
                return results;
            }
        };

        currentTask.setOnSucceeded(e -> {
            List<FileDto> result = currentTask.getValue().stream().map(FileDto::new).toList();
            updateUI(result);
            /*listView.setItems(FXCollections.observableArrayList(
                    (List<File>) currentTask.getValue()
            ));*/
        });

        executor.submit(currentTask);
    }

    private Task<Void> searchTask;

    private void searchStreaming(File root, String query) {

        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel();
        }

        updateUI(Collections.emptyList());

        searchTask = new Task<>() {
            @Override
            protected Void call() {
                searchRecursiveStreaming(root, query.toLowerCase(), new ArrayList<>());
                return null;
            }
        };

        executor.submit(searchTask);
    }

    private void searchRecursiveStreaming(File dir, String query, List<FileDto> list) {

        if (searchTask.isCancelled()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {

            if (searchTask.isCancelled()) return;

            if(file.getName().startsWith(".")){
                continue;
            }

            if (file.getName().toLowerCase().contains(query)) {

                // 🔥 push result immediately to UI
                Platform.runLater(() -> {
                    var fileDto = new FileDto(file);
                    list.add(fileDto);
                    tableFileList.add(fileDto);
                });
            }

            if (file.isDirectory()) {
                searchRecursiveStreaming(file, query, list);
            }
        }

        this.files = list;

        Platform.runLater(() -> {
            if(!tableFileList.isEmpty()) {
                this.tableView.scrollTo(tableFileList.size() - 1);
            }
        });
    }

    private void searchRecursiveCancelable(File dir, String query, List<File> results) {

        if (currentTask.isCancelled()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {

            if (currentTask.isCancelled()) return;

            if (file.getName().toLowerCase().contains(query)) {
                results.add(file);
            }

            if (file.isDirectory()) {
                searchRecursiveCancelable(file, query, results);
            }
        }
    }

    private List<File> searchFiles(File root, String query) {
        List<File> results = new ArrayList<>();
        searchRecursive(root, query.toLowerCase(), results);
        return results;
    }

    private void searchRecursive(File dir, String query, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.getName().toLowerCase().contains(query)) {
                results.add(file);
            }

            if (file.isDirectory()) {
                searchRecursive(file, query, results);
            }
        }
    }

    public void setDirectory(File selectedDirectory) {
        cancelAllTasks();

        directory = selectedDirectory;
        this.files = getFileDtoList(directory);
        filterResult();
        updateTreeView();
        resetSelection();
    }

    private void cancelAllTasks() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel();
        }
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

            var children = Arrays.stream(Objects.requireNonNull(parent.listFiles())).sorted(Comparator.comparing(File::getName)).toList();

            for (File child : children) {
                if (child.isDirectory()) {
                    item.getChildren().add(new TreeItem<>(child));
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

        updateUI(result);

        resizeColumns();
    }

    private void updateUI(List<FileDto> result) {
        tableFileList.clear();
        tableFileList.addAll(result);
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
            executor.submit(() -> openFile(selectedFile));
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

    public void setTabController(TabManager tabManager) {
        this.tabManager = tabManager;
    }
}
