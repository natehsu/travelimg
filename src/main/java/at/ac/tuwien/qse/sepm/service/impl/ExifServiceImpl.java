package at.ac.tuwien.qse.sepm.service.impl;

import at.ac.tuwien.qse.sepm.dao.DAOException;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoNotFoundException;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoRepository;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoSerializer;
import at.ac.tuwien.qse.sepm.dao.repo.impl.FileManager;
import at.ac.tuwien.qse.sepm.dao.repo.impl.JpegSerializer;
import at.ac.tuwien.qse.sepm.dao.repo.impl.PhotoFileRepository;
import at.ac.tuwien.qse.sepm.dao.repo.impl.PhysicalFileManager;
import at.ac.tuwien.qse.sepm.entities.Exif;
import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.service.ExifService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.util.IOHandler;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExifServiceImpl implements ExifService {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofPattern("yyyy:MM:dd HH:mm:ss");

    @Autowired
    private IOHandler ioHandler;

    @Autowired
    private PhotoFileRepository repository;

    @Override
    public Exif getExif(Photo photo) throws ServiceException {
        File file = new File(photo.getPath());
        String exposure = "not available";
        double aperture = 0.0;
        double focalLength = 0.0;
        int iso = 0;
        boolean flash = false;
        String make = "not available";
        String model = "not available";
        double altitude = 0.0;

        try {
            final ImageMetadata metadata = Imaging.getMetadata(file);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

            if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME)
                    != null) {
                exposure = jpegMetadata
                        .findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_EXPOSURE_TIME)
                        .getValueDescription().split(" ")[0];
            }

            if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_APERTURE_VALUE)
                    != null) {
                aperture = jpegMetadata
                        .findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_APERTURE_VALUE)
                        .getDoubleValue();
            }
            if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH)
                    != null) {
                focalLength = jpegMetadata
                        .findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH)
                        .getDoubleValue();
            }
            if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_ISO) != null) {
                iso = jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_ISO)
                        .getIntValue();
            }

            if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_FLASH) != null) {
                flash = jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_FLASH)
                        .getIntValue() != 0;
            }

            if (jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_MAKE) != null) {
                make = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_MAKE)
                        .getValueDescription();
            }
            if (jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_MODEL) != null) {
                model = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_MODEL)
                        .getValueDescription();
            }

            if (jpegMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_ALTITUDE)
                    != null) {
                altitude = jpegMetadata
                        .findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_ALTITUDE)
                        .getDoubleValue();
            }

            return new Exif(photo.getId(), exposure, aperture, focalLength, iso, flash, make, model,
                    altitude);
        } catch (IOException | ImageReadException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    @Override
    public void attachDateAndGeoData(Photo photo) throws ServiceException {
        File file = new File(photo.getPath());
        LocalDateTime datetime = photo.getData().getDatetime();
        double latitude = photo.getData().getLatitude();
        double longitude = photo.getData().getLongitude();

        try {
            ImageMetadata metadata = Imaging.getMetadata(file);

            if (datetime == null)
                datetime = getDateTime(metadata);

            if (Double.compare(latitude, 0.0) == 0 || Double.compare(longitude, 0.0) == 0) {
                final TiffImageMetadata.GPSInfo gpsInfo = ((JpegImageMetadata) metadata).getExif()
                        .getGPS();
                if (null != gpsInfo) {
                    longitude = gpsInfo.getLongitudeAsDegreesEast();
                    latitude = gpsInfo.getLatitudeAsDegreesNorth();
                }
            }
        } catch (IOException | ImageReadException e) {
            // intentionally ignore and use default
            logger.debug("Error occurred attaching geodate and date", e);
        }

        if (datetime == null)
            datetime = LocalDateTime.MIN;

        photo.getData().setDatetime(datetime);
        photo.getData().setLatitude(latitude);
        photo.getData().setLongitude(longitude);
    }

    @Override
    public void setDateAndGeoData(Photo photo) throws ServiceException {

        try {
            Path temp = File.createTempFile("travelimg-temp", "tmp").toPath();
            InputStream is = new BufferedInputStream(Files.newInputStream(photo.getFile()));
            OutputStream os = new BufferedOutputStream(Files.newOutputStream(temp));

            TiffOutputSet outputSet = null;

            File jpegImageFile = new File(photo.getPath());
            // note that metadata might be null if no metadata is found.
            final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (null != exif) {
                    // TiffImageMetadata class is immutable (read-only).
                    // TiffOutputSet class represents the Exif data to write.
                    //
                    // Usually, we want to update existing Exif metadata by
                    // changing
                    // the values of a few fields, or adding a field.
                    // In these cases, it is easiest to use getOutputSet() to
                    // start with a "copy" of the fields read from the image.
                    outputSet = exif.getOutputSet();
                }
            }

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all of the other
            // existing tags.
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            outputSet.setGPSInDegrees(photo.getData().getLongitude(), photo.getData().getLatitude());
            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                    dateFormatter.format(photo.getData().getDatetime()));
            //outputSet.addDirectory(exifDirectory);

            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os,
                    outputSet);

            os.close();
            is.close();

            ioHandler.copyFromTo(temp, photo.getFile());
        } catch (IOException | ImageReadException | ImageWriteException ex) {
            logger.error("Failed to set gps and date", ex);
        }
    }

    @Override public void setMetaData(Photo photo) throws ServiceException {
        JpegSerializer serializer = new JpegSerializer();
        FileManager fileManager = new PhysicalFileManager();

        logger.debug("Set metadata {}", photo);

        Path file = photo.getFile();

        Path temp = Paths.get(file.toString() + ".temp");
        InputStream is = null;
        OutputStream os = null;
        try {
            if (!fileManager.exists(temp)) {
                try {
                    fileManager.createFile(temp);
                } catch (IOException ex) {
                    logger.warn("failed creating temp file at {}", temp);
                    throw new ServiceException(ex);
                }
            }

            try {
                is = fileManager.newInputStream(file);
            } catch (IOException ex) {
                logger.warn("failed creating input stream for file {}", file);
                throw new ServiceException(ex);
            }

            try {
                os = fileManager.newOutputStream(temp);
            } catch (IOException ex) {
                logger.warn("failed creating output stream for file {}", temp);
                throw new ServiceException(ex);
            }

            try {
                serializer.update(is, os, photo.getData());
            } catch (DAOException ex) {
                throw new ServiceException(ex);
            }

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                logger.warn("failed closing input stream for file {}");
                logger.error(ex);
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                logger.warn("failed closing output stream for file {}");
                logger.error(ex);
            }
        }

        try {
            fileManager.copy(temp, file);
        } catch (IOException ex) {
            logger.warn("failed copying {} -> {}", temp, file);
            throw new ServiceException(ex);
        } finally {
            try {
                if (fileManager.exists(temp)) {
                    fileManager.delete(temp);
                }
            } catch (IOException ex) {
                logger.warn("failed deleting temp file {}", temp);
                logger.error(ex);
            }
        }
    }

    private LocalDateTime getDateTime(ImageMetadata metadata) {
        if (metadata == null)
            return null;

        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        if (jpegMetadata.findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
                != null) {
            String tempDate = jpegMetadata
                    .findEXIFValueWithExactMatch(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
                    .getValueDescription();
            tempDate = tempDate
                    .substring(1, tempDate.length() - 1); // remove enclosing single quotes

            return dateFormatter.parse(tempDate, LocalDateTime::from);
        } else {
            return null;
        }
    }

}
