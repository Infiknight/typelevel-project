package com.rockthejvm.jobsboard.modules.jobsDao

import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.*

import cats.effect.kernel.Resource
import com.rockthejvm.jobsboard.modules.DoobieTestHelpers
import com.rockthejvm.jobsboard.dto.postgres.job.WriteJob
import doobie.free.connection.ConnectionIO

object TransactionPartsSpec extends weaver.IOSuite with doobie.weaver.IOChecker {

  override type Res = doobie.Transactor[IO]

  override def sharedResource =
    DoobieTestHelpers
      .postgres
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

  test("insert a job and delete it, should succeed") { xa =>
    val insertAndDelete =
      transactionParts.create(WriteJob.stub).flatMap { id => transactionParts.delete(id) }

    insertAndDelete.transact(xa).attempt.map { res => expect(res.isRight) }
  }

  lazy val transactionParts = TransactionParts.apply
}