package com.musicdistribution.services

import cats.{MonadThrow, Parallel}
import cats.syntax.all._
import cats.data.NonEmptyList

import org.apache.commons.text.similarity.LevenshteinDistance
import com.musicdistribution.domain.model._
import com.musicdistribution.domain.model.types._
import com.musicdistribution.domain.model.ReleaseStatus._
import com.musicdistribution.repositories._
import com.musicdistribution.services.utils.{IdGenerator, TimeProvider}
import eu.timepit.refined.types.numeric._
import eu.timepit.refined.auto._

trait MusicDistributionService[F[_]] {
  // artist creates release
  def createRelease(
    artistId: ArtistId,
    songs: NonEmptyList[SongId],
    proposedReleaseDate: Option[ProposedReleaseDate]
  ): F[ReleaseCreated]
  
  // artist adds songs to a release
  def addSongToRelease(releaseId: ReleaseId, songId: SongId): F[SongAddedToRelease]
  
  // artist proposes a release date
  def proposeReleaseDate(releaseId: ReleaseId, date: ProposedReleaseDate): F[ReleaseDateProposed]
  
  // record label approves release date
  def approveReleaseDate(
    recordLabelId: RecordLabelId,
    releaseId: ReleaseId,
    date: ActualReleaseDate
  ): F[ReleaseDateApproved]
  
  // release songs for streaming
  def distributeRelease(releaseId: ReleaseId): F[ReleaseDistributed]
  
  // search for songs by title within some levenshtein distance threshold
  def searchSongs(query: TitleQuery, threshold: NonNegInt): F[List[Song]]
  
  // track streamed songs
  def recordStream(event: SongStreamed): F[StreamId]
  
  // get stream reports for artist
  def getStreamReport(artistId: ArtistId): F[StreamReport]
  
  // file for payment
  def fileForPayment(artistId: ArtistId): F[PaymentFiled]
  
  // withdraw release from distribution
  def withdrawRelease(releaseId: ReleaseId): F[ReleaseWithdrawn]
}

