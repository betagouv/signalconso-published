-- !Ups

CREATE TABLE report_notifications_blocked
(
    user_id       UUID      NOT NULL,
    company_id    UUID      NOT NULL,
    date_creation TIMESTAMP NOT NULL
);

ALTER TABLE report_notifications_blocked
    ADD CONSTRAINT unique_row UNIQUE (user_id, company_id);

ALTER TABLE report_notifications_blocked
    ADD CONSTRAINT fk_report_notifications_blocked_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE report_notifications_blocked
    ADD CONSTRAINT fk_report_notifications_blocked_company
        FOREIGN KEY (company_id) REFERENCES companies (id);

-- !Downs

DROP TABLE report_notifications_blocked;