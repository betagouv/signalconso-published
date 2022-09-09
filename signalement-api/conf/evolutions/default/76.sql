-- !Ups

create table IF NOT EXISTS report_consumer_review
(
    id uuid not null constraint report_consumer_review_pkey primary key,
    report_id uuid  not null,
    creation_date timestamp with time zone not null,
    evaluation        varchar                  not null,
    details       varchar
);

ALTER TABLE report_consumer_review DROP CONSTRAINT IF EXISTS fk_report_consumer_review;
ALTER TABLE report_consumer_review ADD CONSTRAINT fk_report_consumer_review FOREIGN KEY (report_id) REFERENCES reports(id);

insert into report_consumer_review (
    select
        e.id,
        e.report_id,
        e.creation_date,
        CASE WHEN split_part(e.details->>'description', ' - ', 1) = 'Avis négatif' THEN 'Negative' ELSE 'Positive' END,
        -- splitting on the specific char sequence 'tif - ' to avoid unwanted split on the - char
        -- when details is empty putting null in column
        CASE WHEN coalesce( trim(split_part(e.details->>'description', 'tif - ', 2)),'')='' THEN null ELSE split_part(e.details->>'description', 'tif - ', 2) END
    from events e
    where e.event_type = 'CONSO' and e.action = 'Avis du consommateur sur la réponse du professionnel') ON CONFLICT DO NOTHING
;

-- !Downs

ALTER TABLE report_consumer_review DROP CONSTRAINT fk_report_consumer_review;
drop table  report_consumer_review;