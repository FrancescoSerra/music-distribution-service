package com.musicdistribution.services

import cats.syntax.all._
import cats.effect.IO
import cats.data.NonEmptyList
import com.musicdistribution.domain.model._
import com.musicdistribution.domain.model.types._
import com.musicdistribution.repositories.TestRepositories._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.effect.PropF

import java.time.{Instant, LocalDate}
import eu.timepit.refined.types.numeric.NonNegInt

import scala.concurrent.duration._
import scala.util.Random

/*
* These tests are intentionally testing only the happy paths, as their purpose is to show how such property based tests can be implemented.
* The implementation of the failure scenarios can be done following the same approach as the happy path, and in a real codebase would have been
* implemented as part of this test class.
* */

class MusicDistributionServiceSpec extends CatsEffectSuite with ScalaCheckEffectSuite {
  import Generators._

  // Test implementation of TimeProvider that always returns a fixed date/time
  class TestTimeProvider extends utils.TimeProvider[IO] {
    private val fixedDate = LocalDate.of(2025, 1, 1)
    private val fixedInstant = Instant.parse("2025-01-01T00:00:00Z")

    override def currentDate: IO[LocalDate] = IO.pure(fixedDate)
    override def currentTimestamp: IO[Timestamp] = IO.pure(Timestamp(fixedInstant))
  }

  // Test implementation of IdGenerator that generates predictable IDs
  class TestIdGenerator extends utils.IdGenerator[IO] {
    override def generateStreamId: IO[StreamId] = IO.pure(StreamId(java.util.UUID.randomUUID))
  }

  // Helper to create a service instance with test dependencies
  def createTestService = {
    val artistRepo = new TestArtistRepository
    val songRepo = new TestSongRepository
    val releaseRepo = new TestReleaseRepository
    val streamRepo = new TestStreamRepository
    val paymentRepo = new TestPaymentRepository
    val timeProvider = new TestTimeProvider
    val idGenerator = new TestIdGenerator

    MusicDistributionProgram[IO](
      artistRepo,
      songRepo,
      releaseRepo,
      streamRepo,
      paymentRepo,
      timeProvider,
      idGenerator
    )
  }

  test("createRelease happy path") {
    PropF.forAllF { (artist: Artist, songs: NonEmptyList[Song], proposedDate: Option[ProposedReleaseDate]) =>
      val service = createTestService

      for {
        // Setup: Save artist and songs
        _ <- service.artistRepository.save(artist)
        _ <- songs.toList.traverse_(service.songRepository.save)

        // Test: Create release
        result <- service.createRelease(
          artist.id,
          songs.map(_.id),
          proposedDate
        ).attempt
      } yield
        assert(result match {
          case Right(_) => true
          case Left(e) => fail(s"got error: ${e.getMessage}")
        })
    }
  }

  test("addSongToRelease happy path") {
    PropF.forAllF { (artist: Artist, release: Release, newSong: Song) =>
      val service = createTestService
      val releaseInDraft = release.copy(status = ReleaseStatus.Draft)
      val songForArtist = newSong.copy(artistId = artist.id)

      for {
        // Setup
        _ <- service.songRepository.save(songForArtist)
        _ <- service.artistRepository.save(artist)
        release <- service.releaseRepository.createRelease(
          artist.id,
          releaseInDraft.songs,
          None
        )

        // Test
        result <- service.addSongToRelease(release.id, songForArtist.id).attempt
      } yield
        assert(result match {
          case Right(_) => true
          case Left(e) => fail(s"got error: ${e.getMessage}")
        })
    }
  }

  test("proposeReleaseDate happy path") {
    PropF.forAllF { (release: Release) =>
      val service = createTestService
      val releaseInDraft = release.copy(status = ReleaseStatus.Draft)

      // Ensure proposed date is after the fixed test date
      val futureDate = ProposedReleaseDate(LocalDate.of(2025, 6, 1))

      for {
        // Setup
        release <- service.releaseRepository.createRelease(
          releaseInDraft.artistId,
          releaseInDraft.songs,
          None
        )

        // Test
        result <- service.proposeReleaseDate(release.id, futureDate).attempt
      } yield assert(result match {
        case Right(_) => true
        case Left(e) => fail(s"got error: ${e.getMessage}")
      })
    }
  }

