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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContentTableController {

    @FXML
    public TextField searchTextField;

    @FXML
    public Button refreshDrivesButton;

    @FXML
    private ListView<File> driveListView;

/*    @FXML
    private TreeView<File> fileTreeView;*/

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

    private Map<File, Boolean> directoriesMap = new HashMap<>();

    ObservableList<FileDto> tableFileList;
    List<FileDto> files;

    private TabManager tabManager;


    @FXML
    private void initialize() {

        searchTextField.setOnAction(event -> {
            filterResult();
        });

       /* selectFolderButton.setOnAction((e) -> {
            var selectedDirectory = selectFolder(e);
            setSelectedDirectory(List.of(selectedDirectory));
        });*/

        refreshDrivesButton.setOnAction((e) -> {
           loadExternalDrives();
        });

        if(directoriesMap.isEmpty()){
            loadExternalDrives();
        }
        initExternalDrives();
        initTable();
        initContentListView();
//        initContentFileTree();
    }

    private Set<File> getSelectedDirectories(){
        return directoriesMap.entrySet().stream().filter(Map.Entry::getValue)
                .collect(Collectors.toMap(Map.Entry::getKey, t -> true))
                .keySet();
    }

    private void initExternalDrives(){
        ObservableList<File> drives = FXCollections.observableArrayList();
        drives.addAll(getSelectedDirectories());

        driveListView.setItems(drives);

        // Display only the drive name instead of the full path
        driveListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);

                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });
    }

    private void loadExternalDrives() {
        Path volumes = Paths.get("/Volumes");

        try (Stream<Path> paths = Files.list(volumes)) {
            paths
                    .filter(Files::isDirectory)
                    .filter(this::isExternalDrive)
                    .forEach(it -> directoriesMap.put(it.toFile(), isExternalDrive(it)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean isExternalDrive(Path volume) {
        try {
            Process process = new ProcessBuilder(
                    "diskutil",
                    "info",
                    "-plist",
                    volume.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            String plist = new String(
                    process.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return false;
            }

            // Look for the Internal key in the plist
            var pattern = Pattern.compile(
                    "<key>Internal</key>\\s*<true\\s*/>"
            );

            return !pattern.matcher(plist).find();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    protected File selectFolder(ActionEvent ae) {
        var source = (Node) ae.getSource();
        var stage = source.getScene().getWindow();

        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder File");

        return directoryChooser.showDialog(stage);
    }

    @FXML
    protected void handleClearSearch() {
        searchTextField.clear();
        filterResult();
    }

    @FXML
    protected void handleDeepSearch() {
        if (getSelectedDirectories().isEmpty() || searchTextField.getText().isEmpty()) return;
        performSearchDeepSearch(searchTextField.getText());
    }

    @FXML
    protected void handleDeepSearchNewTab() {
        if (getSelectedDirectories().isEmpty() || searchTextField.getText().isEmpty()) return;
        tabManager.openNewDeepSearchTab(getSelectedDirectories().stream().toList(), searchTextField.getText());
    }

    protected void performSearchDeepSearch(String text) {
        searchTextField.setText(text);

//        searchAsync(directory, searchTextField.getText().toLowerCase());
        searchStreaming(getSelectedDirectories().stream().toList(), searchTextField.getText().toLowerCase());
    }

    @FXML
    protected void handleNavigateUp() {
        if (getSelectedDirectories().isEmpty()) return;

        navigateUp(new FileDto(getSelectedDirectories().stream().toList().getFirst()));
    }

    /*private void initContentFileTree() {
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
                            setSelectedDirectory(List.of(folder));
                        }

                        fileTreeView.getSelectionModel().select(newVal);
                    }
                }
        );
    }*/

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
                handleSelectedFile(contentListView.getSelectionModel().getSelectedItem());
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
                new SimpleStringProperty(cellData.getValue().getDateModified())
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
                    handleSelectedFile(file);
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
                launchInNewTab(fileDto);
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

    private void launchInNewTab(FileDto fileDto) {
        if (fileDto != null) {
            tabManager.openNewTab(new File(fileDto.getPath()));
        }
    }

    private MenuItem getMenuItem(TableRow<FileDto> row) {
        var showEnclosingFolderAction = new MenuItem("Enclosing finder");
        showEnclosingFolderAction.setOnAction(e -> {
            var fileDto = row.getItem();

            if (fileDto == null) {
                return;
            }

            navigateUp(new FileDto(new File(fileDto.getPath()).getParentFile()));
        });
        return showEnclosingFolderAction;
    }

    private void navigateUp(FileDto fileDto) {
        var folder = new File(fileDto.getPath());
        if (folder == null || folder.getParentFile() == null) {
            return;
        }

        setSelectedDirectory(List.of(folder.getParentFile()));
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

    private void searchStreaming(List<File> root, String query) {

        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel();
        }

        updateUI(Collections.emptyList());

        searchTask = new Task<>() {
            @Override
            protected Void call() {
                searchRecursiveStreaming(root, query.toLowerCase(), new HashSet<>());
                return null;
            }
        };

        executor.submit(searchTask);
    }

    private void searchRecursiveStreaming(List<File> dirs, String query, Set<FileDto> list) {

        if (searchTask.isCancelled()) return;

        var files = dirs.stream().flatMap(it -> Arrays.stream(Objects.requireNonNull(it.listFiles())).toList().stream()).toList();
        if (files.isEmpty()) return;

        var querySet = new HashSet<>(Arrays.stream(query.split(" ")).toList());

        if (querySet.isEmpty()) return;

        for (File file : files) {

            if (searchTask.isCancelled()) return;

            if (file.getName().startsWith(".")) {
                continue;
            }
//            var set = new HashSet<>(Arrays.stream(file.getName().toLowerCase().replaceAll(" ", ".").split("\\.")).toList());
            var name = file.getName().toLowerCase().replace(" ", "").replaceAll("\\.", "");
//            if (set.containsAll(querySet)) {
            if (querySet.stream().allMatch(name::contains)) {
                // 🔥 push result immediately to UI
                Platform.runLater(() -> {
                    var fileDto = new FileDto(file, true);
                    list.add(fileDto);
                    tableFileList.add(fileDto);
                });
            } else if (file.isDirectory()) {
                searchRecursiveStreaming(List.of(file), query, list);
            }
        }

        this.files = list.stream().toList();

        Platform.runLater(() -> {
            if (!tableFileList.isEmpty()) {
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
                continue;
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
                continue;
            }

            if (file.isDirectory()) {
                searchRecursive(file, query, results);
            }
        }
    }

    public void setSelectedDirectory(List<File> selectedDirectories) {

        if (Objects.isNull(selectedDirectories) || selectedDirectories.isEmpty()) return;

        selectedDirectories.stream().forEach(it -> {
            directoriesMap.put(it, true);
        });

        cancelAllTasks();
        this.files = getFileDtoList(getSelectedDirectories().stream().toList());
        filterResult();
//        updateTreeView();
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
/*        if(directories.isEmpty()) return;

        File parent = directories.stream().toList().getFirst().getParentFile();

        if (parent == null) return;

        TreeItem<File> rootItem = createNode(parent);
        rootItem.setExpanded(true);

        fileTreeView.setRoot(rootItem);*/
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

    private List<FileDto> getFileDtoList(List<File> directoryList) {
        var result = directoryList.stream().flatMap((e) -> Arrays.stream(Objects.requireNonNull(e.listFiles())))
                .filter(it -> !it.getName().startsWith("."));

        return result.map(FileDto::new).toList();
    }

    private void filterResult() {
        if (this.files == null || this.files.isEmpty()) return;

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
                contentListView.setItems(FXCollections.observableArrayList(getFileDtoList(List.of(file))));
            } else {
                contentListView.setItems(FXCollections.emptyObservableList());
            }
        } else {
            // If it's a file, maybe clear or show just that file
            contentListView.setItems(FXCollections.observableArrayList(newSelection));
        }
    }

    private void handleSelectedFile(FileDto fileDto) {
        File selectedFile = new File(fileDto.getPath());

        if (selectedFile.isFile()) {
            executor.submit(() -> openFile(selectedFile));
        } else if (selectedFile.isDirectory()) {
            if (fileDto.isFromSearch()) {
                launchInNewTab(fileDto);
            } else {
                setSelectedDirectory(List.of(selectedFile));
            }
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
