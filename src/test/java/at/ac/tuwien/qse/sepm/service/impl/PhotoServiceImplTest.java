package at.ac.tuwien.qse.sepm.service.impl;

import at.ac.tuwien.qse.sepm.dao.WithData;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.entities.Photographer;
import at.ac.tuwien.qse.sepm.entities.Place;
import at.ac.tuwien.qse.sepm.entities.Rating;
import at.ac.tuwien.qse.sepm.service.ImportService;
import at.ac.tuwien.qse.sepm.service.PhotoService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.service.ServiceTestBase;
import at.ac.tuwien.qse.sepm.util.Cancelable;
import at.ac.tuwien.qse.sepm.util.ErrorHandler;
import at.ac.tuwien.qse.sepm.util.TestIOHandler;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PhotoServiceImplTest extends ServiceTestBase {


    private static final Photographer defaultPhotographer = new Photographer(1, "Test Photographer");
    private static final Place defaultPlace = new Place(1, "Unkown place", "Unkown place", 0.0, 0.0, null);

    private static final String dataDir = Paths
            .get(System.getProperty("java.io.tmpdir"), "travelimg").toString();
    private static final String sourceDir = Paths.get(
            System.getProperty("os.name").contains("indow") ?
                    ImportTest.class.getClassLoader().getResource("db/testimages").getPath()
                            .substring(1) :
                    ImportTest.class.getClassLoader().getResource("db/testimages").getPath()).toString();
    List<Photo> inputPhotos = new ArrayList<Photo>() {{
        add(new Photo(6, defaultPhotographer, sourceDir + "/6.jpg", Rating.NONE, null, 0, 0, defaultPlace));
        add(new Photo(7, defaultPhotographer, sourceDir + "/7.jpg", Rating.NONE, null, 0, 0, defaultPlace));
        add(new Photo(8, defaultPhotographer, sourceDir + "/8.jpg", Rating.NONE, null, 0, 0, defaultPlace));
    }};
    private List<Photo> expectedPhotos = new ArrayList<Photo>() {{
        add(new Photo(6, defaultPhotographer, dataDir + "/2005/09/11/6.jpg", Rating.NONE, LocalDateTime
                .of(2005, 9, 11, 15, 43, 55), 39.73934166666667, -104.99156111111111, defaultPlace));
        add(new Photo(7, defaultPhotographer, dataDir + "/2005/09/11/7.jpg", Rating.NONE, LocalDateTime.of(2005, 9, 11, 15, 44, 8), 39.739336111111115, -104.9916361111111, defaultPlace));
        add(new Photo(8, defaultPhotographer, dataDir + "/2005/09/11/8.jpg", Rating.NONE, LocalDateTime.of(2005, 9, 11, 15, 48, 7), 39.73994444444445, -104.98952777777778, defaultPlace));
    }};
    @Autowired
    private ImportService importService;
    @Autowired
    private TestIOHandler ioHandler;
    @Autowired
    private PhotoService photoService;


    public PhotoServiceImplTest() {
        for (Photo photo : expectedPhotos) {
            String path = photo.getPath().replace("/", File.separator);
            photo.setPath(path);
        }

        for (Photo photo : inputPhotos) {
            String path = photo.getPath().replace("/", File.separator);
            photo.setPath(path);
        }
    }

    public List<Photo> duplicateList(List<Photo> photos) {
        return photos
                .stream()
                .map(p -> new Photo(p))
                .collect(Collectors.toList());
    }

    private void awaitCompletion(Cancelable task) {
        int waited = 0;
        int interval = 100;
        int maxTimeout = 5000;
        try {
            while (waited < maxTimeout) {
                if (task.isFinished()) {
                    return;
                }
                Thread.sleep(interval);
                waited += interval;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
    }

    private void importPhotos() {
        List<Photo> toBeImported = duplicateList(inputPhotos);
        TestPhotoAcceptor acceptor = new TestPhotoAcceptor();
        TestErrorHandler errorHandler = new TestErrorHandler();
        Cancelable task = importService.importPhotos(toBeImported, acceptor, errorHandler);

        awaitCompletion(task);
    }

    //List<YearMonth> getMonthsWithPhotos() throws ServiceException;
    @WithData
    @Test
    public void testGetMonthsWithPhotos() throws ServiceException {

    }

    public void testDeletePhotos() throws ServiceException {

    }

    // void editPhotos(List<Photo> photos, Photo p) throws ServiceException;
    public void testEditPhotos() throws ServiceException {

    }

    //void deletePhotos(List<Photo> photos) throws ServiceException;

    //List<Photo> getAllPhotos() throws ServiceException;
    public void testGetAllPhotos() throws ServiceException {

    }

    /**
     * Get all photos that match the specified filter.
     *
     * @param filter filter the photos are tested against
     * @return list of all available photos that match the filter
     */

    //List<Photo> getAllPhotos(Predicate<Photo> filter) throws ServiceException;
    public void testGetAllPhotosFilter() throws ServiceException {

    }

    /**
     * Persists the rating of the specified photo.
     *
     * @param photo photo for which the rating should be stored
     * @throws ServiceException failed to perform operation
     */
    //void savePhotoRating(Photo photo) throws ServiceException;
    @Test
    public void testSavePhotoRating() throws ServiceException {
        // TODO
    }

    private class TestPhotoAcceptor implements Consumer<Photo> {
        List<Photo> accepted = new ArrayList<>();

        @Override
        public void accept(Photo photo) {
            accepted.add(photo);
        }

        public List<Photo> getAccepted() {
            return accepted;
        }
    }

    private class TestErrorHandler implements ErrorHandler<ServiceException> {
        public List<ServiceException> exceptions = new ArrayList<>();

        @Override
        public void handle(ServiceException exception) {
            exceptions.add(exception);
        }

        public boolean exceptionOccured() {
            return exceptions.size() > 0;
        }
    }
}
