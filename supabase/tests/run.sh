#!/usr/bin/env bash
# Runs the database tests against a plain Postgres 16 (the CI service container, or a local one).
#
#   PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres supabase/tests/run.sh
#
# Creates a fresh database, adds the Supabase stand-ins (bootstrap.sql), applies every migration
# twice (the Supabase GitHub integration may re-run migrations first applied by hand, so each one
# must be safe to run again), then runs every test file. Each test runs in its own transaction and
# rolls back. Exits non-zero if anything fails.
set -uo pipefail

cd "$(dirname "$0")/../.."
export PGOPTIONS="${PGOPTIONS:-} -c client_min_messages=warning"
db="${TEST_DATABASE:-fortresstv_test}"
psql_db() { psql -X -q -v ON_ERROR_STOP=1 -d "$db" "$@"; }

psql -X -q -v ON_ERROR_STOP=1 -d postgres \
    -c "drop database if exists \"$db\"" -c "create database \"$db\"" >/dev/null || exit 1
psql_db -f supabase/tests/bootstrap.sql >/dev/null || { echo "::error::bootstrap.sql failed"; exit 1; }

failed=0
for pass in 1 2; do
    for migration in supabase/migrations/*.sql; do
        if ! output=$(psql_db -f "$migration" 2>&1 >/dev/null); then
            echo "::error file=$migration::Migration failed on run $pass"
            echo "$output"
            failed=1
        fi
    done
done
[ "$failed" -eq 0 ] && echo "All migrations applied twice"

for test in supabase/tests/*.sql; do
    [ "$(basename "$test")" = "bootstrap.sql" ] && continue
    if output=$(psql_db -f "$test" 2>&1); then
        echo "PASS $(basename "$test")"
    else
        echo "::error file=$test::Test failed"
        echo "$output" | grep -E -A3 "ERROR" | head -20
        failed=1
    fi
done

exit "$failed"
