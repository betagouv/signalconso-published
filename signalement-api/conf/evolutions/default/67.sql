-- !Ups

CREATE TABLE consumer(
    id UUID PRIMARY KEY DEFAULT UUID_GENERATE_V4(),
    name VARCHAR NOT NULL UNIQUE,
    creation_date TIMESTAMP NOT NULL DEFAULT now(),
    api_key VARCHAR NOT NULL,
    delete_date TIMESTAMP
);

INSERT INTO consumer (name, api_key) VALUES ('ReponseConso', '$2a$10$c846BHlor87tCsp3y.VVreZpshBW.HcWp2hrUGVTYUzTtjb1Hd72W');
INSERT INTO consumer (name, api_key) VALUES ('DataEconomie', '$2a$10$zPkX1mST2HnNuLjYloJcYu4QrkGUD6SAlm6hs77vCwXvZVHH4VmWi');

-- !Downs

DROP TABLE consumer;
