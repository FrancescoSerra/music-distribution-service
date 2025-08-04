package com.musicdistribution.domain

import io.estatico.newtype.macros.newtype
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.types.numeric._
import cats.data.NonEmptyList
import monocle.Iso

import java.time._
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object model {

  // NewTypes for domain entities
  object types {
    @newtype case class ArtistId(value: UUID)
    @newtype case class SongId(value: UUID)
    @newtype case class ReleaseId(value: UUID)
    @newtype case class RecordLabelId(value: UUID)
    @newtype case class StreamId(value: UUID)
    @newtype case class SongTitle(value: NonEmptyString)
    @newtype case class ArtistName(value: NonEmptyString)
    @newtype case class RecordLabelName(value: NonEmptyString)
    @newtype case class StreamDuration(value: FiniteDuration)
    @newtype case class PaymentAmount(value: PosBigDecimal)
    @newtype case class Timestamp(value: Instant)
    @newtype case class ProposedReleaseDate(value: LocalDate)
    @newtype case class ActualReleaseDate(value: LocalDate)
    @newtype case class TitleQuery(value: NonEmptyString)
  }

  import types._


  // Domain models
  final case class Artist(
    id: ArtistId,
    name: ArtistName,
    recordLabelId: Option[RecordLabelId]
  )

  final case class RecordLabel(
    id: RecordLabelId,
    name: RecordLabelName
  )

  final case class Song(
    id: SongId,
    title: SongTitle,
    artistId: ArtistId,
    durationSeconds: PosInt
  )

  sealed trait ReleaseStatus
  object ReleaseStatus {
    case object Draft extends ReleaseStatus
    case object ProposedDate extends ReleaseStatus
    case object Approved extends ReleaseStatus
    case object Released extends ReleaseStatus
    case object Withdrawn extends ReleaseStatus
  }

  final case class Release(
    id: ReleaseId,
    artistId: ArtistId,
    songs: NonEmptyList[SongId],
    proposedReleaseDate: Option[ProposedReleaseDate],
    actualReleaseDate: Option[ActualReleaseDate],
    status: ReleaseStatus
  )


  // isomorphic type to Boolean, to avoid boolean blindness
  sealed trait Monetized
  object Monetized {
    case object Yes extends Monetized
    case object No extends Monetized

    val _Bool: Iso[Monetized, Boolean] = Iso[Monetized, Boolean] {
      case Yes => true
      case No => false
    }(if (_) Yes else No)
  }

  final case class AudioStream(
    id: StreamId,
    songId: SongId,
    duration: StreamDuration,
    timestamp: Timestamp,
    monetized: Monetized
  )

  final case class Payment(
    artistId: ArtistId,
    amount: PaymentAmount,
    paidAt: Timestamp,
    streamsPaid: NonEmptyList[StreamId]
  )

  final case class StreamReport(
    monetizedStreams: List[AudioStream],
    nonMonetizedStreams: List[AudioStream]
  )

  sealed trait Event
  final case class ReleaseCreated(artistId: ArtistId, releaseId: ReleaseId) extends Event
  final case class SongAddedToRelease(releaseId: ReleaseId, songId: SongId) extends Event
  final case class ReleaseDateProposed(releaseId: ReleaseId, proposedDate: ProposedReleaseDate) extends Event
  final case class ReleaseDateApproved(releaseId: ReleaseId, approvedDate: ActualReleaseDate) extends Event
  final case class ReleaseDistributed(releaseId: ReleaseId) extends Event
  final case class SongStreamed(streamId: StreamId, songId: SongId, duration: StreamDuration) extends Event
  final case class PaymentFiled(artistId: ArtistId, amount: PaymentAmount, streamIds: NonEmptyList[StreamId]) extends Event
  final case class ReleaseWithdrawn(releaseId: ReleaseId) extends Event
}