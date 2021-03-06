package at.ac.tuwien.qse.sepm.service.impl;

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

import at.ac.tuwien.qse.sepm.entities.Photo;
import at.ac.tuwien.qse.sepm.entities.Photographer;
import at.ac.tuwien.qse.sepm.service.PhotographerService;
import at.ac.tuwien.qse.sepm.service.ServiceException;
import at.ac.tuwien.qse.sepm.service.ServiceTestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

public class PhotographerTest extends ServiceTestBase {

    @Autowired
    private PhotographerService photographerService;

    private Photographer createShelly() throws ServiceException {
        return photographerService.create(new Photographer(-1, "Shelly"));
    }

    @Test(expected = ServiceException.class)
    public void test_create_malformed_throws1() throws ServiceException {
        photographerService.create(new Photographer(1, null));
    }

    @Test(expected = ServiceException.class)
    public void test_create_malformed_throws2() throws ServiceException {
        photographerService.create(new Photographer(1, ""));
    }

    @Test
    public void test_create_persists() throws ServiceException {
        assertThat(photographerService.readAll(), empty());

        Photographer photographer = new Photographer(-1, "Dale");
        Photographer created = photographerService.create(photographer);

        // test that photographer was correctly persisted
        assertThat(photographer.getName(), equalTo(created.getName()));
        assertThat(photographerService.readAll(), contains(created));
    }

    @Test(expected = ServiceException.class)
    public void test_update_malformed_throws1() throws ServiceException {
        Photographer shelly = createShelly();
        shelly.setName(null);
        photographerService.update(shelly);
    }

    @Test(expected = ServiceException.class)
    public void test_update_malformed_throws2() throws ServiceException {
        Photographer shelly = createShelly();
        shelly.setName("");
        photographerService.update(shelly);
    }

    @Test(expected = ServiceException.class)
    public void test_update_malformed_throws3() throws ServiceException {
        Photographer shelly = createShelly();
        shelly.setId(null);
        photographerService.update(shelly);
    }

    @Test
    public void test_readall_returns_created() throws ServiceException {
        Photographer p1 = photographerService.create(new Photographer(-1, "Shelly"));
        Photographer p2 = photographerService.create(new Photographer(-1, "Denise"));
        Photographer p3 = photographerService.create(new Photographer(-1, "Big Ed"));

        assertThat(photographerService.readAll(), containsInAnyOrder(p1, p2, p3));
    }
}
