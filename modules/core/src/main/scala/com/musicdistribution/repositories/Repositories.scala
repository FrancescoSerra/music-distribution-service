package com.musicdistribution.repositories

import cats.data.NonEmptyList
import com.musicdistribution.domain.model._
import com.musicdistribution.domain.model.types._

/*
* these repositories are intentionally left as unimplemented traits - in real life, they would have a production code implementation using
* a library like doobie or skunk to interact with the DB, thus leveraging the interpreter pattern (with one such interpreter, and another one
* for the tests - in this occurrence, only the test interpreters have been implemented in the TestRepositories object)
* */

trait ArtistRepository[F[_]] {
  def findById(id: ArtistId): F[Option[Artist]]
  def save(artist: Artist): F[Unit]
}

trait SongRepository[F[_]] {
  def save(song: Song): F[Unit]
  def findById(id: SongId): F[Option[Song]]
  def howManyExist(songs: NonEmptyList[SongId]): F[Int]
  def findAllReleasedSongs: F[List[Song]]
  def isSongReleased(id: SongId): F[Boolean]
}

trait ReleaseRepository[F[_]] {
  def createRelease(
    artistId: ArtistId,
    songs: NonEmptyList[SongId],
    proposedReleaseDate: Option[ProposedReleaseDate]
  ): F[Release]
  def findById(id: ReleaseId): F[Option[Release]]
  def addSongToRelease(id: ReleaseId, songId: SongId): F[Unit]
  def updateToProposedDate(id: ReleaseId, proposedReleaseDate: ProposedReleaseDate): F[Unit] // this assumes that the ProposedDate status is inserted into DB in this function
  def updateToApproved(id: ReleaseId, actualReleaseDate: ActualReleaseDate): F[Unit] // this assumes that the Approved status is inserted into DB in this function
  def updateToReleased(id: ReleaseId): F[Unit] // this assumes that the Released status is inserted into DB in this function
  def updateToWithdrawn(id: ReleaseId): F[Unit] // this assumes that the Withdrawn status is inserted into DB in this function
}

trait StreamRepository[F[_]] {
  def save(stream: AudioStream): F[Unit]
  def findStreamsByArtistId(artistId: ArtistId): F[List[AudioStream]]
  def findUnpaidMonetizedStreamsByArtistId(artistId: ArtistId): F[List[AudioStream]]
  def markStreamsAsPaid(streamIds: NonEmptyList[StreamId]): F[Unit]
}

trait PaymentRepository[F[_]] {
  def save(payment: Payment): F[Unit]
}