alter table ogrs4_rescored_assessment
    alter column completed_date type timestamptz;
alter table ogrs4_rescored_assessment
    alter column arp_score drop not null;
alter table ogrs4_rescored_assessment
    alter column arp_is_dynamic drop not null;
alter table ogrs4_rescored_assessment
    alter column arp_band drop not null;
alter table ogrs4_rescored_assessment
    alter column csrp_score drop not null;
alter table ogrs4_rescored_assessment
    alter column csrp_is_dynamic drop not null;
alter table ogrs4_rescored_assessment
    alter column csrp_band drop not null;
alter table ogrs4_rescored_assessment
    alter column dc_srp_score drop not null;
alter table ogrs4_rescored_assessment
    alter column dc_srp_band drop not null;
alter table ogrs4_rescored_assessment
    alter column iic_srp_score drop not null;
alter table ogrs4_rescored_assessment
    alter column iic_srp_band drop not null;