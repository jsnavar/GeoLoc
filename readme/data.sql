-- some queries about the dataset
create materialized view if not exists stats as
select split_part(label, ';', 1) country, split_part(label, ';', 2) l1div, count(*) npoly
from country_pol
group by label;

select count(distinct country) ncountries
from stats;

select count(l1div) ndivisions
from stats;

select sum(npoly) polygons
from stats;

select country, sum(npoly) as np
from stats
group by country
order by np desc
limit 10;

select country, l1div, npoly
from stats s1
where country in (
  select country
  from stats
  group by country
  order by sum(npoly) desc
  limit 5)
and l1div in (
  select l1div
  from stats s2
  where s1.country = s2.country
  order by npoly desc
  limit 3
  )
order by country, npoly desc;

select country, sum(npoly) as npoly
from stats
group by country
having sum(npoly) < 15 and sum(npoly) > 5;

select label, jsonb_array_length(polygon) len
from country_pol
order by len desc
limit 25;

select avg(jsonb_array_length(polygon)),
       stddev(jsonb_array_length(polygon)),
       max(jsonb_array_length(polygon)),
       min(jsonb_array_length(polygon))
from country_pol;
