-- SubTable definition

CREATE TABLE SubTable ( submail varchar(20),subname varchar(20),subsurname varchar(20),subpaswd varchar(20),primary key (submail));


-- TableTracker definition

CREATE TABLE TableTracker (tablename varchar(20),submail varchar(20),operation varchar(20), tcreatTime bigint,tlastUpdate bigint, primary key (tablename));


-- SessionTable definition

CREATE TABLE SessionTable ( submail varchar(20),session varchar(20),screatTime bigint, slastUpdate bigint, isActive boolean as (slastUpdate-screatTime < 2), foreign key (submail) references SubTable(submail));


-- RecordTable definition

CREATE TABLE RecordTable ( submail varchar(20),rcreatTime bigint, rlastUpdate bigint, isOnline boolean , foreign key (submail) references SubTable(submail),foreign key (isOnline) references SessionTable(isActive));