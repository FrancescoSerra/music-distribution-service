package com.musicdistribution.services.utils

import java.time.{LocalDate, Instant}
import com.musicdistribution.domain.model.types._

trait TimeProvider[F[_]] {
  def currentDate: F[LocalDate]
  def currentTimestamp: F[Timestamp]
}

trait IdGenerator[F[_]] {
  def generateArtistId: F[ArtistId]
  def generateSongId: F[SongId]
  def generateReleaseId: F[ReleaseId]
  def generateStreamId: F[StreamId]
  def generateRecordLabelId: F[RecordLabelId]
}