/*
* This class implements the core business logic. Each of its dependencies provides the set of low level behaviours that can be composed
* to support the user workflows, as per business requirements.
* */
case class MusicDistributionProgram[F[_]: MonadThrow: Parallel](
  artistRepository: ArtistRepository[F],
  songRepository: SongRepository[F],
  releaseRepository: ReleaseRepository[F],
  streamRepository: StreamRepository[F],
  paymentRepository: PaymentRepository[F],
  timeProvider: TimeProvider[F],
  idGenerator: IdGenerator[F]
) extends MusicDistributionService[F] {

  /*
  * All these methods should be invoked within transactional boundaries, so that iff the relative generated event gets successfully
  * dropped onto, for example, some relevant Kafka topic, then the DB transaction is committed
  *
  * assumption: an artist/record-label client can invoke the functions in this service only after it's been authenticated and authorised by some
  * front-end layer
  *
  * MONETIZATION_THRESHOLD_SECONDS and RATE_PER_STREAM could be passed in as parameters either per instance or as argument to the relevant functions - hence
  * they've been typed as refined types
  * */

  private val MONETIZATION_THRESHOLD_SECONDS: PosInt = 30
  private val RATE_PER_STREAM: PosBigDecimal = BigDecimal("0.003") // Example rate per stream

  /*
  * assumption: ideally an artist should be able to create a release when they have already created at least one song
  * */
  override def createRelease(
    artistId: ArtistId,
    songs: NonEmptyList[SongId],
    proposedReleaseDate: Option[ProposedReleaseDate]
  ): F[ReleaseCreated] = for {
    // assumes that the songs are created through a separate process
    foundSongs <- songRepository.howManyExist(songs)
    _ <- if (foundSongs == songs.size) MonadThrow[F].unit
         else new IllegalArgumentException(
           s"Some of the songs to be added to the release were not found: ${songs.toList.mkString(", ")}" // for logging purposes, we should be more precise for debugging though
         ).raiseError[F, Unit]
    release <- releaseRepository.createRelease(
      artistId,
      songs,
      proposedReleaseDate
    )
  } yield ReleaseCreated(
    artistId,
    release.id
  )

  /*
  * the validation of the existence of the release as well as the song could be done using Validated so that if both are missing,
  * then both errors can be shown
  * */
  override def addSongToRelease(releaseId: ReleaseId, songId: SongId): F[SongAddedToRelease] =
    for {
      (releaseOpt, songOpt) <- (releaseRepository.findById(releaseId),songRepository.findById(songId)).parTupled
      release <- releaseOpt.liftTo[F](new IllegalStateException("Release not found"))
      song <- songOpt.liftTo[F](new IllegalStateException("Song not found"))
      _ <- if (song.artistId == release.artistId) MonadThrow[F].unit
           else new IllegalArgumentException("Song must belong to the same artist as the release").raiseError[F, Unit]

      _ <- release.status match {
        case Draft => MonadThrow[F].unit
        case _ => new IllegalStateException("Can only add songs to a draft release").raiseError[F, Unit]
      }

      _ <- releaseRepository.addSongToRelease(release.id, song.id)
    } yield SongAddedToRelease(
      release.id,
      song.id
    )

  /*
  * assumption: only an artist can invoke this function to propose a release date
  * */
  override def proposeReleaseDate(releaseId: ReleaseId, date: ProposedReleaseDate): F[ReleaseDateProposed] =
    for {
      releaseOpt <- releaseRepository.findById(releaseId)
      release <- releaseOpt.liftTo[F](new IllegalStateException("Release not found"))
      _ <- release.status match {
        case Draft => MonadThrow[F].unit
        case _ => (new IllegalStateException("Can only propose dates for draft releases")).raiseError[F, Unit]
      }
      now <- timeProvider.currentDate
      _ <- if (date.value.isAfter(now)) MonadThrow[F].unit
           else new IllegalArgumentException("Proposed date must be in the future").raiseError[F, Unit]

      _ <- releaseRepository.updateToProposedDate(releaseId: ReleaseId, date: ProposedReleaseDate)
    } yield ReleaseDateProposed(
      release.id,
      date
    )

  /*
  * assumption: only a record label operator can invoke this function to approve the release
  * */
  override def approveReleaseDate(recordLabelId: RecordLabelId, releaseId: ReleaseId, date: ActualReleaseDate): F[ReleaseDateApproved] =
    for {
      releaseOpt <- releaseRepository.findById(releaseId)
      release <- releaseOpt.liftTo[F](new IllegalStateException("Release not found"))
      _ <- release.status match {
        case ProposedDate => MonadThrow[F].unit
        case _ => (new IllegalStateException("Can only approve dates for releases with proposed dates")).raiseError[F, Unit]
      }
      artistOpt <- artistRepository.findById(release.artistId)
      artist <- artistOpt.liftTo[F](new IllegalStateException("Artist not found"))
      artistRecordLabelId <- artist.recordLabelId.liftTo[F](new IllegalArgumentException("Unlabeled artists are temporarily out of scope"))
      _ <- if (recordLabelId == artistRecordLabelId) MonadThrow[F].unit
           else new IllegalStateException("A record label can only approve releases for its artists").raiseError[F, Unit]
      now <- timeProvider.currentDate
      _ <- if (date.value.isAfter(now)) MonadThrow[F].unit
           else new IllegalArgumentException("Approved date must be in the future").raiseError[F, Unit]

      _ <- releaseRepository.updateToApproved(
        release.id,
        date
      )
    } yield ReleaseDateApproved(
      release.id,
      date
    )

  /*
  * assumption: only a record label operator can invoke this function to approve the release or it could be
  * programmatically invoked whenever a ReleaseDateApproved event is consumed
  * */
  override def distributeRelease(releaseId: ReleaseId): F[ReleaseDistributed] =
    for {
      releaseOpt <- releaseRepository.findById(releaseId)
      release <- releaseOpt.liftTo[F](new IllegalStateException("Release not found"))
      _ <- release.status match {
        case Approved => MonadThrow[F].unit
        case _ => (new IllegalStateException("Can only distribute approved releases")).raiseError[F, Unit]
      }

      now <- timeProvider.currentDate
      releaseDate <- release.actualReleaseDate.liftTo[F](
        new IllegalStateException("Release date not set")
      )
      _ <- if (releaseDate.value.isBefore(now)) MonadThrow[F].unit else (
        new IllegalStateException("Release date has not been reached yet").raiseError[F, Unit]
      )
      _ <- releaseRepository.updateToReleased(release.id)
    } yield ReleaseDistributed(
      release.id
    )

  /*
  * assumption: a query is always populated with at least a character and a non-negative threshold
  * */
  override def searchSongs(query: TitleQuery, threshold: NonNegInt): F[List[Song]] =
    for {
      allSongs <- songRepository.findAllReleasedSongs
      matchedSongs = searchByLevenshteinDistance(allSongs, query.value, threshold)
    } yield matchedSongs

  private def searchByLevenshteinDistance(songs: List[Song], query: String, threshold: NonNegInt): List[Song] = {
    val levenshtein = LevenshteinDistance.getDefaultInstance

    songs.filter { song =>
      val distance = levenshtein.apply(query.toLowerCase, song.title.value.value.toLowerCase)
      distance <= threshold
    }
  }

  /*
  * this function is programmatically invoked whenever a song has been streamed
  * */
  override def recordStream(event: SongStreamed): F[StreamId] =
    for {
      _ <- songRepository.isSongReleased(event.songId).ifM(
          MonadThrow[F].unit,
          new IllegalStateException("Song was not released for streaming").raiseError[F,Unit] // ideally this check is performed at the point of streaming
      )
      streamId <- idGenerator.generateStreamId
      now <- timeProvider.currentTimestamp
      audioStream = AudioStream(
        id = streamId,
        songId = event.songId,
        duration = event.duration,
        timestamp = now,
        monetized = Monetized._Bool.reverseGet(event.duration.value.toSeconds >= MONETIZATION_THRESHOLD_SECONDS) // only streams longer than 30s are monetized
      )
      _ <- streamRepository.save(audioStream)
    } yield streamId

  /*
  * assumption: only an artist can invoke this function to get a report
  * */
  override def getStreamReport(artistId: ArtistId): F[StreamReport] =
    for {
      artistOpt <- artistRepository.findById(artistId)
      _ <- artistOpt.liftTo[F](new IllegalStateException("Artist not found"))
      streams <- streamRepository.findStreamsByArtistId(artistId)
      (monetized, notMonetized) = streams.partition(s => Monetized._Bool.get(s.monetized))
    } yield StreamReport(
      monetizedStreams = monetized,
      nonMonetizedStreams = notMonetized
    )

  /*
  * assumption: only an artist can invoke this function to file for a payment
  * */
  override def fileForPayment(artistId: ArtistId): F[PaymentFiled] =
    for {
      artistOpt <- artistRepository.findById(artistId)
      _ <- artistOpt.liftTo[F](new IllegalStateException("Artist not found"))
      unpaidStreams <- streamRepository.findUnpaidMonetizedStreamsByArtistId(artistId)

      streamIdsOpt = NonEmptyList.fromList(unpaidStreams.map(_.id))
      streamIds <- streamIdsOpt.liftTo[F](new IllegalStateException("No unpaid and monetized streams found"))
      amount <- calculatePaymentAmount(streamIds)
      now <- timeProvider.currentTimestamp
      payment = Payment(
        artistId = artistId,
        amount = amount,
        paidAt = now,
        streamsPaid = streamIds
      )
      _ <- paymentRepository.save(payment)
      _ <- streamRepository.markStreamsAsPaid(streamIds)
    } yield PaymentFiled(
      artistId,
      amount,
      streamIds
    )

  private def calculatePaymentAmount(streams: NonEmptyList[StreamId]): F[PaymentAmount] = {
      PosBigDecimal.from(streams.size * RATE_PER_STREAM.value).leftMap(_ => new IllegalStateException(
                     "This shouldn't be happening, as the size of a NEL is always positive, but the compiler can't prove it for us"
                   ))
                 .liftTo[F]
                   .map(PaymentAmount.apply)

  }

  /*
* assumption: only an artist can invoke this function to withdraw a release
* */
  override def withdrawRelease(releaseId: ReleaseId): F[ReleaseWithdrawn] =
    for {
      releaseOpt <- releaseRepository.findById(releaseId)
      release <- releaseOpt.liftTo[F](new IllegalStateException("Release not found"))
      _ <- release.status match {
        case Released => MonadThrow[F].unit
        case _ => new IllegalStateException("Can only withdraw released releases").raiseError[F, Unit]
      }

      _ <- releaseRepository.updateToWithdrawn(release.id)
    } yield ReleaseWithdrawn(release.id)
}
