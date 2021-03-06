package at.ac.tuwien.qse.sepm.dao.repo.impl;

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

import at.ac.tuwien.qse.sepm.dao.repo.FileWatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PollingFileWatcher implements FileWatcher {

    private static final Logger LOGGER = LogManager.getLogger();

    private final FileManager fileManager;

    private final Set<String> extensions = new HashSet<>();
    private final Set<Path> directories = new HashSet<>();
    private final Set<Path> watched = new HashSet<>();
    private final Map<Path, LocalDateTime> modified = new HashMap<>();
    private final Collection<Listener> listeners = new LinkedList<>();

    public PollingFileWatcher() {
        this(new PhysicalFileManager());
    }

    public PollingFileWatcher(FileManager fileManager) {
        if (fileManager == null) throw new IllegalArgumentException();
        this.fileManager = fileManager;
    }

    /**
     * Get the a modifiable set of file extensions recognized by this watcher. Only if a file ends
     * with a period and one of these extensions will it trigger notifications.
     *
     * @return set of recognized extensions
     */
    public Set<String> getExtensions() {
        return extensions;
    }

    @Override public boolean recognizes(Path file) {
        return checkExtension(file) && checkDirectories(file);
    }

    @Override public Collection<Path> index() {
        return new HashSet<>(watched);
    }

    @Override public void register(Path directory) {
        if (directory == null) throw new IllegalArgumentException();
        LOGGER.debug("registering directory {}", directory);

        if (directories.contains(directory)) {
            LOGGER.debug("already registered directory {}", directory);
            return;
        }

        directories.add(directory);
        LOGGER.debug("registered directory {}", directory);
    }

    @Override public void unregister(Path directory) {
        directories.remove(directory);
    }

    @Override public void addListener(Listener listener) {
        if (listener == null) throw new IllegalArgumentException();
        listeners.add(listener);
        LOGGER.debug("added listener {}", listener);
    }

    @Override public void removeListener(Listener listener) {
        if (listener == null) throw new IllegalArgumentException();
        listeners.remove(listener);
        LOGGER.debug("removed listener {}", listener);
    }

    /**
     * Polls the file system for changes since the last refresh.
     *
     * If this is the first refresh, all existing files trigger create events.
     */
    public void refresh() {
        LOGGER.debug("currently watching {} files", watched.size());
        LOGGER.debug("refreshing");
        Set<Path> physicalFiles = listFiles();
        Set<Path> files = new HashSet<>(physicalFiles);
        files.addAll(watched);
        LOGGER.debug("now watching {} files", watched.size());
        files.forEach(file -> refresh(physicalFiles, file));
    }

    // Refreshes a single file.
    private void refresh(Set<Path> physicalFiles, Path file) {
        boolean isPhysical = physicalFiles.contains(file);
        boolean isKnown = watched.contains(file);
        if (isPhysical && isKnown) {
            LocalDateTime knownModified = modified.get(file);
            LocalDateTime physicalModified = getLastModified(file);
            if (knownModified.isBefore(physicalModified)) {
                listeners.forEach(l -> l.onUpdate(this, file));
                modified.put(file, physicalModified);
            }
        } else if (isPhysical) {
            watched.add(file);
            modified.put(file, LocalDateTime.now());
            listeners.forEach(l -> l.onCreate(this, file));
        } else if (isKnown) {
            watched.remove(file);
            modified.remove(file);
            listeners.forEach(l -> l.onDelete(this, file));
        }
    }

    // Get the last modification time of a physical file.
    private LocalDateTime getLastModified(Path file) {
        try {
            FileTime time = fileManager.getLastModifiedTime(file);
            return LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
        } catch (IOException ex) {
            LOGGER.warn("failed getting last modification time for file {}", file);
            // NOTE: Assume it has changed.
            return LocalDateTime.MAX;
        }
    }

    // Lists all files in the registered directories.
    private Set<Path> listFiles() {
        Set<Path> files = new HashSet<>();
        LOGGER.debug("listing files from {} directories", directories.size());
        directories.forEach(dir -> collectFiles(dir, files));
        LOGGER.debug("found {} files", files.size());
        return files;
    }

    // Recursively collects all files in a directory into a set.
    private void collectFiles(Path directory, Set<Path> result) {
        try {
            // NOTE: Stream must always be closed.
            // Otherwise the directory remains open in the application.
            Stream<Path> stream = fileManager.list(directory);
            Collection<Path> paths = stream.collect(Collectors.toList());
            stream.close();
            for (Path path : paths) {
                if (fileManager.isDirectory(path)) {
                    collectFiles(path, result);
                }
                if (fileManager.isFile(path) && checkExtension(path)) {
                    result.add(path);
                }
            }
        } catch (NoSuchFileException ex) {
            LOGGER.info("can not find directory {}", directory);
        } catch (IOException ex) {
            // NOTE: The path may not exist, or it may be a file, or something else. There is a lot
            // that can happen from outside the application, so we just have to ignore such errors.
            LOGGER.warn("failed listing contents for directory {}", directory);
            LOGGER.error("error: ", ex);
        }
    }

    private boolean checkExtension(Path file) {
        String fileString = file.toString();
        for (String ext : extensions) {
            if (fileString.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDirectories(Path file) {
        for (Path dir : directories) {
            if (file.startsWith(dir)) {
                return true;
            }
        }
        return false;
    }

    public Collection<Path> getDirectories() {
        return directories;
    }
}
