
CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


CREATE FUNCTION new_event(tid bigint, msg text) RETURNS bigint
    LANGUAGE plpgsql
    AS $$DECLARE
  tx_id bigint;
  event_id bigint;
  BEGIN
    INSERT INTO tx(ts) VALUES(now());

    SELECT MAX(id) INTO tx_id FROM tx;

    INSERT INTO "event"(tx, topic_id, content)
      VALUES (tx_id, tid, msg);

    SELECT MAX(id) INTO event_id FROM "event";

    INSERT INTO notification(tx, event_id, user_id)
      SELECT tx_id, event_id, user_id
        FROM active_subscriptions
       WHERE topic_id = tid;

   RETURN tx_id;
  END;$$;


CREATE FUNCTION new_tx() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
 DECLARE
   current_tx bigint;
    BEGIN
        INSERT INTO tx(ts) VALUES(now());
	SELECT MAX(id) INTO current_tx FROM tx;
        NEW.tx := current_tx;
        RETURN NEW;
    END;
$$;


CREATE TABLE topic (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    tx bigint NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    service_id integer NOT NULL
);


CREATE VIEW active_services AS
 SELECT t1.id,
    t1.service_id,
    t1.name
   FROM topic t1,
    ( SELECT topic.service_id,
            topic.name,
            max(topic.tx) AS tx
           FROM topic
          GROUP BY topic.service_id, topic.name) t2
  WHERE ((((t1.service_id = t2.service_id) AND ((t1.name)::text = (t2.name)::text)) AND (t1.tx = t2.tx)) AND (t1.deleted = false));


CREATE TABLE subscription (
    id integer NOT NULL,
    user_id integer NOT NULL,
    topic_id integer NOT NULL,
    tx bigint NOT NULL,
    deleted boolean DEFAULT false NOT NULL
);


CREATE VIEW active_subscriptions AS
 SELECT s1.id,
    s1.user_id,
    s1.topic_id
   FROM subscription s1,
    ( SELECT subscription.user_id,
            subscription.topic_id,
            max(subscription.tx) AS tx
           FROM subscription
          GROUP BY subscription.user_id, subscription.topic_id) s2
  WHERE ((((s1.user_id = s2.user_id) AND (s1.topic_id = s2.topic_id)) AND (s1.tx = s2.tx)) AND (s1.deleted = false));


CREATE VIEW active_topics AS
 SELECT t1.id,
    t1.service_id,
    t1.name
   FROM topic t1,
    ( SELECT topic.service_id,
            topic.name,
            max(topic.tx) AS tx
           FROM topic
          GROUP BY topic.service_id, topic.name) t2
  WHERE ((((t1.service_id = t2.service_id) AND ((t1.name)::text = (t2.name)::text)) AND (t1.tx = t2.tx)) AND (t1.deleted = false));


CREATE TABLE "user" (
    id integer NOT NULL,
    email character varying(100) NOT NULL,
    service_id integer NOT NULL,
    tx bigint NOT NULL,
    deleted boolean DEFAULT false NOT NULL
);


CREATE VIEW active_users AS
 SELECT u1.id,
    u1.email,
    u1.service_id
   FROM "user" u1,
    ( SELECT "user".email,
            "user".service_id,
            max("user".tx) AS tx
           FROM "user"
          GROUP BY "user".email, "user".service_id) u2
  WHERE (((((u1.email)::text = (u2.email)::text) AND (u1.service_id = u2.service_id)) AND (u1.tx = u2.tx)) AND (u1.deleted = false));


CREATE TABLE event (
    id bigint NOT NULL,
    tx bigint NOT NULL,
    topic_id integer NOT NULL,
    content text NOT NULL
);


CREATE SEQUENCE event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE event_id_seq OWNED BY event.id;


CREATE TABLE notification (
    id bigint NOT NULL,
    tx bigint NOT NULL,
    event_id bigint NOT NULL,
    read boolean DEFAULT false NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    user_id integer NOT NULL
);


CREATE SEQUENCE notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE notification_id_seq OWNED BY notification.id;


