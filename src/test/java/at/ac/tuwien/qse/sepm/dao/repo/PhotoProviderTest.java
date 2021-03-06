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
import at.ac.tuwien.qse.sepm.entities.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-config.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class PhotoProviderTest {

    protected abstract PhotoProvider getObject();
    protected abstract Context getContext();
    protected abstract void add(PhotoProvider object, Photo photo) throws DAOException;

    @Test
    public void index_nothingAdded_returnsEmpty() throws DAOException {
        PhotoProvider object = getObject();

        assertTrue(object.index().isEmpty());
    }

    @Test
    public void index_someExisting_returnPaths() throws DAOException {
        PhotoProvider object = getObject();
        Photo photo1 = getContext().getPhoto1();
        Photo photo2 = getContext().getPhoto2();
        add(object, photo1);
        add(object, photo2);

        Collection<Path> index = object.index();
        assertEquals(2, index.size());
        assertTrue(index.contains(photo1.getFile()));
        assertTrue(index.contains(photo2.getFile()));
    }

    @Test
    public void contains_nonExisting_returnsFalse() throws DAOException {
        PhotoProvider object = getObject();
        Path file = getContext().getFile1();

        assertFalse(object.contains(file));
    }

    @Test
    public void contains_existing_returnsTrue() throws DAOException {
        PhotoProvider object = getObject();
        Photo photo = getContext().getPhoto1();
        add(object, photo);

        assertTrue(object.contains(photo.getFile()));
    }

    @Test(expected = PhotoNotFoundException.class)
    public void read_nonExisting_throws() throws DAOException {
        PhotoProvider object = getObject();
        Path file = getContext().getFile1();

        object.read(file);
    }

    @Test
    public void read_existing_returnsPhoto() throws DAOException {
        PhotoProvider object = getObject();
        Photo photo = getContext().getPhoto1();
        add(object, photo);

        assertEquals(photo, object.read(photo.getFile()));
    }

    @Test
    public void readAll_nothingAdded_empty() throws DAOException {
        PhotoProvider object = getObject();

        assertTrue(object.readAll().isEmpty());
    }

    @Test
    public void readAll_someExisting_returnsPhotos() throws DAOException {
        PhotoProvider object = getObject();
        Photo photo1 = getContext().getPhoto1();
        Photo photo2 = getContext().getPhoto2();
        add(object, photo1);
        add(object, photo2);

        Collection<Photo> photos = object.readAll();
        assertEquals(2, photos.size());
        assertTrue(photos.contains(photo1));
        assertTrue(photos.contains(photo2));
    }

    public abstract class Context {

        public abstract Path getFile1();

        public abstract Path getFile2();

        public Photo getPhoto1() {
            return new Photo(getFile1(), getPhotoData1());
        }

        public Photo getPhoto2() {
            return new Photo(getFile2(), getPhotoData2());
        }

        public PhotoMetadata getPhotoData1() {
            PhotoMetadata data = new PhotoMetadata();
            data.setDatetime(LocalDateTime.of(1993, 3, 30, 7, 5));
            data.setLongitude(17.0);
            data.setLatitude(18.0);
            data.setRating(Rating.GOOD);
            data.setPhotographer(new Photographer(null, "Kris"));
            data.getTags().clear();
            data.getTags().add(new Tag(null, "food"));
            data.getTags().add(new Tag(null, "india"));
            Journey journey = new Journey(null, "India 1993", LocalDateTime.of(1993, 1, 1, 0, 0), LocalDateTime.of(1993, 12, 31, 0, 0));
            Place place = new Place(null, "Bombay", "Indien", 15.0, 16.0);
            data.setPlace(place);
            data.setJourney(journey);
            return data;
        }

        public PhotoMetadata getPhotoData2() {
            PhotoMetadata data = new PhotoMetadata();
            data.setDatetime(LocalDateTime.of(2014, 8, 14, 15, 36));
            data.setLongitude(42.0);
            data.setLatitude(43.0);
            data.setRating(Rating.NEUTRAL);
            data.setPhotographer(new Photographer(null, "Lukas"));
            data.getTags().clear();
            data.getTags().add(new Tag(null, "usa"));
            data.getTags().add(new Tag(null, "nature"));
            Journey journey = new Journey(null, "Austria 2014", LocalDateTime.of(2014, 1, 1, 0, 0), LocalDateTime.of(
                    2014, 12, 31, 0, 0));
            Place place = new Place(null, "Wien", "Österreich", 40.0, 41.0);
            data.setPlace(place);
            data.setJourney(journey);
            return data;
        }

        public Photo getModified1() {
            return new Photo(getFile1(), getPhotoData2());
        }

        public PhotoMetadata getMinimalData() {
            PhotoMetadata data = new PhotoMetadata();
            data.setDatetime(LocalDateTime.of(2000, 1, 2, 3, 4));
            data.setLongitude(5.5);
            data.setLatitude(7.8);
            data.setRating(Rating.BAD);
            return data;
        }

        public PhotoMetadata getMaximalData() {
            PhotoMetadata data = new PhotoMetadata();
            data.setDatetime(LocalDateTime.of(2000, 1, 2, 3, 4));
            data.setLongitude(5.5);
            data.setLatitude(7.8);
            data.setRating(Rating.BAD);
            data.setPhotographer(new Photographer(null, "David"));
            data.getTags().add(new Tag(null, "food"));
            data.getTags().add(new Tag(null, "people"));
            Journey journey = new Journey(null, "Italien 2000", LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(
                    2000, 12, 31, 0, 0));
            Place place = new Place(null, "Rom", "Italien", 5, 8);
            data.setPlace(place);
            data.setJourney(journey);
            return data;
        }
    }
}
