package at.ac.tuwien.qse.sepm.dao.repo.impl;

import at.ac.tuwien.qse.sepm.dao.repo.Photo;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoRepository;
import at.ac.tuwien.qse.sepm.dao.repo.PhotoRepositoryTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MemoryPhotoRepositoryTest extends PhotoRepositoryTest {

    private final Path basePath = Paths.get("test/path");

    @Override protected PhotoRepository getObject() {
        return new MemoryPhotoRepository(basePath) {
            @Override protected Photo read(Path file, InputStream stream) throws IOException {
                return MemoryPhotoRepositoryTest.this.read(file, stream);
            }

            @Override protected void update(Photo photo, OutputStream stream) throws IOException {
                MemoryPhotoRepositoryTest.this.update(photo, stream);
            }
        };
    }

    @Override protected Path getPhotoFile1() {
        return basePath.resolve("some/file.jpg");
    }

    @Override protected Path getPhotoFile2() {
        return basePath.resolve("other/file.jpg");
    }
}
