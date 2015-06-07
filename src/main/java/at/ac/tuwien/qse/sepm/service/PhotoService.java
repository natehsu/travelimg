package at.ac.tuwien.qse.sepm.service;

import at.ac.tuwien.qse.sepm.entities.Journey;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.entities.Place;
import at.ac.tuwien.qse.sepm.entities.Tag;

import java.time.YearMonth;
import java.util.List;
import java.util.function.Predicate;

public interface PhotoService extends Service {

    /**
     * Retrieve a list of those months for which there are photos.
     *
     * @return a list of months with available photos
     */
    List<YearMonth> getMonthsWithPhotos() throws ServiceException;

    /**
     * delete the delivered List of Photos
     *
     * @param photos the list of photos
     * @throws ServiceException
     */
    void deletePhotos(List<Photo> photos) throws ServiceException;

    /**
     * edit the delivered List of Photos
     *
     * @param photos the list of photos
     * @throws ServiceException
     */
    void editPhotos(List<Photo> photos, Photo p) throws ServiceException;

    /**
     * @return the list of all available photos
     * @throws ServiceException
     */
    List<Photo> getAllPhotos() throws ServiceException;

    /**
     * Get all photos that match the specified filter.
     *
     * @param filter filter the photos are tested against
     * @return list of all available photos that match the filter
     */
    List<Photo> getAllPhotos(Predicate<Photo> filter) throws ServiceException;

    /**
     * Add Tag <tt>tag</tt> to every photo in list <tt>photos</tt>. If a photo already has this tag,
     * then it will keep it.
     *
     * @param photos must not be null; all elements must not be null; no element.id must be null
     * @param tag    must not be null; tag.id must not be null
     * @throws ServiceException         if an Exception in this or an underlying
     *                                  layer occurs
     */
    void addTagToPhotos(List<Photo> photos, Tag tag) throws ServiceException;

    /**
     * Remove Tag <tt>tag</tt> from all photos in list <tt>photos</tt>. If a photo in the list
     * does not have this tag, then no action will be taken for this photo.
     *
     * @param photos must not be null; all elements must not be null; no element.id must be null
     * @param tag    must not be null; tag.id must not be null
     * @throws ServiceException         if an Exception in this or an underlying
     *                                  layer occurs
     */
    void removeTagFromPhotos(List<Photo> photos, Tag tag) throws ServiceException;

    /**
     * Return list of all tags which are currently set for <tt>photo</tt>.
     *
     * @param photo must not be null; photo.id must not be null;
     * @return List with all tags which are linked to <tt>photo</tt> as a PhotoTag;
     * If no tag exists, return an empty List.
     * @throws ServiceException         if an exception occurs on this or an underlying layer
     */
    List<Tag> getTagsForPhoto(Photo photo) throws ServiceException;

    /**
     * Persists the rating of the specified photo.
     *
     * @param photo photo for which the rating should be stored
     * @throws ServiceException failed to perform operation
     */
    void savePhotoRating(Photo photo) throws ServiceException;

    void addJourneyToPhotos(List<Photo> photos, Journey journey) throws ServiceException;

    void addPlaceToPhotos(List<Photo> photos, Place place) throws ServiceException;
}
