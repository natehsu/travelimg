package at.ac.tuwien.qse.sepm.service.impl;

import at.ac.tuwien.qse.sepm.dao.DAOException;
import at.ac.tuwien.qse.sepm.dao.ExifDAO;
import at.ac.tuwien.qse.sepm.dao.impl.JDBCExifDAO;
import at.ac.tuwien.qse.sepm.entities.Exif;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.service.ExifService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

public class ExifServiceImpl implements ExifService {

    private static final Logger logger = LogManager.getLogger(ExifServiceImpl.class);
    private ExifDAO exifDAO;

    public ExifServiceImpl() throws ServiceException {
        try {
            exifDAO = new JDBCExifDAO();
        } catch (DAOException e) {
            throw new ServiceException(e.getMessage());
        }

    }

    public void changeExif(Exif e) {

    }


    public Exif importExif(Photo p) {
        File file = new File(p.getPath());
        Timestamp date;
        String exposure;
        double aperture;
        double focalLength;
        int iso;
        boolean flash;
        String cameraModel;
        Rational[] longitude;
        String longitudeDirection;
        Rational[] latitude;
        String latitudeDirection;
        double altitude = 0;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            Directory subIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Directory iFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Directory gPSDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            try {

                date = new Timestamp(subIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).getTime());
                exposure = subIFDDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
                aperture = subIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_APERTURE);
                focalLength = subIFDDirectory.getDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
                iso = subIFDDirectory.getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
                flash = subIFDDirectory.getBoolean(ExifSubIFDDirectory.TAG_FLASH);
                cameraModel = iFD0Directory.getString(ExifIFD0Directory.TAG_MAKE) + " " + iFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);

                longitude = gPSDirectory.getRationalArray(GpsDirectory.TAG_LONGITUDE);
                if(longitude == null) {
                    // TODO: Handle Photos without GPS + add altitude and latitude test
                    logger.error("No GPS found");
                }
                double longitudeHours = longitude[0].doubleValue();
                double longitudeMinutes = longitude[1].doubleValue();
                double longitudeSeconds = (longitude[1].doubleValue() - Math.floor(longitude[1].doubleValue())) * 60;
                longitudeDirection = gPSDirectory.getString(GpsDirectory.TAG_LONGITUDE_REF);

                latitude = gPSDirectory.getRationalArray(GpsDirectory.TAG_LATITUDE);
                double latitudeHours = latitude[0].doubleValue();
                double latitudeMinutes = latitude[1].doubleValue();
                double latitudeSeconds = (latitude[1].doubleValue() - Math.floor(latitude[1].doubleValue())) * 60;
                latitudeDirection = gPSDirectory.getString(GpsDirectory.TAG_LATITUDE_REF);



                altitude = gPSDirectory.getDouble(GpsDirectory.TAG_ALTITUDE);
                Exif exif = new Exif(p.getId(), date, exposure, aperture, focalLength, iso, flash, cameraModel, "0", 0, altitude);
                exifDAO.create(exif);
                return exif;
            } catch (MetadataException e) {
                e.printStackTrace();
            } catch (DAOException e) {
                e.printStackTrace();
            }
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
