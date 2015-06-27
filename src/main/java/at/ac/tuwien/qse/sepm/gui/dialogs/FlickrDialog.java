package at.ac.tuwien.qse.sepm.gui.dialogs;

import at.ac.tuwien.qse.sepm.entities.Journey;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.gui.FXMLLoadHelper;
import at.ac.tuwien.qse.sepm.gui.FullscreenWindow;
import at.ac.tuwien.qse.sepm.gui.control.DownloadProgressControl;
import at.ac.tuwien.qse.sepm.gui.control.GoogleMapScene;
import at.ac.tuwien.qse.sepm.gui.control.Keyword;
import at.ac.tuwien.qse.sepm.gui.controller.Menu;
import at.ac.tuwien.qse.sepm.gui.controller.impl.MenuImpl;
import at.ac.tuwien.qse.sepm.gui.util.LatLong;
import at.ac.tuwien.qse.sepm.service.ClusterService;
import at.ac.tuwien.qse.sepm.service.ExifService;
import at.ac.tuwien.qse.sepm.service.FlickrService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.util.Cancelable;
import at.ac.tuwien.qse.sepm.util.ErrorHandler;
import at.ac.tuwien.qse.sepm.util.IOHandler;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FlickrDialog extends ResultDialog<List<com.flickr4java.flickr.photos.Photo>> {

    @FXML
    private BorderPane borderPane;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private FlowPane photosFlowPane;
    @FXML
    private Button searchButton, fullscreenButton, resetButton, importButton, cancelButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane keywordsFlowPane;
    @FXML
    private TextField keywordTextField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox journeysComboBox;
    @FXML
    private GoogleMapScene mapScene;

    private static final Logger logger = LogManager.getLogger();
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    private FlickrService flickrService;
    private ExifService exifService;
    private ClusterService clusterService;
    private IOHandler ioHandler;
    private LatLong actualLatLong;
    private FlickrImageTile lastSelected;
    private ArrayList<Photo> photos = new ArrayList<>();
    private Cancelable searchTask;
    private Cancelable downloadTask;
    private Menu sender;
    private boolean newSearch = false;

    public FlickrDialog(Node origin, String title, FlickrService flickrService, ExifService exifService,
            ClusterService clusterService, IOHandler ioHandler,
            Menu sender) {
        super(origin, title);
        FXMLLoadHelper.load(this, this, FlickrDialog.class, "view/FlickrDialog.fxml");

        this.flickrService = flickrService;
        this.exifService = exifService;
        this.ioHandler = ioHandler;
        this.clusterService = clusterService;
        this.sender = sender;

        flickrService.reset();
        keywordTextField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER && !keywordTextField.getText().isEmpty()) {
                    addKeyword(keywordTextField.getText().trim());
                    keywordTextField.clear();
                    keywordsFlowPane.requestFocus();
                }
            }
        });
        keywordTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(final ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
                if (keywordTextField.getText().length() > 20) {
                    String s = keywordTextField.getText().substring(0, 20);
                    keywordTextField.setText(s);
                }
            }
        });
        scrollPane.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.isControlDown() && event.getCode() == KeyCode.A) {
                    for (Node n : photosFlowPane.getChildren()) {
                        if (n instanceof FlickrImageTile) {
                            ((FlickrImageTile) n).select();
                        }
                    }
                }
            }
        });
        scrollPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                if (!event.isControlDown()) {
                    for (Node n : photosFlowPane.getChildren()) {
                        if(n instanceof FlickrImageTile){
                            if(n==lastSelected) {
                                ((FlickrImageTile) n).select();
                            }
                            else {
                                ((FlickrImageTile) n).deselect();
                            }
                        }
                    }
                }
            }
        });
        journeysComboBox.setCellFactory(new Callback<ListView<Journey>, ListCell<Journey>>() {
            @Override public ListCell<Journey> call(ListView<Journey> l) {
                return new ListCell<Journey>() {
                    @Override protected void updateItem(Journey j, boolean empty) {
                        super.updateItem(j, empty);
                        if(j!=null)
                            setText(j.getName());
                    }
                };
            }
        });
        journeysComboBox.setConverter(new StringConverter<Journey>() {
            @Override public String toString(Journey journey) {
                if (journey == null) {
                    return null;
                } else {
                    return journey.getName();
                }
            }

            @Override public Journey fromString(String string) {
                return null;
            }

        });
        try {
            Journey j = new Journey(-1,"Keine Reise",null,null);
            List<Journey> journeys = clusterService.getAllJourneys();
            journeysComboBox.getItems().add(j);
            journeysComboBox.getItems().addAll(journeys);
        } catch (ServiceException e) {
            logger.debug(e);
        }
        journeysComboBox.getSelectionModel().selectFirst();
        datePicker.setValue(LocalDate.now());
        mapScene.setDoubleClickCallback((position) -> dropMarker(position));
        cancelButton.setOnAction(e -> handleLeaveFlickrDialog());
        importButton.setOnAction(e -> handleImport());
        searchButton.setOnAction(e -> handleSearch());
        fullscreenButton.setOnAction(e -> handleFullscreen());
        resetButton.setOnAction(e -> handleReset());
    }

    private void dropMarker(LatLong ll) {
        if (actualLatLong != null) {
            mapScene.clear();
        }
        this.actualLatLong = ll;
        mapScene.addMarker(ll);
        newSearch = true;
    }

    public void addKeyword(String name) {
        Keyword keyword = new Keyword(name);
        keyword.setOnClosed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                keywordsFlowPane.getChildren().remove(keyword);
                newSearch = true;
            }
        });
        keywordsFlowPane.getChildren().add(keyword);
        newSearch = true;
    }

    private void handleSearch() {
        if(newSearch){
            flickrService.reset();
        }
        newSearch = false;
        searchButton.setDisable(true);
        progressIndicator.setVisible(true);
        importButton.setDisable(true);

        ObservableList<Node> keywords = keywordsFlowPane.getChildren();
        String[] tags = keywords.stream().map(keyword -> ((Keyword)keyword).getName()).toArray(String[]::new);
        double latitude = 0.0;
        double longitude = 0.0;
        boolean useGeoData = false;
        if (actualLatLong != null) {
            latitude = actualLatLong.getLatitude();
            longitude = actualLatLong.getLongitude();
            useGeoData = true;
        }

        try {
            searchTask = flickrService.searchPhotos(tags, latitude, longitude, useGeoData

                    , new Consumer<com.flickr4java.flickr.photos.Photo>() {
                public void accept(com.flickr4java.flickr.photos.Photo photo) {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            FlickrImageTile flickrImageTile = new FlickrImageTile(photo);
                            Photo p = new Photo();
                            p.setPath(tmpDir+photo.getId()+"."+photo.getOriginalFormat());
                            photos.add(p);
                            if(photos.size()==1){
                                fullscreenButton.setDisable(false);
                            }
                            photosFlowPane.getChildren().add(flickrImageTile);
                        }
                    });
                }
            }
                    , new Consumer<Double>() {
                public void accept(Double downloadProgress) {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            if (downloadProgress == 1.0) {
                                reActivateElements();
                            }
                        }
                    });
                }
            }
                    , new ErrorHandler<ServiceException>() {
                public void handle(ServiceException exception) {
                    reActivateElements();
                }
            });
        } catch (ServiceException e) {
            reActivateElements();
        }
    }

    private void handleFullscreen() {
        FullscreenWindow fullscreenWindow = new FullscreenWindow();
        fullscreenWindow.present(photos, photos.get(0));
    }

    private void handleReset(){
        flickrService.reset();
        photos.clear();
        photosFlowPane.getChildren().clear();
        actualLatLong = null;
        mapScene.clear();
        keywordsFlowPane.getChildren().clear();
        keywordTextField.clear();
        fullscreenButton.setDisable(true);
        progressIndicator.setVisible(false);
        if (searchTask != null){
            searchTask.cancel();
            logger.debug("Canceling the download...");
        }
    }

    private void handleImport() {
        flickrService.reset();
        ArrayList<com.flickr4java.flickr.photos.Photo> flickrPhotos = new ArrayList<>();
        for (int i = 0; i<photosFlowPane.getChildren().size(); i++) {
            FlickrImageTile flickrImageTile = (FlickrImageTile) photosFlowPane.getChildren().get(i);
            if(flickrImageTile.isSelected()){
                flickrPhotos.add(flickrImageTile.getFlickrPhoto());
            }
        }
        if(flickrPhotos.isEmpty()){
            logger.debug("No photos selected");
            close();
            return;
        }
        logger.debug("Photos prepared for download {}", flickrPhotos);
        Button flickrButton = ((MenuImpl)sender).getFlickrButton();
        DownloadProgressControl downloadProgressControl = new DownloadProgressControl(flickrButton);
        flickrButton.getGraphic().setStyle("-fx-fill: -tmg-primary;");
        flickrButton.setTooltip(null);
        flickrButton.setOnAction(null);

        try {

            downloadTask = flickrService.downloadPhotos(flickrPhotos,
                    new Consumer<com.flickr4java.flickr.photos.Photo>() {
                        @Override public void accept(com.flickr4java.flickr.photos.Photo flickrPhoto) {
                            Photo p = new Photo();
                            p.setPath(tmpDir+flickrPhoto.getId()+"_o."+flickrPhoto.getOriginalFormat());
                            p.getData().setLatitude(flickrPhoto.getGeoData().getLatitude());
                            p.getData().setLongitude(flickrPhoto.getGeoData().getLongitude());
                            p.getData().setDatetime(datePicker.getValue().atStartOfDay());
                            //TODO set tags
                            //need a setter for that in photo entity
                            //Path path = Paths.get(p.getPath());
                            //TODO method which alters exif data (date, tags, geodata)
                            //exifService.setDateAndGeoData(p);
                            //ioHandler.copyFromTo(Paths.get(p.getPath()),Paths.get(System.getProperty("user.home"),"travelimg/"+path.getFileName()));
                        }
                    }
                    ,
                    new Consumer<Double>() {
                        public void accept(Double downloadProgress) {
                            Platform.runLater(new Runnable() {
                                public void run() {
                                    downloadProgressControl.setProgress(downloadProgress);
                                    if (downloadProgress == 1.0) {
                                        downloadProgressControl.finish(false);
                                    }
                                    logger.debug("Downloading photos from flickr. Progress {}",
                                            downloadProgress);
                                }
                            });

                        }
                    }
                    , new ErrorHandler<ServiceException>() {

                        public void handle(ServiceException exception) {
                            downloadProgressControl.finish(true);
                        }
                    });
        } catch (ServiceException e) {
            downloadProgressControl.finish(true);
        }
        close();
    }

    private void handleLeaveFlickrDialog() {
        handleReset();
        close();
    }

    private void reActivateElements(){
        searchButton.setDisable(false);
        progressIndicator.setVisible(false);
        importButton.setDisable(false);
    }

    /**
     * Widget for one widget in the image grid. Can either be in a selected or an unselected state.
     */
    private class FlickrImageTile extends StackPane {

        private final BorderPane overlay = new BorderPane();
        private BooleanProperty selected = new SimpleBooleanProperty(false);
        private com.flickr4java.flickr.photos.Photo flickrPhoto;

        public FlickrImageTile(com.flickr4java.flickr.photos.Photo flickrPhoto) {
            super();
            this.flickrPhoto = flickrPhoto;
            Image image = null;
            try {
                image = new Image(new FileInputStream(new File(tmpDir+ flickrPhoto.getId()+"."+ flickrPhoto
                        .getOriginalFormat())), 150, 0, true, true);
            } catch (FileNotFoundException ex) {
                logger.error("Could not find photo", ex);
                return;
            }
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            getChildren().add(imageView);

            setAlignment(overlay,Pos.BOTTOM_CENTER);
            FontAwesomeIconView checkIcon = new FontAwesomeIconView();
            checkIcon.setGlyphName("CHECK");
            checkIcon.setStyle("-fx-fill: white");
            overlay.setAlignment(checkIcon, Pos.CENTER_RIGHT);
            overlay.setStyle("-fx-background-color: -tmg-secondary; -fx-max-height: 20px;");
            overlay.setBottom(checkIcon);
            getChildren().add(overlay);
            overlay.setVisible(false);
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent event) {
                    if(!getSelectedProperty().getValue()){
                        select();
                    }
                    else{
                        deselect();
                    }
                }
            });
        }

        public com.flickr4java.flickr.photos.Photo getFlickrPhoto() {
            return flickrPhoto;
        }

        public void select() {
            selected.setValue(true);
            overlay.setVisible(true);
            setStyle("-fx-border-color: -tmg-secondary");
            lastSelected = this;
        }

        public void deselect() {
            selected.setValue(false);
            overlay.setVisible(false);
            setStyle("-fx-border-color: none");
        }

        /**
         * Property which represents if this tile is currently selected or not.
         *
         * @return The selected property.
         */
        public BooleanProperty getSelectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.getValue();
        }

    }

}
