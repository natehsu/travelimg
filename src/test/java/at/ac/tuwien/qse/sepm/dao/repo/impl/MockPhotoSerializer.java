package at.ac.tuwien.qse.sepm.dao.repo.impl;

import at.ac.tuwien.qse.sepm.dao.repo.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MockPhotoSerializer implements PhotoSerializer {

    private final Map<Integer, PhotoMetadata> dataByIndex = new HashMap<>();
    private final Map<PhotoMetadata, Integer> indicesByData = new HashMap<>();

    public void put(int index, PhotoMetadata photo) {
        dataByIndex.put(index, photo);
        indicesByData.put(photo, index);
    }

    @Override public PhotoMetadata read(InputStream is) throws PersistenceException {
        try {
            int index = is.read();
            if (!dataByIndex.containsKey(index)) {
                throw new FormatException();
            }
            return dataByIndex.get(index);
        } catch (IOException ex) {
            throw new PersistenceException();
        }
    }

    @Override public void update(InputStream is, OutputStream os, PhotoMetadata metadata) throws PersistenceException {
        try {
            if (!indicesByData.containsKey(metadata)) {
                throw new FormatException();
            }
            int index = indicesByData.get(metadata);
            os.write(index);
        } catch (IOException ex) {
            throw new PersistenceException();
        }
    }
}
