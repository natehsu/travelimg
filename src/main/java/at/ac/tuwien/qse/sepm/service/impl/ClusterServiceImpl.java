package at.ac.tuwien.qse.sepm.service.impl;

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

import at.ac.tuwien.qse.sepm.dao.DAOException;
import at.ac.tuwien.qse.sepm.dao.JourneyDAO;
import at.ac.tuwien.qse.sepm.dao.PhotoDAO;
import at.ac.tuwien.qse.sepm.dao.PlaceDAO;
import at.ac.tuwien.qse.sepm.dao.EntityWatcher;
import at.ac.tuwien.qse.sepm.entities.Journey;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.entities.Place;
import at.ac.tuwien.qse.sepm.entities.validators.ValidationException;
import at.ac.tuwien.qse.sepm.service.ClusterService;
import at.ac.tuwien.qse.sepm.service.GeoService;
import at.ac.tuwien.qse.sepm.service.PhotoService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ClusterServiceImpl implements ClusterService {

    private static final Logger logger = LogManager.getLogger(ClusterServiceImpl.class);

    @Autowired private GeoService geoService;
    @Autowired private PhotoDAO photoDAO;
    @Autowired private JourneyDAO journeyDAO;
    @Autowired private PlaceDAO placeDAO;
    @Autowired private PhotoService photoService;

    private Collection<Consumer<Place>> placesListeners = new LinkedList<>();
    private Collection<Consumer<Journey>> journeyListeners = new LinkedList<>();

    @Autowired private void setPlaceWatcher(EntityWatcher<Place> watcher) {
        watcher.subscribeAdded(this::placeAdded);
    }

    @Autowired private void setJourneyWatcher(EntityWatcher<Journey> watcher) {
        watcher.subscribeAdded(this::journeyAdded);
    }

    @Override public List<Journey> getAllJourneys() throws ServiceException {
        try {
            return journeyDAO.readAll();
        } catch (DAOException ex) {
            logger.error("Failed to get all journeys", ex);
            throw new ServiceException("Failed to get all journeys", ex);
        }
    }

    @Override public List<Place> getAllPlaces() throws ServiceException {
        try {
            return placeDAO.readAll();
        } catch (DAOException ex) {
            logger.error("Failed to get all places", ex);
            throw new ServiceException("Failed to get all places", ex);
        }
    }

    @Override public List<Place> getPlacesByJourneyChronological(Journey journey)
            throws ServiceException {
        List<Photo> photos;

        try {
            photos = photoDAO.readPhotosByJourney(journey);
        } catch (DAOException ex) {
            logger.error("Failed to read all photos by journey", ex);
            throw new ServiceException("Failed to read all photos by journey", ex);
        }

        Map<Place, LocalDateTime> minTimeByPlace = new HashMap<>();

        for (Photo photo : photos) {
            Place place = photo.getData().getPlace();
            LocalDateTime time = photo.getData().getDatetime();

            if (!minTimeByPlace.containsKey(place)) {
                minTimeByPlace.put(place, time);
            } else if (minTimeByPlace.get(place).isAfter(time)) {
                minTimeByPlace.put(place, time);
            }
        }

        return minTimeByPlace.keySet().stream()
                .sorted((p1, p2) -> minTimeByPlace.get(p1).compareTo(minTimeByPlace.get(p2)))
                .collect(Collectors.toList());
    }

    @Override public Place addPlace(Place place) throws ServiceException {
        try {
            place = placeDAO.create(place);
            placeAdded(place);
            return place;
        } catch (DAOException ex) {
            logger.error("Place-creation for {} failed.", place);
            throw new ServiceException("Creation of Place failed.", ex);
        } catch (ValidationException e) {
            throw new ServiceException("Failed to validate entity", e);
        }
    }

    @Override public Journey addJourney(Journey journey) throws ServiceException {
        try {
            journey =  journeyDAO.create(journey);
            journeyAdded(journey);
            return journey;
        } catch (DAOException ex) {
            logger.error("Journey-creation for {} failed.", journey);
            throw new ServiceException("Creation of journey failed.", ex);
        } catch (ValidationException ex) {
            throw new ServiceException("Failed to validate entity: " + ex.getMessage(), ex);
        }
    }

    @Override public List<Place> clusterJourney(Journey journey) throws ServiceException {
        logger.debug("clustering journey {}", journey);

        List<Photo> photos;
        List<Place> places = new ArrayList<>();

        addJourney(journey);

        try {
            photos = photoDAO.readPhotosBetween(journey.getStartDate(), journey.getEndDate());
        } catch (DAOException e) {
            logger.error("Failed to read photos of journey", e);
            throw new ServiceException("Failed to read photos of journey", e);
        }

        // attach a place to each photo
        for (Photo photo : photos) {
            final double latitude = photo.getData().getLatitude();
            final double longitude = photo.getData().getLongitude();

            double epsilon = 1.0;
            Optional<Place> place = places.stream().filter(p -> Math.abs(p.getLatitude() - latitude)
                    < epsilon)   // find an existing place in lateral proximity
                    .filter(p -> Math.abs(p.getLongitude() - longitude)
                            < epsilon) // find an existing place in longitudinal proximity
                    .findFirst();

            if (!place.isPresent()) {
                // if no we don't already know a place in close proximity look it up
                place = Optional.of(lookupPlace(photo));
                places.add(place.get());
            }

            photo.getData().setPlace(place.get());
            photo.getData().setJourney(journey);
            photoService.editPhoto(photo);
        }
        return places;
    }

    @Override public Place getPlaceNearTo(Photo photo) throws ServiceException{

        final double latitude = photo.getData().getLatitude();
        final double longitude = photo.getData().getLongitude();

        List<Place> places = getAllPlaces();
        double epsilon = 1.0;
        Optional<Place> place = places.stream().filter(p -> Math.abs(p.getLatitude() - latitude)
                    < epsilon)   // find an existing place in lateral proximity
                    .filter(p -> Math.abs(p.getLongitude() - longitude)
                            < epsilon) // find an existing place in longitudinal proximity
                    .findFirst();

        if (!place.isPresent()) {
            // if we don't already know a place in close proximity look it up
            return lookupPlace(photo);
        }
        return place.get();
    }

    private Place lookupPlace(Photo photo) throws ServiceException {
        Place place = geoService
                .getPlaceByGeoData(photo.getData().getLatitude(), photo.getData().getLongitude());

        logger.debug("New unknown place cluster: {}", place);

        return addPlace(place);
    }

    private void placeAdded(Place place) {

        placesListeners.forEach(l -> l.accept(place));
    }

    private void journeyAdded(Journey journey) {
        journeyListeners.forEach(l -> l.accept(journey));
    }

    @Override public void subscribePlaceChanged(Consumer<Place> callback) {
        placesListeners.add(callback);
    }

    @Override public void subscribeJourneyChanged(Consumer<Journey> callback) {
        journeyListeners.add(callback);

    }
}