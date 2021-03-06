package at.ac.tuwien.qse.sepm.dao.impl;

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
import at.ac.tuwien.qse.sepm.dao.SlideDAO;
import at.ac.tuwien.qse.sepm.dao.SlideshowDAO;
import at.ac.tuwien.qse.sepm.entities.*;
import at.ac.tuwien.qse.sepm.entities.validators.SlideshowValidator;
import at.ac.tuwien.qse.sepm.entities.validators.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JDBCSlideshowDAO extends JDBCDAOBase implements SlideshowDAO{

    private static final String READ_ALL_STATEMENT = "SELECT id, name, durationbetweenphotos FROM SLIDESHOW;";
    private static final String UPDATE_STATEMENT = "UPDATE SLIDESHOW SET name = ?, durationbetweenphotos = ? WHERE id = ?;";
    private static final String DELETE_STATEMENT = "DELETE FROM SLIDESHOW WHERE id = ?;";

    private SimpleJdbcInsert insertSlideshow;

    @Autowired
    private SlideDAO slideDAO;

    @Override
    @Autowired
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
        this.insertSlideshow = new SimpleJdbcInsert(dataSource)
                .withTableName("Slideshow")
                .usingGeneratedKeyColumns("id");
    }


    @Override
    public Slideshow create(Slideshow slideshow) throws DAOException, ValidationException {
        logger.debug("Creating slideshow {}", slideshow);

        SlideshowValidator.validate(slideshow);

        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", slideshow.getName());
            parameters.put("durationbetweenphotos", slideshow.getDurationBetweenPhotos());
            Number newId = insertSlideshow.executeAndReturnKey(parameters);
            slideshow.setId((int) newId.longValue());

            logger.debug("Created slideshow {}", slideshow);
            return slideshow;
        } catch (DataAccessException ex) {
            logger.error("Failed to create slideshow", ex);
            throw new DAOException("Failed to create slideshow", ex);
        }
    }

    @Override
    public Slideshow update(Slideshow slideshow) throws DAOException, ValidationException {
        logger.debug("Updating slideshow {}", slideshow);

        SlideshowValidator.validate(slideshow);
        SlideshowValidator.validateID(slideshow.getId());

        try {
            jdbcTemplate.update(UPDATE_STATEMENT,
                    slideshow.getName(),
                    slideshow.getDurationBetweenPhotos(),
                    slideshow.getId()
            );
            logger.debug("Successfully updated slideshow");
            return slideshow;
        } catch (DataAccessException ex) {
            logger.error("Failed to update slideshow", ex);
            throw new DAOException("Failed to update slideshow", ex);
        }
    }

    @Override
    public void delete(Slideshow slideshow) throws DAOException, ValidationException {
        logger.debug("Deleting slideshow {}", slideshow);

        SlideshowValidator.validateID(slideshow.getId());

        try {
            deleteSlidesForSlideshow(slideshow);
            jdbcTemplate.update(DELETE_STATEMENT, slideshow.getId());
        } catch (DataAccessException ex) {
            throw new DAOException("Failed to delete slideshow", ex);
        }
    }

    private void deleteSlidesForSlideshow(Slideshow slideshow) throws DAOException, ValidationException {
        for (PhotoSlide slide : slideDAO.getPhotoSlidesForSlideshow(slideshow.getId())) {
            slideDAO.delete(slide);
        }

        for (MapSlide slide : slideDAO.getMapSlidesForSlideshow(slideshow.getId())) {
            slideDAO.delete(slide);
        }

        for (TitleSlide slide : slideDAO.getTitleSlidesForSlideshow(slideshow.getId())) {
            slideDAO.delete(slide);
        }
    }

    @Override
    public List<Slideshow> readAll() throws DAOException {
        logger.debug("retrieving all slideshows");

        try {
            return jdbcTemplate.query(READ_ALL_STATEMENT, new SlideshowMapper());
        } catch (DataAccessException e) {
            throw new DAOException("Failed to read all slides", e);
        }
    }

    private class SlideshowMapper implements RowMapper<Slideshow> {
        @Override
        public Slideshow mapRow(ResultSet rs, int rowNum) throws SQLException {
            int slideshowId = rs.getInt(1);
            List<Slide> slides;

            Slideshow slideshow = new Slideshow(slideshowId, rs.getString(2), rs.getDouble(3));

            try {
                slideshow.getPhotoSlides().addAll(slideDAO.getPhotoSlidesForSlideshow(slideshowId));
                slideshow.getMapSlides().addAll(slideDAO.getMapSlidesForSlideshow(slideshowId));
                slideshow.getTitleSlides().addAll(slideDAO.getTitleSlidesForSlideshow(slideshowId));
            } catch (DAOException ex) {
                throw new DAOException.Unchecked("Failed to retrieve slides for given slideshow", ex);
            }

            return slideshow;
        }
    }
}
