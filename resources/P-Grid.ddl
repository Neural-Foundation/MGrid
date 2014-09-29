create table HOSTS (
     HOST_ID integer AUTO_INCREMENT,
     GUID varchar not null,
     ADDRESS varchar not null,
     PORT numeric not null,
     QOS varchar,
     PATH varchar not null,
     TEMP numeric not null DEFAULT 0,
     constraint U_HOST unique(GUID),
     constraint ID_HOSTS primary key (HOST_ID));

create table INDEX_SIGNATURE_CACHE (
    HOST_ID integer not null,
    SIGNATURE varchar not null,
    constraint U_SIG_HOST unique(HOST_ID)
);

create table INDEX_ITEMS (
     KEY varchar not null,
     ID bigint not null,
     GUID varchar not null,
   /*  TYPE_NAME varchar not null, */
     HOST_ID integer not null,
   /*  DATA varchar_ignorecase, */
     DATA_ID integer not null,
     constraint U_INDEX_ITEMS unique(GUID)
);



create table MESSAGES_TABLE (
     MESSAGE_ID integer AUTO_INCREMENT,
     GUID varchar not null,
     MESSAGE MEDIUMBLOB not null);

create table INDEX_TABLES (
     INDEX_TABLE_ID integer AUTO_INCREMENT,
     HOST_ID integer not null,
     SIGNATURE varchar,
     constraint U_INDEXHOST unique(HOST_ID),
     constraint ID_INDEX_TABLES primary key (INDEX_TABLE_ID));

create table Config (
    KEY varchar not null,
    VALUE varchar,
    constraint U_CONFIG unique(KEY),
    constraint ID_CONFIG primary key (KEY));


CREATE UNIQUE INDEX I_MESSAGE ON MESSAGES_TABLE (GUID);

/*alter table INDEX_TABLES add constraint FKDataHost
     foreign key (HOST_ID)
     references HOSTS on delete cascade; */



create table FIDGET_LIST (
     HOST_ID integer not null,
     FID_HOST_ID integer not null,
     constraint U_FIDGET unique(HOST_ID, FID_HOST_ID),
     constraint ID_FIDGET_LIST primary key (HOST_ID, FID_HOST_ID));

