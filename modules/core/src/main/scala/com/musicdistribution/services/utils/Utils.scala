package com.musicdistribution.services.utils

import java.time._
import com.musicdistribution.domain.model.types._

trait TimeProvider[F[_]] {
  def currentDate: F[LocalDate]
  def currentTimestamp: F[Timestamp]
}

trait IdGenerator[F[_]] {
  def generateStreamId: F[StreamId]
}