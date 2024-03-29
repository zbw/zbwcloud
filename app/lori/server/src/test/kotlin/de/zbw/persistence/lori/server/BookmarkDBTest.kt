package de.zbw.persistence.lori.server

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.type.Bookmark
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [BookmarkDB].
 *
 * Created on 03-16-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    ).bookmarkDB

    @Test
    fun testBookmarkRoundtrip() {
        // Case: Create and Read
        // when
        val generatedId = dbConnector.insertBookmark(TEST_BOOKMARK)
        val receivedBookmarks = dbConnector.getBookmarksByIds(listOf(generatedId))
        // then
        assertThat(receivedBookmarks.first().toString(), `is`(TEST_BOOKMARK.copy(bookmarkId = generatedId).toString()))

        // Case: Update
        val expectedBMUpdated = TEST_BOOKMARK.copy(noRightInformationFilter = NoRightInformationFilter())
        val updatedBMs = dbConnector.updateBookmarkById(generatedId, expectedBMUpdated)
        assertThat(updatedBMs, `is`(1))
        assertThat(
            expectedBMUpdated.copy(bookmarkId = generatedId).toString(),
            `is`(
                dbConnector.getBookmarksByIds(listOf(generatedId)).first().toString()
            )
        )
        // Case: Delete
        val countDeleted = dbConnector.deleteBookmarkById(generatedId)
        assertThat(countDeleted, `is`(1))
        val receivedBookmarksAfterDeletion = dbConnector.getBookmarksByIds(listOf(generatedId))
        // then
        assertThat(receivedBookmarksAfterDeletion, `is`(emptyList()))
    }

    @Test
    fun testGetBookmarkList() {
        val bookmark1 = TEST_BOOKMARK.copy(description = "foo")
        val receivedId1 = dbConnector.insertBookmark(bookmark1)
        val expected1 = bookmark1.copy(bookmarkId = receivedId1)
        assertThat(
            dbConnector.getBookmarkList(50, 0).toString(),
            `is`(
                listOf(
                    expected1
                ).toString()
            )
        )

        val bookmark2 = TEST_BOOKMARK.copy(description = "bar")
        val receivedId2 = dbConnector.insertBookmark(bookmark2)
        val expected2 = bookmark2.copy(bookmarkId = receivedId2)

        assertThat(
            dbConnector.getBookmarkList(50, 0).toString(),
            `is`(
                listOf(
                    expected1,
                    expected2,
                ).toString()
            )
        )
    }

    companion object {
        val TEST_BOOKMARK = Bookmark(
            bookmarkId = 1,
            bookmarkName = "test",
            description = "some description",
            searchPairs = LoriServerBackend.parseValidSearchPairs("tit:someTitle"),
            publicationDateFilter = QueryParameterParser.parsePublicationDateFilter("2020-2030"),
            publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter("BOOK,ARTICLE"),
            accessStateFilter = QueryParameterParser.parseAccessStateFilter("OPEN,RESTRICTED"),
            temporalValidityFilter = QueryParameterParser.parseTemporalValidity("FUTURE,PAST"),
            validOnFilter = QueryParameterParser.parseRightValidOnFilter("2018-04-01"),
            startDateFilter = QueryParameterParser.parseStartDateFilter("2020-01-01"),
            endDateFilter = QueryParameterParser.parseEndDateFilter("2021-12-31"),
            formalRuleFilter = QueryParameterParser.parseFormalRuleFilter("ZBW_USER_AGREEMENT"),
            paketSigelFilter = QueryParameterParser.parsePaketSigelFilter("sigel"),
            zdbIdFilter = QueryParameterParser.parseZDBIdFilter("zdbId1,zdbId2"),
            noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter("false"),
        )
    }
}
