\o ./stats.txt

select current_timestamp "Date de l'extraction";

-- Nb utilisateurs DGCCRF
select count(*) "Nb utilisateurs DGCCRF" from users where role = 'DGCCRF';

-- Nb utilisateurs DGCCRF actifs
select count(*) "Nb utilisateurs DGCCRF actifs" from users
where role = 'DGCCRF'
and users.last_email_validation > 'now'::timestamp - '90 days'::interval;;

-- Nb utilisateurs DGCCRF connectés au mois de novembre
select count(distinct(u.id)) "Nb utilisateurs DGCCRF connectés au mois de novembre"
from users u, auth_attempts a
where u.role = 'DGCCRF'
and u.email = a.login
and timestamp >= '2021-11-01'::timestamp
and timestamp <= '2021-12-01'::timestamp;

-- Nb abonnements DGCCRF
select count(*) "Nb abonnements DGCCRF"
from subscriptions s, users u
where u.role = 'DGCCRF'
and u.id = s.user_id
and u.last_email_validation > 'now'::timestamp - '90 days'::interval;

-- Nb d'entreprises pour lesquelles un contrôle DGCCRF a été déclaré au mois de novembre
select count(distinct company_id) "Nb d''entreprises controlées au mois de novembre"
from events
where action = 'Contrôle effectué'
and creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp;

-- % entreprises signalées avec un compte activé
select ((count(distinct(a.company_id))::numeric / count(distinct(r.company_id))::numeric) * 100)::numeric(5,2) "% entreprises signalées avec un compte activé"
from reports r
left join company_accesses a on r.company_id = a.company_id
where r.company_id is not null;

-- % signalements lus (signalements du mois de novembre)
select ((count(*) filter ( where status in ('Transmis', 'PromesseAction', 'Infonde', 'MalAttribue', 'ConsulteIgnore') ))::numeric /
(count(*) filter ( where status not in ('NA', 'LanceurAlerte') ))::numeric * 100)::numeric(5,2) "Signalements du mois de novembre : % lus"
from reports
where reports.creation_date > '2021-12-01'::timestamp - '81 days'::interval;

-- % signalements avec une réponse (Signalements du mois de novembre)
select ((count(*) filter ( where status in ('PromesseAction', 'Infonde', 'MalAttribue') ))::numeric /
(count(*) filter ( where status not in ('NA', 'LanceurAlerte') ))::numeric * 100)::numeric(5,2) "Signalements du mois de novembre : % avec une réponse"
from reports
where creation_date > '2021-12-01'::timestamp - '81 days'::interval;


-- temps de lecture moyen des signalements (signalements du mois de novembre)
drop table if exists read_delay_table;

select round(extract(epoch from events.creation_date - reports.creation_date) / 60) as read_delay
into temporary table read_delay_table
from events, reports
where reports.id = events.report_id
  and action = 'Première consultation du signalement par le professionnel'
    and reports.creation_date >= '2021-11-01'::timestamp
    and reports.creation_date <= '2021-12-01'::timestamp;

select (percentile_cont(0.5) within group ( order by read_delay) / (60 * 24))::numeric(6,2) "Signalements du mois de novembre : temps de lecture moyen"
from read_delay_table;


-- répartition des réponses (réponses du mois de novembre)
select count(1) "Nb réponses du mois de novembre",
((count(1) filter ( where  details->>'responseType' = 'ACCEPTED'))::numeric / count(1) * 100)::numeric(5,2) "% promesse d'action",
((count(1) filter ( where  details->>'responseType' = 'REJECTED'))::numeric / count(1) * 100)::numeric(5,2) "% infondé",
((count(1) filter ( where  details->>'responseType' = 'NOT_CONCERNED'))::numeric / count(1) * 100)::numeric(5,2) "% mal attribué"
from events
where action = 'Réponse du professionnel au signalement'
and creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp;

-- avis sur la réponse du Pro (avis du mois de novembre)
select count(1) "Nb avis du mois de novembre sur la réponse Pro",
((count(1) filter ( where  details->>'description' like 'Avis positif%'))::numeric / count(1) * 100)::numeric(5,2) "% avis positif",
((count(1) filter ( where  details->>'description' like 'Avis négatif%'))::numeric / count(1) * 100)::numeric(5,2) "% avis négatif"
from events
where action = 'Avis du consommateur sur la réponse du professionnel'
and creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp;

-- avis sur les pages d'info
select count(1) "Nb avis du mois de novembre sur les pages d'info",
((count(1) filter ( where  positive = true))::numeric / count(1) * 100)::numeric(5,2) "% avis positif",
((count(1) filter ( where  positive = false))::numeric / count(1) * 100)::numeric(5,2) "% avis négatif"
from ratings
where creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp;

--  avis sur les pages d'info dispatché par actions (avis des 200 derniers jours)
select category "Catégorie", count(1) "Nb avis des 200 derniers jours sur les pages d'info",
((count(1) filter ( where  positive = true))::numeric / count(1) * 100)::numeric(5,2) "% avis positif",
((count(1) filter ( where  positive = false))::numeric / count(1) * 100)::numeric(5,2) "% avis négatif"
from ratings
where creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp
group by category;

-- % signalements internets (signalements des 30 derniers jours)
select count(1) "Nb signalements des 30 derniers jours",
       ((count(*) filter ( where website_url is not null ))::numeric / count(1)::numeric * 100)::numeric(5,2) "% signalement internet",
       ((count(*) filter ( where website_url is not null and company_id is not null and not exists(
               select * from events e where e.report_id = reports.id and e.action = 'Modification du commerçant'
           )))::numeric / (count(*) filter ( where website_url is not null ))::numeric * 100)::numeric(5,2) "% sign entreprise ident par conso parmi les sign internets",
       ((count(*) filter ( where website_url is not null and company_id is not null and exists(
               select * from events e where e.report_id = reports.id and e.action = 'Modification du commerçant'
           )))::numeric / (count(*) filter ( where website_url is not null ))::numeric * 100)::numeric(5,2) "% sign entreprise ident par admin parmi les sign internets",
       ((count(*) filter ( where website_url is not null and company_id is null ))::numeric / (count(*) filter ( where website_url is not null ))::numeric * 100)::numeric(5,2) "% sign entreprise non ident par le conso parmi les sign internets",
       ((count(*) filter ( where website_url is not null and company_id is null and company_country is not null))::numeric / (count(*) filter ( where website_url is not null and company_id is null ))::numeric * 100)::numeric(5,2) "% sign entreprises de pays étrangers ident parmi sign entr non identifiées"
from reports
where creation_date >= '2021-11-01'::timestamp
and creation_date <= '2021-12-01'::timestamp;

\o