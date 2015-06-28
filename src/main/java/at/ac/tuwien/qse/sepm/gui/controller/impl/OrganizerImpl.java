package at.ac.tuwien.qse.sepm.gui.controller.impl;

import at.ac.tuwien.qse.sepm.entities.*;
import at.ac.tuwien.qse.sepm.gui.control.FilterList;
import at.ac.tuwien.qse.sepm.gui.controller.Inspector;
import at.ac.tuwien.qse.sepm.gui.controller.Organizer;
import at.ac.tuwien.qse.sepm.gui.dialogs.ErrorDialog;
import at.ac.tuwien.qse.sepm.gui.dialogs.InfoDialog;
import at.ac.tuwien.qse.sepm.gui.util.BufferedBatchOperation;
import at.ac.tuwien.qse.sepm.service.*;
import at.ac.tuwien.qse.sepm.service.impl.PhotoFilter;
import at.ac.tuwien.qse.sepm.service.impl.PhotoPathFilter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Controller for organizer view which is used for browsing photos by month.
 */
public class OrganizerImpl implements Organizer {

    private static final Logger LOGGER = LogManager.getLogger(OrganizerImpl.class);
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy MMM");

    @Autowired private PhotographerService photographerService;
    @Autowired private ClusterService clusterService;
    @Autowired private Inspector<Photo> inspectorController;
    @Autowired private TagService tagService;
    @Autowired private WorkspaceService workspaceService;
    @Autowired private PhotoService photoService;

    @FXML private BorderPane root;
    @FXML private VBox filterContainer;
    @FXML private FilterList<Rating> ratingListView;
    @FXML private FilterList<Tag> categoryListView;
    @FXML private FilterList<Photographer> photographerListView;
    @FXML private FilterList<Journey> journeyListView;
    @FXML private FilterList<Place> placeListView;

    private ToggleButton folderViewButton = new ToggleButton("Ordneransicht");
    private ToggleButton filterViewButton = new ToggleButton("Filteransicht");

    @FXML private TreeView<String> filesTree;

    private HBox buttonBox;
    private ChoiceBox<Path> directoryChoiceBox = new ChoiceBox<>();
    private PhotoFilter usedFilter = new PhotoFilter();
    private PhotoFilter photoFilter = usedFilter;
    private PhotoFilter folderFilter = new PhotoPathFilter();
    private Runnable filterChangeCallback;
    private boolean suppressChangeEvent = false;

    private BufferedBatchOperation<Photo> addOperation;

    @Autowired public void setScheduler(ScheduledExecutorService scheduler) {
        addOperation = new BufferedBatchOperation<>(photos -> {
            Platform.runLater(() -> {/*TODO*/});
        }, scheduler);
    }

    @Override public void setFilterChangeAction(Runnable callback) {
        LOGGER.debug("setting usedFilter change action");
        filterChangeCallback = callback;
    }

    @Override public PhotoFilter getUsedFilter() {
        return usedFilter;
    }

    @FXML private void initialize() {
        initializeFilesTree();

        folderViewButton.setOnAction(event -> folderViewClicked());
        filterViewButton.setOnAction(event -> filterViewClicked());

        ToggleGroup toggleGroup = new ToggleGroup();
        filterViewButton.setToggleGroup(toggleGroup);
        folderViewButton.setToggleGroup(toggleGroup);

        buttonBox = new HBox(filterViewButton, folderViewButton);
        buttonBox.setAlignment(Pos.CENTER);

        ratingListView = new FilterList<>(value -> {
            switch (value) {
                case GOOD:
                    return "Gut";
                case NEUTRAL:
                    return "Neutral";
                case BAD:
                    return "Schlecht";
                default:
                    return "Unbewertet";
            }
        });
        ratingListView.setTitle("Bewertungen");
        ratingListView.setChangeHandler(this::handleRatingsChange);
        categoryListView = new FilterList<>(value -> {
            if (value == null)
                return "Nicht kategorisiert";
            return value.getName();
        });
        categoryListView.setTitle("Kategorien");
        categoryListView.setChangeHandler(this::handleCategoriesChange);
        photographerListView = new FilterList<>(value -> value.getName());
        photographerListView.setTitle("Fotografen");
        photographerListView.setChangeHandler(this::handlePhotographersChange);
        journeyListView = new FilterList<>(value -> {
            if (value == null)
                return "Keiner Reise zugeordnet";
            return value.getName();
        });
        journeyListView.setTitle("Reisen");
        journeyListView.setChangeHandler(this::handleJourneysChange);
        placeListView = new FilterList<Place>(value -> {
            if (value == null)
                return "Keinem Ort zugeordnet";
            return value.getCountry() + ", " + value.getCity();
        });
        placeListView.setTitle("Orte");
        placeListView.setChangeHandler(this::handlePlacesChange);

        filterContainer.getChildren()
                .addAll(buttonBox, ratingListView, categoryListView, photographerListView,
                        journeyListView, placeListView);

        refreshLists();
        resetFilter();

        tagService.subscribeTagChanged(this::refreshCategoryList);
        clusterService.subscribeJourneyChanged(this::refreshJourneyList);
        clusterService.subscribePlaceChanged(this::refreshPlaceList);
        photographerService.subscribeChanged(this::refreshPhotographerList);

        photoService.subscribeCreate(this::handlePhotoAdded);
    }

