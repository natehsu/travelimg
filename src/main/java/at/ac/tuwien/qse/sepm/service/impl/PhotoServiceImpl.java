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
import at.ac.tuwien.qse.sepm.dao.PhotoDAO;
import at.ac.tuwien.qse.sepm.dao.repo.AsyncPhotoRepository;
import at.ac.tuwien.qse.sepm.dao.repo.Operation;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoRepository;
import at.ac.tuwien.qse.sepm.dao.repo.impl.PollingFileWatcher;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.service.PhotoService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.service.WorkspaceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PhotoServiceImpl implements PhotoService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int REFRESH_RATE = 5;

    @Autowired
    private PhotoDAO photoDAO;
    @Autowired
    private PollingFileWatcher watcher;
    @Autowired
    private AsyncPhotoRepository photoRepository;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ScheduledExecutorService scheduler;

    private Listener listener;
    private ScheduledFuture<?> watcherSchedule = null;

    private final ExecutorService operationExecutor = Executors.newFixedThreadPool(4);


    @Autowired
    private void initializeListeners(AsyncPhotoRepository repository) {
        listener = new Listener();
        repository.addListener((AsyncPhotoRepository.AsyncListener)listener);
        repository.addListener((PhotoRepository.Listener)listener);
    }

    @Override
    public void initializeRepository() {
        LOGGER.debug("Synchronizing repository");

        watcher.getExtensions().add("jpeg");
        watcher.getExtensions().add("jpg");
        watcher.getExtensions().add("JPEG");
        watcher.getExtensions().add("JPG");

        // schedule the update in a separate thread with a little delay
        scheduler.schedule(this::synchronizeAndSchedule, 2, TimeUnit.SECONDS);
    }

    private void synchronizeAndSchedule() {
        // register saved directories
        try {
            workspaceService.getDirectories().forEach(watcher::register);
        } catch (ServiceException ex) {
            LOGGER.error("Failed to register directory", ex);
        }

        // refresh the watcher and synchronize the repository
        watcher.refresh();
        photoRepository.clearQueue();
        try {
            photoRepository.synchronize();
        } catch (DAOException ex) {
            LOGGER.error("Failed to synchronize files", ex);
        }

        // schedule watcher to notify about future changes
        watcherSchedule = scheduler.scheduleAtFixedRate(watcher::refresh, REFRESH_RATE, REFRESH_RATE, TimeUnit.SECONDS);
    }

    public void close() {
        if (watcherSchedule != null) {
            watcherSchedule.cancel(true);
        }
        operationExecutor.shutdown();
    }

    @Override
    public void deletePhotos(Collection<Photo> photos) throws ServiceException {
        if (photos == null) {
            throw new ServiceException("List<Photo> photos is null");
        }
        for (Photo p : photos) {
            LOGGER.debug("Deleting photo {}", p);
            try {
                photoRepository.delete(p.getFile());
            } catch (DAOException e) {
                throw new ServiceException(e);
            }
        }
    }

    @Override
    public List<Photo> getAllPhotos() throws ServiceException {
        LOGGER.debug("Retrieving all photos...");
        try {
            return photoDAO.readAll();
        } catch (DAOException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public List<Photo> getAllPhotos(Predicate<Photo> filter) throws ServiceException {
        LOGGER.debug("Entering getAllPhotos with {}", filter);
        return getAllPhotos()
                .stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    @Override
    public void editPhoto(Photo photo) throws ServiceException {
        LOGGER.debug("Entering editPhoto with {}", photo);

        try {
            photoRepository.update(photo);
            LOGGER.info("Successfully updated {}", photo);
        } catch (DAOException ex) {
            LOGGER.error("Updating {} failed due to DAOException", photo);
            throw new ServiceException("Could update photo.", ex);
        }

        LOGGER.debug("Leaving editPhoto with {}", photo);
    }

    @Override
    public void subscribeCreate(Consumer<Photo> callback) {
        photoRepository.addListener(new PhotoRepository.Listener() {
            @Override public void onCreate(PhotoRepository repository, Path file) {
                LOGGER.info("created {}", file);
                try {
                    Photo photo = repository.read(file);
                    callback.accept(photo);
                } catch (DAOException ex) {
                    LOGGER.error("Failed to read photo {}", file);
                }
            }
        });
    }

    @Override
    public void subscribeUpdate(Consumer<Photo> callback) {
        photoRepository.addListener(new PhotoRepository.Listener() {
            @Override public void onUpdate(PhotoRepository repository, Path file) {
                LOGGER.info("updated {}", file);
                try {
                    Photo photo = repository.read(file);
                    callback.accept(photo);
                } catch (DAOException ex) {
                    LOGGER.error("Failed to read photo {}", file);
                }
            }
        });
    }

    @Override
    public void subscribeDelete(Consumer<Path> callback) {
        photoRepository.addListener(new PhotoRepository.Listener() {
            @Override public void onDelete(PhotoRepository repository, Path file) {
                LOGGER.info("deleted {}", file);
                callback.accept(file);
            }
        });
    }

    private class Listener implements
            AsyncPhotoRepository.AsyncListener,
            PhotoRepository.Listener {

        @Override public void onError(PhotoRepository repository, DAOException error) {
            LOGGER.error("repository error {}", error);
        }

        @Override public void onError(AsyncPhotoRepository repository, Operation operation, DAOException error) {
            LOGGER.warn("failed operation {}", operation);
            LOGGER.error("operation error {}", error);
        }

        @Override public void onQueue(AsyncPhotoRepository repository, Operation operation) {
            LOGGER.info("queued {}", operation);
            LOGGER.info("queue length {}", repository.getQueue().size());
            operationExecutor.execute(repository::completeNext);
        }
    }
}
