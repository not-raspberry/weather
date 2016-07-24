# weather

A weather web app.

## Configuration

Supply an EDN file, similar to the ./env/dev/config/config.edn file.

Where to put the file? - see https://github.com/yogthos/config/.

# Tasks

`lein run <arg>` or `compiled.jar <arg>`, where `<arg>` is:

- `migrate` - upgrades the schema
- `rollback`- downgrades the last migration
- `initial-fetch` - Fetches the forecast, formats it, and saves to the database.
- `fetch` - Fetches the forecast, formats it, prepends with fake historical data, and saves to the
  database.

Without the argument, the web app is run.

## Requirements

JVM 1.8, Clojure 1.8, PostgreSQL 9.5 (won't run on 9.4 because of the use of upsert).

## License

Distributed under the Eclipse Public License. See the LICENSE file.

Copyright © 2016 not-raspberry (https://github.com/not-raspberry/).