    private void initializeFilesTree() {
        filesTree = new TreeView<>();
        filesTree.setOnMouseClicked(event -> handleFolderChange());
        filesTree.setCellFactory(treeView -> {
            HBox hbox = new HBox();
            hbox.setMaxWidth(200);
            hbox.setPrefWidth(200);
            hbox.setSpacing(7);

            FontAwesomeIconView openFolderIcon = new FontAwesomeIconView(
                    FontAwesomeIcon.FOLDER_OPEN_ALT);
            openFolderIcon.setTranslateY(7);
            FontAwesomeIconView closedFolderIcon = new FontAwesomeIconView(
                    FontAwesomeIcon.FOLDER_ALT);
            closedFolderIcon.setTranslateY(7);

            Label dirName = new Label();
            dirName.setMaxWidth(150);

            FontAwesomeIconView removeIcon = new FontAwesomeIconView(FontAwesomeIcon.REMOVE);

            Tooltip deleteToolTip = new Tooltip();
            deleteToolTip.setText("Verzeichnis aus Workspace entfernen");

            Button button = new Button(null, removeIcon);
            button.setTooltip(deleteToolTip);
            button.setTranslateX(8);

            return new TreeCell<String>() {
                @Override public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null) {
                        setGraphic(null);
                        setText(null);
                    } else if (getTreeItem() instanceof FilePathTreeItem) {
                        hbox.getChildren().clear();
                        dirName.setText(item);
                        if (getTreeItem().isExpanded()) {
                            hbox.getChildren().add(openFolderIcon);
                        } else {
                            hbox.getChildren().add(closedFolderIcon);
                        }
                        hbox.getChildren().add(dirName);
                        if (getTreeItem().getParent().equals(filesTree.getRoot())) {
                            String path = ((FilePathTreeItem) getTreeItem()).getFullPath();
                            button.setOnAction(event -> handleDeleteDirectory(Paths.get(path)));
                            hbox.getChildren().add(button);
                        }
                        setGraphic(hbox);
                    }
                }
            };
        });
    }

    private void handleDeleteDirectory() {
        Path directory = directoryChoiceBox.getSelectionModel().getSelectedItem();
        handleDeleteDirectory(directory);
    }

    private void handleDeleteDirectory(Path directory) {
        if (directory != null) {
            try {
                workspaceService.removeDirectory(directory);
            } catch (ServiceException ex) {
                LOGGER.error("Could not delete directory");
            }
        }
    }

    @Override public void setWorldMapPlace(Place place) {
        placeListView.uncheckAll();
        placeListView.check(place);

        handlePlacesChange(placeListView.getChecked());
    }

    // This Method let's the User switch to the usedFilter-view
    private void filterViewClicked() {
        filterContainer.getChildren().clear();
        filterContainer.getChildren()
                .addAll(buttonBox, ratingListView, categoryListView, photographerListView,
                        journeyListView, placeListView);
        usedFilter = photoFilter;
        handleFilterChange();
    }

    // This Method let's the User switch to the folder-view
    private void folderViewClicked() {
        LOGGER.debug("Switch view");

        filterContainer.getChildren().clear();
        filterContainer.getChildren().addAll(buttonBox, filesTree);
        VBox.setVgrow(filesTree, Priority.ALWAYS);

        usedFilter = new PhotoPathFilter();
        handleFilterChange();
    }

    private void buildTreeView() {
        Collection<Path> workspaceDirectories;
        try {
            workspaceDirectories = workspaceService.getDirectories();
        } catch (ServiceException ex) {
            workspaceDirectories = new LinkedList<>();
        }

        FilePathTreeItem root = new FilePathTreeItem(Paths.get("workspace"));
        root.setExpanded(true);
        filesTree.setRoot(root);
        filesTree.setShowRoot(false);

        for (Path dirPath : workspaceDirectories) {
            findFiles(dirPath.toFile(), root);
        }

        directoryChoiceBox.setItems(FXCollections.observableArrayList(workspaceDirectories));
        filterContainer.getChildren().addAll(buttonBox, filesTree);
        VBox.setVgrow(filesTree, Priority.ALWAYS);
    }

    private void findFiles(File dir, FilePathTreeItem parent) {
        FilePathTreeItem rootNode = new FilePathTreeItem(dir.toPath());
        rootNode.setExpanded(true);
        try {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    System.out.println("directory:" + file.getCanonicalPath());
                    findFiles(file, rootNode);
                }
            }
            if (parent == null) {
                filesTree.setRoot(rootNode);
            } else {
                parent.getChildren().add(rootNode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleFilterChange() {
        LOGGER.debug("usedFilter changed");
        if (filterChangeCallback == null || suppressChangeEvent)
            return;
        filterChangeCallback.run();
    }

    private void handleFolderChange() {
        LOGGER.debug("handle folder");
        FilePathTreeItem item = (FilePathTreeItem) filesTree.getSelectionModel().getSelectedItem();
        if (item != null) {
            if (usedFilter instanceof PhotoPathFilter)
                ((PhotoPathFilter) usedFilter).setIncludedPath(Paths.get(item.getFullPath()));
            handleFilterChange();
        }
    }

    private void handleRatingsChange(List<Rating> values) {
        LOGGER.debug("rating usedFilter changed");
        usedFilter.getIncludedRatings().clear();
        usedFilter.getIncludedRatings().addAll(values);
        handleFilterChange();
    }

    private void handleCategoriesChange(List<Tag> values) {
        LOGGER.debug("category usedFilter changed");
        usedFilter.setUntaggedIncluded(values.contains(null));
        values.remove(null);
        usedFilter.getIncludedCategories().clear();
        usedFilter.getIncludedCategories().addAll(values);
        handleFilterChange();
    }

    private void handlePhotographersChange(List<Photographer> values) {
        LOGGER.debug("photographer usedFilter changed");
        usedFilter.getIncludedPhotographers().clear();
        usedFilter.getIncludedPhotographers().addAll(values);
        handleFilterChange();
    }

    private void handleJourneysChange(List<Journey> values) {
        LOGGER.debug("journey usedFilter changed");
        usedFilter.getIncludedJourneys().clear();
        usedFilter.getIncludedJourneys().addAll(values);
        handleFilterChange();
    }

    private void handlePlacesChange(List<Place> values) {
        LOGGER.debug("place usedFilter changed");
        usedFilter.getIncludedPlaces().clear();
        usedFilter.getIncludedPlaces().addAll(values);
        handleFilterChange();
    }

    private void refreshLists() {
        LOGGER.debug("refreshing usedFilter");

        ratingListView.setValues(getAllRatings());
        categoryListView.setValues(getAllCategories());
        photographerListView.setValues(getAllPhotographers());
        journeyListView.setValues(getAllJourneys());
        placeListView.setValues(getAllPlaces());
    }

    private List<Rating> getAllRatings() {
        LOGGER.debug("fetching ratings");
        List<Rating> list = new LinkedList<Rating>();
        list.add(Rating.GOOD);
        list.add(Rating.NEUTRAL);
        list.add(Rating.BAD);
        list.add(Rating.NONE);
        LOGGER.debug("fetching ratings succeeded with {} items", list.size());
        return list;
    }

    private List<Tag> getAllCategories() {
        LOGGER.debug("fetching categories");
        try {
            List<Tag> list = tagService.getAllTags();
            LOGGER.debug("fetching categories succeeded with {} items", list.size());
            list.sort((a, b) -> a.getName().compareTo(b.getName()));
            list.add(null);
            return list;
        } catch (ServiceException ex) {
            LOGGER.error("fetching categories failed", ex);
            ErrorDialog.show(root, "Fehler beim Laden",
                    "Foto-Kategorien konnten nicht geladen werden.");
            return new ArrayList<>();
        }
    }

    private List<Photographer> getAllPhotographers() {
        LOGGER.debug("fetching photographers");
        try {
            List<Photographer> list = photographerService.readAll();
            LOGGER.debug("fetching photographers succeeded with {} items", list.size());
            return list;
        } catch (ServiceException ex) {
            LOGGER.error("fetching photographers failed", ex);
            ErrorDialog.show(root, "Fehler beim Laden", "Fotografen konnten nicht geladen werden.");
            return new ArrayList<>();
        }
    }

    private List<Journey> getAllJourneys() {
        LOGGER.debug("fetching journeys");
        try {
            List<Journey> list = clusterService.getAllJourneys();
            list.sort((a, b) -> a.getName().compareTo(b.getName()));
            list.add(null);
            return list;
        } catch (ServiceException ex) {
            LOGGER.error("fetching journeys failed", ex);
            InfoDialog dialog = new InfoDialog(root, "Fehler");
            dialog.setError(true);
            dialog.setHeaderText("Fehler beim Laden");
            dialog.setContentText("Reisen konnten nicht geladen werden.");
            dialog.showAndWait();
            return new ArrayList<>();
        }
    }

    private List<Place> getAllPlaces() {
        LOGGER.debug("fetching journeys");
        try {
            List<Place> list = clusterService.getAllPlaces();
            //list.sort((a, b) -> a.getName().compareTo(b.getName()));
            return list;
        } catch (ServiceException ex) {
            LOGGER.error("fetching journeys failed", ex);
            ErrorDialog.show(root, "Fehler beim Laden", "Reisen konnten nicht geladen werden.");
            return new ArrayList<>();
        }
    }

    private void resetFilter() {
        // don't update the usedFilter until all list views have been reset
        Runnable savedCallback = filterChangeCallback;
        filterChangeCallback = null;

        refreshLists();
        inspectorController.refresh();
        categoryListView.checkAll();
        ratingListView.checkAll();
        photographerListView.checkAll();
        journeyListView.checkAll();
        placeListView.checkAll();

        // restore the callback and handle the change
        filterChangeCallback = savedCallback;
    }

    private void refreshCategoryList(Tag tag) {
        Platform.runLater(() -> {
            suppressChangeEvent = true;
            List<Tag> all = categoryListView.getValues();
            all.add(tag);
            List<Tag> checked = categoryListView.getChecked();
            checked.add(tag);
            categoryListView.setValues(all);
            categoryListView.checkAll(checked);
            LOGGER.info("refresh tag-filterlist");
            suppressChangeEvent = false;
        });
    }

    private void refreshJourneyList(Journey journey) {
        Platform.runLater(() -> {
            suppressChangeEvent = true;
            List<Journey> all = journeyListView.getValues();
            all.add(journey);
            List<Journey> checked = journeyListView.getChecked();
            checked.add(journey);
            journeyListView.setValues(all);
            journeyListView.checkAll(checked);
            LOGGER.info("refresh journey-filterlist");
            suppressChangeEvent = false;
        });
    }

    private void refreshPlaceList(Place place) {
        Platform.runLater(() -> {
            suppressChangeEvent = true;
            List<Place> all = placeListView.getValues();
            all.add(place);
            List<Place> checked = placeListView.getChecked();
            checked.add(place);
            placeListView.setValues(all);
            placeListView.checkAll(checked);
            LOGGER.info("refresh place-filterlist");
            suppressChangeEvent = false;
        });
    }

    private void refreshPhotographerList(Photographer photographer) {
        Platform.runLater(() -> {
            suppressChangeEvent = true;
            List<Photographer> all = photographerListView.getValues();
            all.add(photographer);
            List<Photographer> checked = photographerListView.getChecked();
            checked.add(photographer);
            photographerListView.setValues(all);
            photographerListView.checkAll(checked);
            LOGGER.info("refresh photographer-filterlist");
            suppressChangeEvent = false;
        });
    }

    private void handlePhotoAdded(Photo photo) {
        addOperation.add(photo);
    }
}