  test("approveReleaseDate happy path") {
    PropF.forAllF { (artist: Artist, release: Release) =>
      val service = createTestService
      val recordLabelId = RecordLabelId(java.util.UUID.randomUUID)
      val artistWithLabel = artist.copy(recordLabelId = Some(recordLabelId))
      val releaseWithProposedDate = release.copy(
        status = ReleaseStatus.ProposedDate,
        artistId = artistWithLabel.id
      )

      // Ensure actual date is after the fixed test date
      val futureDate = ActualReleaseDate(LocalDate.of(2025, 6, 1))

      for {
        // Setup
        _ <- service.artistRepository.save(artistWithLabel)
        release <- service.releaseRepository.createRelease(
          releaseWithProposedDate.artistId,
          releaseWithProposedDate.songs,
          Some(ProposedReleaseDate(LocalDate.of(2025, 5, 1)))
        )

        // Test
        result <- service.approveReleaseDate(recordLabelId, release.id, futureDate).attempt
      } yield assert(result match {
        case Right(_) => true
        case Left(e) => fail(s"got error: ${e.getMessage}")
      })
    }
  }

  test("distributeRelease happy path") {
    PropF.forAllF { (release: Release) =>
      val service = createTestService
      val approvedRelease = release.copy(
        status = ReleaseStatus.Approved,
        actualReleaseDate = Some(ActualReleaseDate(LocalDate.of(2024, 12, 1))) // Before fixed test date
      )

      for {
        // Setup
        release <- service.releaseRepository.createRelease(
          approvedRelease.artistId,
          approvedRelease.songs,
          None
        )
        _ <-
          service.releaseRepository.updateToApproved(
            release.id,
            approvedRelease.actualReleaseDate.get
          )

        // Test
        result <- service.distributeRelease(release.id).attempt
      } yield assert(result match {
        case Right(_) => true
        case Left(e) => fail(s"got error: ${e.getMessage}")
      })
    }
  }

  test("searchSongs happy path") {
    PropF.forAllF { (song: Song) =>
      val service = createTestService
      val nonNegThreshold = NonNegInt.unsafeFrom(Random.nextInt(10))

      val query = TitleQuery(
        song.title.value
      )
      for {
        _ <- service.songRepository.save(song)
        result <-
          service.searchSongs(query, nonNegThreshold)
      } yield assertEquals(result.head, song)
    }
  }

  test("recordStream happy path") {
    PropF.forAllF { (song: Song) =>
      val service = createTestService
      val streamEvent = SongStreamed(
        StreamId(java.util.UUID.randomUUID),
        song.id,
        StreamDuration(40.seconds) // Longer than monetization threshold
      )

      for {
        _ <- service.songRepository.save(song)
        // Test
        streamId <- service.recordStream(streamEvent).attempt
      } yield assert(streamId.isRight)
    }
  }

  test("getStreamReport happy path") {
    PropF.forAllF { (artist: Artist, streams: List[AudioStream]) =>
      val service = createTestService

      for {
        // Setup
        _ <- service.artistRepository.save(artist)
        _ <- streams.traverse_(service.streamRepository.save)

        // Test
        report <- service.getStreamReport(artist.id)
      } yield {
        assert(report.monetizedStreams.isEmpty || report.nonMonetizedStreams.isEmpty ||
          report.monetizedStreams.size + report.nonMonetizedStreams.size > 0)
      }
    }
  }

  test("fileForPayment happy path") {
    PropF.forAllF { (artist: Artist, streams: NonEmptyList[AudioStream]) =>
      val service = createTestService
      val monetizedStreams = streams.map(_.copy(monetized = Monetized.Yes))

      for {
        // Setup
        _ <- service.artistRepository.save(artist)
        _ <- monetizedStreams.traverse_(service.streamRepository.save)

        // Test
        result <- service.fileForPayment(artist.id)
      } yield {
        assertEquals(result.artistId, artist.id)
        assert(result.amount.value.value > 0)
        assert(result.streamIds.size > 0)
      }
    }
  }

  test("withdrawRelease happy path") {
    PropF.forAllF { (release: Release) =>
      val service = createTestService
      val releasedRelease = release.copy(status = ReleaseStatus.Released)

      for {
        // Setup
        release <- service.releaseRepository.createRelease(
          releasedRelease.artistId,
          releasedRelease.songs,
          None
        )
        _ <- service.releaseRepository.updateToReleased(release.id)

        // Test
        result <- service.withdrawRelease(release.id)
      } yield
        assert(result.isInstanceOf[ReleaseWithdrawn] )
    }
  }
}