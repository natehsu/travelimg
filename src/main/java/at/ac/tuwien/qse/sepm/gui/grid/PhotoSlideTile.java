package at.ac.tuwien.qse.sepm.gui.grid;

import at.ac.tuwien.qse.sepm.entities.PhotoSlide;
import at.ac.tuwien.qse.sepm.gui.control.SmartImage;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;

public class PhotoSlideTile extends SlideTileBase<PhotoSlide> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final SmartImage imageView = new SmartImage();

    public PhotoSlideTile(PhotoSlide slide) {
        super(slide);
        getStyleClass().add("photo");
        getChildren().add(0, imageView);

        String url;
        try {
            url = slide.getPhoto().getFile().toUri().toURL().toString();
        } catch (MalformedURLException ex) {
            LOGGER.warn("could not convert path to URL {}", slide.getPhoto().getPath());
            LOGGER.error(ex);
            return;
        }

        Image image = new Image(url, true);
        imageView.setImage(image);
    }
}
