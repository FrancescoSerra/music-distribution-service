package com.musicdistribution.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import com.musicdistribution.domain.model._
import com.musicdistribution.domain.model.types._
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

import java.time._
import java.util.UUID
import cats.syntax.all._
import com.musicdistribution.domain.model.ReleaseStatus.{Approved, Released}
import eu.timepit.refined.types.numeric._
import eu.timepit.refined.types.string._

import scala.collection.mutable
import scala.concurrent.duration._

/**
 * Example of Test implementations of the repository interfaces.
 * These are intended to be used in tests, providing in-memory implementations
 * of the repositories
 */
object TestRepositories {
  
  // Generators for domain types
  object Generators {
    val genUUID: Gen[UUID] = Gen.uuid
    
    val genArtistId: Gen[ArtistId] = genUUID.map(ArtistId(_))
    val genSongId: Gen[SongId] = genUUID.map(SongId(_))
    val genReleaseId: Gen[ReleaseId] = genUUID.map(ReleaseId(_))
    val genStreamId: Gen[StreamId] = genUUID.map(StreamId(_))
    
    val genNonEmptyString: Gen[NonEmptyString] =
      Gen
        .chooseNum(1, 200)
        .flatMap { n =>
          Gen.buildableOfN[String, Char](n, Gen.alphaNumChar)
        }
        .map(s => NonEmptyString.unsafeFrom(s))
    
    val genArtistName: Gen[ArtistName] = genNonEmptyString.map(ArtistName.apply)
    val genSongTitle: Gen[SongTitle] = genNonEmptyString.map(SongTitle.apply)

    val genRecordLabelId: Gen[RecordLabelId] = genUUID.map(RecordLabelId(_))
    val genRecordLabelName: Gen[RecordLabelName] = genNonEmptyString.map(RecordLabelName(_))

    val genPosInt: Gen[PosInt] =
      Gen.posNum[Int].map(PosInt.unsafeFrom)
    
    val genPosBigDecimal: Gen[PosBigDecimal] =
      Gen.posNum[BigDecimal].map(PosBigDecimal.unsafeFrom)
    
    val genPaymentAmount: Gen[PaymentAmount] = genPosBigDecimal.map(PaymentAmount(_))
    
    val genLocalDate: Gen[LocalDate] = 
      for {
        year <- Gen.choose(2000, 2030)
        month <- Gen.choose(1, 12)
        day <- Gen.choose(1, 28) // Keep it simple to avoid invalid dates
      } yield LocalDate.of(year, month, day)
    
    val genProposedReleaseDate: Gen[ProposedReleaseDate] = genLocalDate.map(ProposedReleaseDate(_))
    val genActualReleaseDate: Gen[ActualReleaseDate] = genLocalDate.map(ActualReleaseDate(_))
    
    val genInstant: Gen[Instant] = Gen.chooseNum(0L, System.currentTimeMillis()).map(Instant.ofEpochMilli)
    val genTimestamp: Gen[Timestamp] = genInstant.map(Timestamp(_))
    
    val genDuration: Gen[FiniteDuration] = Gen.chooseNum[Long](1, 600).map(_.seconds)
    val genStreamDuration: Gen[StreamDuration] = genDuration.map(StreamDuration(_))
    
    val genReleaseStatus: Gen[ReleaseStatus] = Gen.oneOf(
      ReleaseStatus.Draft, 
      ReleaseStatus.ProposedDate, 
      ReleaseStatus.Approved, 
      ReleaseStatus.Released, 
      ReleaseStatus.Withdrawn
    )
    
    val genMonetized: Gen[Monetized] = Gen.oneOf(Monetized.Yes, Monetized.No)
    
    val genArtist: Gen[Artist] = for {
      id <- genArtistId
      name <- genArtistName
      labelId <- Gen.option(genUUID.map(RecordLabelId(_)))
    } yield Artist(id, name, labelId)

    val genRecordLabel: Gen[RecordLabel] = for {
      id <- genRecordLabelId
      name <- genRecordLabelName
    } yield RecordLabel(id, name)

    val genSong: Gen[Song] = for {
      id <- genSongId
      title <- genSongTitle
      artistId <- genArtistId
      duration <- genPosInt
    } yield Song(id, title, artistId, duration)
    
    def genNonEmptyList[A](gen: Gen[A]): Gen[NonEmptyList[A]] = 
      for {
        head <- gen
        tail <- Gen.listOf(gen)
      } yield NonEmptyList(head, tail)
    
    val genRelease: Gen[Release] = for {
      id <- genReleaseId
      artistId <- genArtistId
      songs <- genNonEmptyList(genSongId)
      proposedDate <- Gen.option(genProposedReleaseDate)
      actualDate <- Gen.option(genActualReleaseDate)
      status <- genReleaseStatus
    } yield Release(id, artistId, songs, proposedDate, actualDate, status)
    
    val genAudioStream: Gen[AudioStream] = for {
      id <- genStreamId
      songId <- genSongId
      duration <- genStreamDuration
      timestamp <- genTimestamp
      monetized <- genMonetized
    } yield AudioStream(id, songId, duration, timestamp, monetized)
    
    val genPayment: Gen[Payment] = for {
      artistId <- genArtistId
      amount <- genPaymentAmount
      paidAt <- genTimestamp
      streamsPaid <- genNonEmptyList(genStreamId)
    } yield Payment(artistId, amount, paidAt, streamsPaid)
    
