insert into ogrs4_rescored_assessment(id, crn, completed_date, arp_score, arp_is_dynamic, arp_band, csrp_score,
                                      csrp_is_dynamic, csrp_band, dc_srp_score, dc_srp_band, iic_srp_score,
                                      iic_srp_band)
values (1, 'X765432', current_date - 7, 75.00, true, 'HIGH', 1.00, false, 'LOW', 0.00, 'LOW', 0.00, 'LOW');
insert into ogrs4_rescored_assessment(id, crn, completed_date, arp_score, arp_is_dynamic, arp_band, csrp_score,
                                      csrp_is_dynamic, csrp_band, dc_srp_score, dc_srp_band, iic_srp_score,
                                      iic_srp_band)
values (2, 'X765432', current_date - 400, 25.00, true, 'LOW', 2.50, false, 'MEDIUM', 0.00, 'LOW', 0.00, 'LOW');
