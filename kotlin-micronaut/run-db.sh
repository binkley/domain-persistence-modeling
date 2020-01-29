# Assumes no processing needed for SQL file
case $# in
0) exec docker run -p 5432:5432 -e TZ=Etc/UTC -v "$(git rev-parse --show-toplevel)/kotlin-micronaut/src/main/resources/db/migration":/docker-entrypoint-initdb.d postgres:12.1 ;;
*) exec docker run -p 5432:5432 -e TZ=Etc/UTC postgres:12.1 ;;
esac
