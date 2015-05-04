DROP TABLE PhotoTag IF EXISTS;
DROP TABLE Tag IF EXISTS;
DROP TABLE Photo IF EXISTS;
DROP TABLE Photographer IF EXISTS;
DROP TABLE EXIF IF EXISTS;


CREATE TABLE Photographer(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(30));
CREATE TABLE Tag(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(30));
CREATE TABLE Photo(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, photographer_id INTEGER REFERENCES Photographer(id), path VARCHAR NOT NULL, date DATE NOT NULL, rating INTEGER NOT NULL DEFAULT 0);
CREATE TABLE PhotoTag(photo_id INTEGER REFERENCES Photo(id), tag_id INTEGER REFERENCES Tag(id), PRIMARY KEY(photo_id, tag_id));
CREATE TABLE Exif(photo_id INTEGER REFERENCES Photo(id), date TIMESTAMP NOT NULL, exposure DOUBLE NOT NULL, aperture DOUBLE NOT NULL, focallength DOUBLE NOT NULL, iso INTEGER NOT NULL, flash BOOLEAN NOT NULL, cameramodel VARCHAR NOT NULL, longitude DOUBLE NOT NULL, latitude DOUBLE NOT NULL, altitude DOUBLE NOT NULL);

INSERT INTO Photographer(name) VALUES ('Alex Kinara');
INSERT INTO Photographer(name) VALUES ('Thomas Mueller');

INSERT INTO Photo(photographer_id,path,date) VALUES (1,'test','2012-09-17');
INSERT INTO Photo(photographer_id,path,date) VALUES (2,'test1','2012-09-18');
INSERT INTO Photo(photographer_id,path,date) VALUES (1,'test3','2012-09-17');
INSERT INTO Photo(photographer_id,path,date) VALUES (2,'test4','2012-09-16');
INSERT INTO Photo(photographer_id,path,date) VALUES (1,'test5','2012-09-16');

INSERT INTO Tag(name) VALUES ('Person');
INSERT INTO Tag(name) VALUES ('Essen');

INSERT INTO PhotoTag (photo_id,tag_id) VALUES (1,1);
INSERT INTO PhotoTag (photo_id,tag_id) VALUES (1,2);
INSERT INTO PhotoTag (photo_id,tag_id) VALUES (2,2);

INSERT INTO Exif(photo_id, date, exposure, aperture, focallength, iso, flash, cameramodel, longitude, latitude, altitude) VALUES (1,'2015-03-28 12:13:20',0.25,8,24,200,false,'Canon 600D',48.12,16.22,255);
INSERT INTO Exif(photo_id, date, exposure, aperture, focallength, iso, flash, cameramodel, longitude, latitude, altitude) VALUES (2,'2015-03-28 12:13:20',0.25,8,24,200,false,'Canon 600D',48.12,16.22,255);
INSERT INTO Exif(photo_id, date, exposure, aperture, focallength, iso, flash, cameramodel, longitude, latitude, altitude) VALUES (3,'2015-03-28 12:13:20',0.25,8,24,200,false,'Canon 600D',48.12,16.22,255);
INSERT INTO Exif(photo_id, date, exposure, aperture, focallength, iso, flash, cameramodel, longitude, latitude, altitude) VALUES (4,'2015-03-28 12:13:20',0.25,8,24,200,false,'Canon 600D',48.12,16.22,255);