package at.ac.tuwien.qse.sepm.service.impl;

import at.ac.tuwien.qse.sepm.entities.*;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhotoFilterTest {

    private Journey createJourney() {
        return new Journey(1, "Asia 2014",
                LocalDateTime.of(2014, 1, 1, 0, 0),
                LocalDateTime.of(2015, 1, 1, 0, 0));
    }

    private Place createPlace() {
        return new Place(1, "Bombay", "India", 10, 20);
    }

    private Photo createMatchingPhoto() {
        Photo photo = new Photo();
        photo.getData().setRating(Rating.NONE);
        photo.getData().setPhotographer(new Photographer(1, "John"));
        photo.getData().setDatetime(LocalDateTime.of(2015, 12, 1, 0, 0));
        photo.getData().setPlace(createPlace());
        photo.getData().setJourney(createJourney());
        return photo;
    }

    private PhotoFilter createFilter() {
        PhotoFilter filter = new PhotoFilter();
        filter.getIncludedPhotographers().add(new Photographer(1, "John"));
        filter.getIncludedRatings().add(Rating.NONE);
        filter.setUntaggedIncluded(true);
        filter.getIncludedJourneys().add(createJourney());
        filter.getIncludedPlaces().add(createPlace());
        filter.getIncludedJourneys().add(createJourney());
        return filter;
    }

    @Test
    public void filter_untaggedIncludedAndPhotoIsUntagged_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        photo.getData().getTags().clear();
        filter.setUntaggedIncluded(true);
        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_untaggedIncludedAndPhotoIsTagged_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        photo.getData().getTags().add(new Tag(1, "blue"));
        filter.setUntaggedIncluded(true);
        filter.getIncludedCategories().clear();

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_untaggedNotIncludedAndPhotoIsUntagged_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        photo.getData().getTags().clear();
        filter.setUntaggedIncluded(false);

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_categoriesPhotoHasDifferent_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedCategories().clear();
        filter.getIncludedCategories().add(new Tag(1, "blue"));
        filter.getIncludedCategories().add(new Tag(2, "red"));
        photo.getData().getTags().clear();
        photo.getData().getTags().add(new Tag(3, "green"));
        photo.getData().getTags().add(new Tag(3, "pink"));

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_categoriesPhotoHasSame_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedCategories().clear();
        filter.getIncludedCategories().add(new Tag(1, "blue"));
        filter.getIncludedCategories().add(new Tag(2, "red"));
        photo.getData().getTags().clear();
        photo.getData().getTags().add(new Tag(1, "blue"));
        photo.getData().getTags().add(new Tag(2, "red"));

        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_categoriesPhotoHasOverlap_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedCategories().clear();
        filter.getIncludedCategories().add(new Tag(1, "blue"));
        filter.getIncludedCategories().add(new Tag(2, "red"));
        photo.getData().getTags().clear();
        photo.getData().getTags().add(new Tag(2, "red"));
        photo.getData().getTags().add(new Tag(3, "green"));

        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_ratingPhotoHasDifferent_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedRatings().clear();
        filter.getIncludedRatings().add(Rating.GOOD);
        filter.getIncludedRatings().add(Rating.NEUTRAL);
        photo.getData().setRating(Rating.NONE);

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_ratingPhotoHasSame_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedRatings().clear();
        filter.getIncludedRatings().add(Rating.GOOD);
        filter.getIncludedRatings().add(Rating.NEUTRAL);
        photo.getData().setRating(Rating.GOOD);

        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_photographerPhotoHasDifferent_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedPhotographers().clear();
        filter.getIncludedPhotographers().add(new Photographer(1, "John"));
        filter.getIncludedPhotographers().add(new Photographer(2, "Bill"));
        photo.getData().setPhotographer(new Photographer(3, "Tina"));

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_photographerPhotoHasSame_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        filter.getIncludedPhotographers().clear();
        filter.getIncludedPhotographers().add(new Photographer(1, "John"));
        filter.getIncludedPhotographers().add(new Photographer(2, "Bill"));
        photo.getData().setPhotographer(new Photographer(2, "Bill"));

        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_placeAndJourneyIsNull_isMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        photo.getData().setPlace(null);
        photo.getData().setJourney(null);
        filter.getIncludedJourneys().clear();
        filter.getIncludedJourneys().add(null);
        filter.getIncludedPlaces().clear();
        filter.getIncludedPlaces().add(null);

        assertTrue(filter.test(photo));
    }

    @Test
    public void filter_placePhotoHasDifferent_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        Place place = createPlace();
        place.setCity("Mubai");
        filter.getIncludedPlaces().clear();
        filter.getIncludedPlaces().add(place);

        assertFalse(filter.test(photo));
    }

    @Test
    public void filter_journeyPhotoHasDifferent_noMatch() {
        PhotoFilter filter = createFilter();
        Photo photo = createMatchingPhoto();

        Journey journey = createJourney();
        journey.setName("White Lodge");
        filter.getIncludedJourneys().clear();
        filter.getIncludedJourneys().add(journey);

        assertFalse(filter.test(photo));
    }
}
