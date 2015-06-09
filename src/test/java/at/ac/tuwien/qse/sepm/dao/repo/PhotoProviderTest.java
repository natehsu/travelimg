package at.ac.tuwien.qse.sepm.dao.repo;

import at.ac.tuwien.qse.sepm.entities.Rating;
import org.junit.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public abstract class PhotoProviderTest {

    protected abstract PhotoProvider getObject();
    protected abstract Context getContext();
    protected abstract void add(PhotoProvider object, Photo photo) throws PersistenceException;

    @Test
    public void index_nothingAdded_returnsEmpty() throws PersistenceException {
        PhotoProvider object = getObject();

        assertTrue(object.index().isEmpty());
    }

    @Test
    public void index_someExisting_returnPaths() throws PersistenceException {
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
    public void contains_nonExisting_returnsFalse() throws PersistenceException {
        PhotoProvider object = getObject();
        Path file = getContext().getFile1();

        assertFalse(object.contains(file));
    }

    @Test
    public void contains_existing_returnsTrue() throws PersistenceException {
        PhotoProvider object = getObject();
        Photo photo = getContext().getPhoto1();
        add(object, photo);

        assertTrue(object.contains(photo.getFile()));
    }

    @Test(expected = PhotoNotFoundException.class)
    public void read_nonExisting_throws() throws PersistenceException {
        PhotoProvider object = getObject();
        Path file = getContext().getFile1();

        object.read(file);
    }

    @Test
    public void read_existing_returnsPhoto() throws PersistenceException {
        PhotoProvider object = getObject();
        Photo photo = getContext().getPhoto1();
        add(object, photo);

        assertEquals(photo, object.read(photo.getFile()));
    }

    @Test
    public void readAll_nothingAdded_empty() throws PersistenceException {
        PhotoProvider object = getObject();

        assertTrue(object.readAll().isEmpty());
    }

    @Test
    public void readAll_someExisting_returnsPhotos() throws PersistenceException {
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

        public PhotoMetaData getPhotoData1() {
            PhotoMetaData data = new PhotoMetaData();
            data.setDate(LocalDateTime.of(1993, 3, 30, 7, 5));
            data.setLongitude(17.0);
            data.setLatitude(18.0);
            data.setRating(Rating.GOOD);
            data.setPhotographer("Kris");
            data.getTags().clear();
            data.getTags().add("food");
            data.getTags().add("india");
            return data;
        }

        public PhotoMetaData getPhotoData2() {
            PhotoMetaData data = new PhotoMetaData();
            data.setDate(LocalDateTime.of(2014, 8, 14, 15, 36));
            data.setLongitude(42.0);
            data.setLatitude(43.0);
            data.setRating(Rating.NEUTRAL);
            data.setPhotographer("Lukas");
            data.getTags().clear();
            data.getTags().add("usa");
            data.getTags().add("nature");
            return data;
        }

        public Photo getModified1() {
            return new Photo(getFile1(), getPhotoData2());
        }
    }
}