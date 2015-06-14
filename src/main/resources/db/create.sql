CREATE TABLE IF NOT EXISTS Journey(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR, start TIMESTAMP NOT NULL, end TIMESTAMP NOT NULL);
CREATE TABLE IF NOT EXISTS Place(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, city VARCHAR NOT NULL, country VARCHAR NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, journey_id INTEGER );
CREATE TABLE IF NOT EXISTS Photographer(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(30));
CREATE TABLE IF NOT EXISTS Tag(id INTEGER GENERATED BY DEFAULT  AS IDENTITY PRIMARY KEY, name VARCHAR(30));
CREATE TABLE IF NOT EXISTS Photo(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, photographer_id INTEGER REFERENCES Photographer(id), path VARCHAR NOT NULL, rating INTEGER NOT NULL DEFAULT 0, datetime TIMESTAMP NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, place_id INTEGER REFERENCES place(id), journey_id INTEGER REFERENCES journey(id));
CREATE TABLE IF NOT EXISTS PhotoTag(photo_id INTEGER REFERENCES Photo(id), tag_id INTEGER REFERENCES Tag(id), PRIMARY KEY(photo_id, tag_id));