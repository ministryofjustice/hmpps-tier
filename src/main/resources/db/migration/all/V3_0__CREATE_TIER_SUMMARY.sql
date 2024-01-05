create table if not exists tier_summary
(
    crn           varchar  not null primary key,
    uuid          UUID     not null,
    protect_level varchar  not null,
    change_level  smallint not null,
    version       integer  not null,
    last_modified timestamp
);