CREATE TABLE service (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    tx bigint NOT NULL,
    deleted boolean DEFAULT false NOT NULL
);


CREATE SEQUENCE service_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE service_id_seq OWNED BY service.id;


CREATE SEQUENCE subscription_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE subscription_id_seq OWNED BY subscription.id;


CREATE SEQUENCE topic_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE topic_id_seq OWNED BY topic.id;


CREATE TABLE tx (
    id bigint NOT NULL,
    ts timestamp without time zone DEFAULT now() NOT NULL
);


CREATE SEQUENCE tx_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE tx_id_seq OWNED BY tx.id;


CREATE SEQUENCE user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE user_id_seq OWNED BY "user".id;


ALTER TABLE ONLY event ALTER COLUMN id SET DEFAULT nextval('event_id_seq'::regclass);


ALTER TABLE ONLY notification ALTER COLUMN id SET DEFAULT nextval('notification_id_seq'::regclass);


ALTER TABLE ONLY service ALTER COLUMN id SET DEFAULT nextval('service_id_seq'::regclass);


ALTER TABLE ONLY subscription ALTER COLUMN id SET DEFAULT nextval('subscription_id_seq'::regclass);


ALTER TABLE ONLY topic ALTER COLUMN id SET DEFAULT nextval('topic_id_seq'::regclass);


ALTER TABLE ONLY tx ALTER COLUMN id SET DEFAULT nextval('tx_id_seq'::regclass);


ALTER TABLE ONLY "user" ALTER COLUMN id SET DEFAULT nextval('user_id_seq'::regclass);


ALTER TABLE ONLY event
    ADD CONSTRAINT event_pk PRIMARY KEY (id);


ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pk PRIMARY KEY (id);


ALTER TABLE ONLY service
    ADD CONSTRAINT services_pkey PRIMARY KEY (id);


ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_pk PRIMARY KEY (id);


ALTER TABLE ONLY topic
    ADD CONSTRAINT topic_pk PRIMARY KEY (id);


ALTER TABLE ONLY tx
    ADD CONSTRAINT tx_pk PRIMARY KEY (id);


ALTER TABLE ONLY "user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


CREATE INDEX fki_event_tx_fk ON event USING btree (tx);


CREATE INDEX fki_service_tx_fk ON service USING btree (tx);


CREATE INDEX fki_topic_tx_fk ON topic USING btree (tx);


CREATE INDEX notification_user_idx ON notification USING btree (user_id, deleted);


CREATE INDEX user_tx_idx ON "user" USING btree (email, tx DESC);


CREATE TRIGGER new_service_trg BEFORE INSERT ON service FOR EACH ROW EXECUTE PROCEDURE new_tx();


CREATE TRIGGER new_subscription_trg BEFORE INSERT ON subscription FOR EACH ROW EXECUTE PROCEDURE new_tx();


CREATE TRIGGER new_topic_trg BEFORE INSERT ON topic FOR EACH ROW EXECUTE PROCEDURE new_tx();


CREATE TRIGGER new_user_trg BEFORE INSERT ON "user" FOR EACH ROW EXECUTE PROCEDURE new_tx();


ALTER TABLE ONLY event
    ADD CONSTRAINT event_topic_fk FOREIGN KEY (topic_id) REFERENCES topic(id);


ALTER TABLE ONLY event
    ADD CONSTRAINT event_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_event_fk FOREIGN KEY (event_id) REFERENCES event(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY service
    ADD CONSTRAINT services_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_topic_fk FOREIGN KEY (topic_id) REFERENCES topic(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY subscription
    ADD CONSTRAINT subscription_user_fk FOREIGN KEY (user_id) REFERENCES "user"(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY topic
    ADD CONSTRAINT topic_service_fk FOREIGN KEY (service_id) REFERENCES service(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY topic
    ADD CONSTRAINT topic_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


ALTER TABLE ONLY "user"
    ADD CONSTRAINT user_tx_fk FOREIGN KEY (tx) REFERENCES tx(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
