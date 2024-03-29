package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.sql.SQLException
import java.sql.Statement
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testing [RightDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertRightNoRowInsertedError() {
        // given
        val prepStmt = spyk(dbConnector.connection.prepareStatement(RightDB.STATEMENT_INSERT_RIGHT)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk(relaxed = true) {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            },
            tracer,
            mockk(),
        )
        // when
        dbConnectorMockked.rightDB.insertRight(TEST_RIGHT)
        // then exception
    }

    @Test
    fun testRightRoundtrip() {
        // given
        val initialRight = TEST_RIGHT

        // Insert
        // when
        val generatedRightId = dbConnector.rightDB.insertRight(initialRight)
        val receivedRights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(listOf(generatedRightId))

        // then
        assertThat(receivedRights.first(), `is`(initialRight.copy(rightId = generatedRightId, lastAppliedOn = null)))
        assertTrue(dbConnector.rightDB.rightContainsId(generatedRightId))

        // upsert

        // given
        val updatedRight =
            initialRight.copy(rightId = generatedRightId, lastUpdatedBy = "user2", accessState = AccessState.RESTRICTED)
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.plusDays(1).toInstant()

        // when
        val updatedRights = dbConnector.rightDB.upsertRight(updatedRight)

        // then
        assertThat(updatedRights, `is`(1))
        val receivedUpdatedRights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(listOf(generatedRightId))
        assertThat(receivedUpdatedRights.first(), `is`(updatedRight.copy(lastUpdatedOn = NOW.plusDays(1), lastAppliedOn = null)))

        // delete
        // when
        val deletedItems = dbConnector.rightDB.deleteRightsByIds(listOf(generatedRightId))

        // then
        assertThat(deletedItems, `is`(1))

        // when + then
        assertThat(dbConnector.rightDB.getRightsByIds(listOf(generatedRightId)), `is`(emptyList()))
        assertFalse(dbConnector.rightDB.rightContainsId(generatedRightId))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetRightException() {
        val dbConnector = DatabaseConnector(
            mockk(relaxed = true) {
                every { prepareStatement(any()) } throws SQLException()
            },
            tracer,
            mockk(),
        )
        dbConnector.rightDB.getRightsByIds(listOf("1"))
    }

    companion object {
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.RightDBTest")
    }
}
