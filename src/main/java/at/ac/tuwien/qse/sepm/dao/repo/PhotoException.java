package at.ac.tuwien.qse.sepm.dao.repo;

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

import java.nio.file.Path;

/**
 * Exception that is thrown when an operation on a specific photo failed.
 */
public abstract class PhotoException extends DAOException {

    private final PhotoProvider photoProvider;
    private final Path photoFile;

    public PhotoException(PhotoProvider photoProvider, Path photoFile) {
        if (photoProvider == null) throw new IllegalArgumentException();
        if (photoFile == null) throw new IllegalArgumentException();
        this.photoProvider = photoProvider;
        this.photoFile = photoFile;
    }

    public PhotoException(PhotoProvider photoProvider, Path photoFile, Throwable cause) {
        super(cause);
        if (photoProvider == null) throw new IllegalArgumentException();
        if (photoFile == null) throw new IllegalArgumentException();
        this.photoProvider = photoProvider;
        this.photoFile = photoFile;
    }

    /**
     * Get the provider in which the operation was performed.
     *
     * @return provider responsible for the error
     */
    public PhotoProvider getPhotoProvider() {
        return photoProvider;
    }

    /**
     * Get the path of the photo for which the operation failed.
     *
     * @return path of photo
     */
    public Path getPhotoFile() {
        return photoFile;
    }
}
