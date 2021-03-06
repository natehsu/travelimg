package at.ac.tuwien.qse.sepm.gui;

/*
 * Copyright (c) 2015 Lukas Eibensteiner
 * Copyright (c) 2015 Kristoffer Kleine
 * Copyright (c) 2015 Branko Majic
 * Copyright (c) 2015 Enri Miho
 * Copyright (c) 2015 David Peherstorfer
 * Copyright (c) 2015 Marian Stoschitzky
 * Copyright (c) 2015 Christoph Wasylewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import at.ac.tuwien.qse.sepm.entities.PhotoSlide;
import at.ac.tuwien.qse.sepm.entities.Slide;
import at.ac.tuwien.qse.sepm.entities.Slideshow;
import at.ac.tuwien.qse.sepm.gui.slide.SlideView;
import at.ac.tuwien.qse.sepm.gui.util.ColorUtils;
import at.ac.tuwien.qse.sepm.gui.util.ImageCache;
import at.ac.tuwien.qse.sepm.gui.util.ImageSize;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PresentationWindow extends StackPane {

    private static final Logger logger = LogManager.getLogger();

    private Stage stage;
    private Scene scene;
    private boolean isPaused = false;

    private int activeIndex = 0;
    private Timeline timeline = null;

    private Slideshow slideshow;
    private List<Slide> slides;

    public PresentationWindow(Slideshow slideshow) {
        FXMLLoadHelper.load(this, this, PresentationWindow.class, "view/PresentationWindow.fxml");

        getStyleClass().add("fullscreen");

        this.slideshow = slideshow;
        this.slides = slideshow.getAllSlides().stream()
                .sorted((s1, s2) -> s1.getOrder().compareTo(s2.getOrder()))
                .collect(Collectors.toList());
    }

    @FXML
    private void initialize() {
        this.stage = new Stage();
        this.scene = new Scene(this);

        stage.setScene(scene);

        // TODO: replace by css
        Background background = new Background(new BackgroundFill(Paint.valueOf("black"), null, null));
        setBackground(background);

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            public void handle(final KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.RIGHT) {
                    showNextSlide(null);
                }
                if (keyEvent.getCode() == KeyCode.LEFT) {
                    logger.debug("Button pressed");
                    showPrevSlide(null);
                }
                if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    close();
                }
                if (keyEvent.getCode() == KeyCode.SPACE) {
                    if (!isPaused) {
                        timeline.pause();
                        isPaused = true;
                    } else {
                        timeline.play();
                        isPaused = false;
                    }
                }
            }
        });
    }

    public void present() {
        loadSlide();

        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();

        timeline = new Timeline(new KeyFrame(Duration.seconds(slideshow.getDurationBetweenPhotos()), this::showNextSlide));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void close() {
        if (timeline != null) {
            timeline.stop();
        }

        stage.close();
    }

    private void showPrevSlide(ActionEvent event) {
        activeIndex = Math.max(0, activeIndex - 1);
        loadSlide();
    }

    private void showNextSlide(ActionEvent event) {
        if (activeIndex == slides.size() - 1)
            return;

        activeIndex = Math.min(slides.size() - 1, activeIndex + 1);
        loadSlide();
    }

    private void loadSlide() {
        if (slides.size() == 0 || slides.size() <= activeIndex) {
            // out of bounds
            return;
        }

        int width = (int)Screen.getPrimary().getVisualBounds().getWidth();
        int height = (int)Screen.getPrimary().getVisualBounds().getHeight();

        SlideView slideView = SlideView.of(slides.get(activeIndex), width, height);

        getChildren().clear();
        getChildren().add(slideView);
    }
}
