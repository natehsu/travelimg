package at.ac.tuwien.qse.sepm.service.impl;

import at.ac.tuwien.qse.sepm.dao.DAOException;
import at.ac.tuwien.qse.sepm.dao.ExifDAO;
import at.ac.tuwien.qse.sepm.dao.PhotoDAO;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.entities.Tag;
import at.ac.tuwien.qse.sepm.entities.validators.ValidationException;
import at.ac.tuwien.qse.sepm.service.PhotoService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.util.Cancelable;
import at.ac.tuwien.qse.sepm.util.CancelableTask;
import at.ac.tuwien.qse.sepm.util.ErrorHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PhotoServiceImpl implements PhotoService {

    private static final Logger logger = LogManager.getLogger();

    private PhotoDAO photoDAO;
    @Autowired ExifDAO exifDAO;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    public PhotoServiceImpl() {

    }

    @Autowired
    public void setPhotoDAO(PhotoDAO photoDAO) {
        this.photoDAO = photoDAO;
    }

    public List<Photo> getAllPhotos() throws ServiceException {
        try {
            return photoDAO.readAll();
        } catch (DAOException e) {
            throw new ServiceException(e);
        } catch(ValidationException e) {
            throw new ServiceException("Failed to validate entity", e);
        }
    }

    public List<Tag> getAllTags() throws ServiceException {
        return null;
    }

    public void requestFullscreenMode(List<Photo> photos) throws ServiceException {

    }

    public void deletePhotos(List<Photo> photos) throws ServiceException {

    }

    public void addTagToPhotos(List<Photo> photos, Tag t) throws ServiceException {

    }

    public void removeTagFromPhotos(List<Photo> photos, Tag t) throws ServiceException {

    }

    public void editPhotos(List<Photo> photos, Photo p) throws ServiceException {

    }

    public Cancelable loadPhotosByDate(Date date, Consumer<Photo> callback, ErrorHandler<ServiceException> errorHandler) {
        logger.debug("Loading photos");
        AsyncLoader loader = new AsyncLoader(date, callback, errorHandler);
        executorService.submit(loader);

        return loader;
    }

    private class AsyncLoader extends CancelableTask {
        private Date date;
        private Consumer<Photo> callback;
        private ErrorHandler<ServiceException> errorHandler;

        public AsyncLoader(Date date, Consumer<Photo> callback, ErrorHandler<ServiceException> errorHandler) {
            super();
            this.date = date;
            this.callback = callback;
            this.errorHandler = errorHandler;
        }

        @Override
        protected void execute() {
            List<Photo> photos;
            try {
                photos = photoDAO.readPhotosByDate(date);
            } catch (DAOException e) {
                errorHandler.propagate(new ServiceException("Failed to load photos", e));
                return;
            }

            for(Photo p : photos){
                if(!getIsRunning())
                    return;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                callback.accept(p);
            }
        }
    }

    @Override
    public List<Date> getMonthsWithPhotos() throws ServiceException{
        try {
            return exifDAO.getMonthsWithPhotos();
        } catch (DAOException ex) {
            throw new ServiceException(ex);
        }
    }

    public void close() {
        executorService.shutdown();
    }
}
