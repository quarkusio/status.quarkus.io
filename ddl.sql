CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE hibernate_sequence OWNER TO quarkus;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE testexecution (
                                      id bigint NOT NULL,
                                      successful boolean NOT NULL,
                                      testname character varying(255),
                                      job_id bigint
);

ALTER TABLE testexecution OWNER TO quarkus;

CREATE TABLE testjob (
                                id bigint NOT NULL,
                                completedat timestamp without time zone,
                                name character varying(255),
                                sha character varying(255),
                                url character varying(255)
);

ALTER TABLE testjob OWNER TO quarkus;

ALTER TABLE ONLY testexecution
    ADD CONSTRAINT testexecution_pkey PRIMARY KEY (id);

ALTER TABLE ONLY testjob
    ADD CONSTRAINT testjob_pkey PRIMARY KEY (id);

ALTER TABLE ONLY testexecution
    ADD CONSTRAINT fkptkmwmynles1g3xwdo7iudrun FOREIGN KEY (job_id) REFERENCES testjob(id);

create index on testexecution (testname, id desc, successful);
create index on testexecution using btree (testname);