    // Set up Arbitrary instances for ScalaCheck
    implicit val arbArtistId: Arbitrary[ArtistId] = Arbitrary(genArtistId)
    implicit val arbSongId: Arbitrary[SongId] = Arbitrary(genSongId)
    implicit val arbReleaseId: Arbitrary[ReleaseId] = Arbitrary(genReleaseId)
    implicit val arbStreamId: Arbitrary[StreamId] = Arbitrary(genStreamId)
    implicit val arbArtist: Arbitrary[Artist] = Arbitrary(genArtist)
    implicit val arbSong: Arbitrary[Song] = Arbitrary(genSong)
    implicit val arbRelease: Arbitrary[Release] = Arbitrary(genRelease)
    implicit val arbAudioStream: Arbitrary[AudioStream] = Arbitrary(genAudioStream)
    implicit val arbPayment: Arbitrary[Payment] = Arbitrary(genPayment)
    implicit val arbProposedReleaseDate: Arbitrary[ProposedReleaseDate] = Arbitrary(genProposedReleaseDate)
    implicit val arbActualReleaseDate: Arbitrary[ActualReleaseDate] = Arbitrary(genActualReleaseDate)
    implicit val arbTimestamp: Arbitrary[Timestamp] = Arbitrary(genTimestamp)
    implicit val arbStreamDuration: Arbitrary[StreamDuration] = Arbitrary(genStreamDuration)
    implicit val arbPaymentAmount: Arbitrary[PaymentAmount] = Arbitrary(genPaymentAmount)

    implicit val arbMonetized: Arbitrary[Monetized] = Arbitrary(genMonetized)
    implicit val arbReleaseStatus: Arbitrary[ReleaseStatus] = Arbitrary(genReleaseStatus)

    implicit val arbRecordLabel: Arbitrary[RecordLabel] = Arbitrary(genRecordLabel)

    implicit def arbNonEmptyList[A: Arbitrary]: Arbitrary[NonEmptyList[A]] =
      Arbitrary(genNonEmptyList(Arbitrary.arbitrary[A]))
  }
  
  /**
   * Test implementation of ArtistRepository for use in tests.
   */
  class TestArtistRepository extends ArtistRepository[IO] {
    private val artistsRef = mutable.Map.empty[ArtistId, Artist]
    
    override def findById(id: ArtistId): IO[Option[Artist]] = 
      artistsRef.get(id).pure[IO]
    
    override def save(artist: Artist): IO[Unit] =
      artistsRef.update(artist.id, artist).pure[IO]
  }
  
  /**
   * Test implementation of SongRepository for use in tests.
   */
  class TestSongRepository extends SongRepository[IO] {
    private val songsRef = mutable.Map.empty[SongId, Song]
    
    override def findById(id: SongId): IO[Option[Song]] =
      songsRef.get(id).pure[IO]
    
    override def howManyExist(songs: NonEmptyList[SongId]): IO[Int] =
      songs.toList.count(songsRef.keySet.contains).pure[IO]
    
    override def findAllReleasedSongs: IO[List[Song]] =
      songsRef.values.toList.pure[IO]
    
    override def isSongReleased(id: SongId): IO[Boolean] =
      true.pure[IO]

    override def save(song: Song): IO[Unit] = songsRef.update(song.id, song).pure[IO]
  }
  
  /**
   * Test implementation of ReleaseRepository for use in tests.
   */
  class TestReleaseRepository extends ReleaseRepository[IO] {
    private val releasesRef = mutable.Map.empty[ReleaseId, Release]
    
    override def createRelease(
      artistId: ArtistId,
      songs: NonEmptyList[SongId],
      proposedReleaseDate: Option[ProposedReleaseDate]
    ): IO[Release] = {
      val id = ReleaseId(UUID.randomUUID())
      val release = Release(
        id = id,
        artistId = artistId,
        songs = songs,
        proposedReleaseDate = proposedReleaseDate,
        actualReleaseDate = None,
        status = if (proposedReleaseDate.isDefined) ReleaseStatus.ProposedDate else ReleaseStatus.Draft
      )
      
      releasesRef.update(id, release).pure[IO].as(release)
    }
    
    override def findById(id: ReleaseId): IO[Option[Release]] =
      releasesRef.get(id).pure[IO]
    
    override def addSongToRelease(id: ReleaseId, songId: SongId): IO[Unit] =
      IO.unit
    
    override def updateToProposedDate(id: ReleaseId, proposedReleaseDate: ProposedReleaseDate): IO[Unit] =
      IO.unit
    
    override def updateToApproved(id: ReleaseId, actualReleaseDate: ActualReleaseDate): IO[Unit] =
      releasesRef.updateWith(id)(_.map(_.copy(status = Approved, actualReleaseDate = actualReleaseDate.some))).pure[IO].void
    
    override def updateToReleased(id: ReleaseId): IO[Unit] =
      releasesRef.updateWith(id)(_.map(_.copy(status = Released))).pure[IO].void
    
    override def updateToWithdrawn(id: ReleaseId): IO[Unit] =
      IO.unit
  }
  
  /**
   * Test implementation of StreamRepository for use in tests.
   */
  class TestStreamRepository extends StreamRepository[IO] {
    private val streamsRef = mutable.Map.empty[StreamId, AudioStream]
    override def save(stream: AudioStream): IO[Unit] =
      streamsRef.update(stream.id, stream).pure[IO]
    
    override def findStreamsByArtistId(artistId: ArtistId): IO[List[AudioStream]] =
      streamsRef.values.toList.pure[IO]
    
    override def findUnpaidMonetizedStreamsByArtistId(artistId: ArtistId): IO[List[AudioStream]] =
      findStreamsByArtistId(artistId)
    
    override def markStreamsAsPaid(streamIds: NonEmptyList[StreamId]): IO[Unit] =
      IO.unit
  }
  
  /**
   * Test implementation of PaymentRepository for use in tests.
   */
  class TestPaymentRepository extends PaymentRepository[IO] {
    override def save(payment: Payment): IO[Unit] =
      IO.unit
  }
}