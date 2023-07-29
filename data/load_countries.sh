#!/bin/bash

DATABASE=geodb
TABLE=country_pol

# directory with GeoJSON files
JSON_DIR=./gadm

for json_doc in ${JSON_DIR}/*.json
do
    LOAD_CMDS+="\set doc \`cat ${json_doc}\` \\\\ INSERT INTO tmp VALUES(:'doc'); "
done

psql -d ${DATABASE} <<EOF
START TRANSACTION;
CREATE TABLE IF NOT EXISTS ${TABLE} (label TEXT, polygon JSONB);
TRUNCATE TABLE ${TABLE};

CREATE TEMP TABLE tmp (doc JSONB);

${LOAD_CMDS}

INSERT INTO ${TABLE}
SELECT CONCAT(ft -> 'properties' ->> 'COUNTRY', ';', ft -> 'properties' ->> 'NAME_1'), jsonb_array_elements(ft -> 'geometry' -> 'coordinates') -> 0
FROM tmp CROSS JOIN LATERAL jsonb_array_elements(doc -> 'features') AS ft;

COMMIT;
EOF

unset DATABASE
unset TABLE
unset JSON_DIR
unset LOAD_CMDS
