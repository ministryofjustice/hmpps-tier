create table if not exists ogrs4_rescored_assessment
(
    id              serial primary key,
    crn             char(7)       not null,
    completed_date  date          not null,
    arp_score       numeric(5, 2) not null,
    arp_is_dynamic  boolean       not null,
    arp_band        text          not null,
    csrp_score      numeric(5, 2) not null,
    csrp_is_dynamic boolean       not null,
    csrp_band       text          not null,
    dc_srp_score    numeric(5, 2) not null,
    dc_srp_band     text          not null,
    iic_srp_score   numeric(5, 2) not null,
    iic_srp_band    text          not null
);

create index ogrs4_rescored_assessment_crn_completed_date on ogrs4_rescored_assessment (crn, completed_date);