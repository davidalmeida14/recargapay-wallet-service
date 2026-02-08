#########
# Tasks #
#########


# Run linter check
#
#   make lint/check
#
lint/check:
	mvn spotless:check

# Run linter applies
#
#   make lint/apply
#
lint/apply:
	mvn spotless:apply

# Run sonar analysis without running tests
#
#   make sonar
#
sonar:
	mvn clean install sonar:sonar -Dmaven.test.skip=true -Dsonar.login=${SONAR_TOKEN}

# Run project tests
#
#   make test
#
test: dependencies/services test/unit test/run/integration/wallet test/run/e2e/wallet dependencies/clean/services

test/unit: test/lint/check test/run/unit
test/lint/check: lint/check
test/run/unit:
	ENVIRONMENT=test mvn -B test -Dgroups=unit

test/integration: dependencies/services test/run/integration/wallet dependencies/clean/services

test/run/integration/wallet:
	$(MAKE) dependencies/services
	ENVIRONMENT=test mvn -B test -Dgroups=integration
	$(MAKE) dependencies/clean/services

test/e2e: dependencies/services test/run/e2e/wallet dependencies/clean/services

test/run/e2e/wallet:
	$(MAKE) dependencies/services
	ENVIRONMENT=test mvn -B test -Dgroups=e2e
	$(MAKE) dependencies/clean/services

# Setup dependent services and third party dependencies
#
#   make dependencies/services
#
dependencies/services: dependencies/services/run db/migrate
dependencies/services/run:
	docker compose up -d postgres localstack redis
	docker compose exec -T postgres pg_isready -U postgres -d wallet || docker compose up -d --wait postgres
dependencies/clean/services:
	docker compose stop && docker compose rm -vf

# Build and start the application container
#
#   make up
#
up: dependencies/services/run db/migrate
	docker compose up -d --build app

# Stop and remove all containers
#
#   make down
#
down:
	docker compose down -v

# Setup the local development environment with python3 venv and project dependencies
#
#   make dev/setup
#
dev/setup:
	mvn compile

# Generate a new migration file that holds a database change
#
#   make db/migration name=add_new_table
#
db/migration:
	echo "--liquibase formatted sql\n--changeset PUT_YOUR_USERNAME_HERE:V$(shell date +%Y%m%d%H%M%S)__$(name)\n\n--rollback" > src/main/resources/db/migrations/V$(shell date +%Y%m%d%H%M%S)__$(name).sql

# Run the pending migrations against the configured database (default to docker/local database)
#
#   make db/migrate
#
db/migrate:
	$(_liquibase_cmd) update

# Prints the list of targets from this file
.PHONY: help
help:
	@$(MAKE) -pRrq -f $(lastword $(MAKEFILE_LIST)) : 2>/dev/null | awk -v RS= -F: '/^# File/,/^# Finished Make data base/ {if ($$1 !~ "^[#.]") {print $$1}}' | sort | egrep -v -e '^[^[:alnum:]]' -e '^$@$$'

###############
# Definitions #
###############

DATABASE_USER ?= postgres
DATABASE_PASSWORD ?= postgres
DATABASE_HOST ?= localhost
DATABASE_PORT ?= 5432
DATABASE_DBNAME ?= wallet
MIGRATE_DB_URL := postgresql://$(DATABASE_HOST):$(DATABASE_PORT)/$(DATABASE_DBNAME)?currentSchema=public

_liquibase_cmd = docker run --rm --net host -v ${PWD}/src/main/resources:/liquibase/changelog liquibase/liquibase:4.25 \
	--changeLogFile=db/changelog.xml \
	--username="$(DATABASE_USER)" \
  	--password="$(DATABASE_PASSWORD)" \
  	--url="jdbc:$(MIGRATE_DB_URL)"


version = $(shell git rev-parse --short HEAD | tr -d "\n")
