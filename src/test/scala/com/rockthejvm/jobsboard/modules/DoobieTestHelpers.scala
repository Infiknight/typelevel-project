package com.rockthejvm.jobsboard.modules

import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.effect.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import org.testcontainers.containers.PostgreSQLContainer
import com.zaxxer.hikari.HikariConfig
import doobieTestHelpers.EmbeddedPg

class DoobieTestHelpers(transactorResource: Resource[IO, doobie.Transactor[IO]]) {
  def transactorRsIncludingSetup = transactorResource
    .evalTap { xa =>
      val setupDBQuery = sql"""
              CREATE TABLE jobs(
                  id uuid DEFAULT gen_random_uuid(),
                  date bigint NOT NULL,
                  ownerEmail text NOT NULL,
                  company text NOT NULL,
                  title text NOT NULL,
                  description text NOT NULL,
                  externalUrl text NOT NULL DEFAULT false,
                  remote boolean NOT NULL DEFAULT false,
                  location text NOT NULL,
                  salaryLo integer,
                  salaryHi integer, 
                  currency text,
                  country text,
                  tags text[],
                  image text,
                  seniority text,
                  other text,
                  active BOOLEAN NOT NULL DEFAULT false
              );

              ALTER TABLE jobs
              ADD CONSTRAINT pk_jobs PRIMARY KEY (id);

              SET DEFAULT_TRANSACTION_ISOLATION TO SERIALIZABLE ;
              """.update

      setupDBQuery.run.transact(xa)
    }
    .map(xa => doobie.Transactor.after.set(xa, doobie.HC.rollback))
}
