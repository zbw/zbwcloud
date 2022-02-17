package de.zbw.business.lori.server

import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.test.assertTrue

/**
 * Test [LoriServerBackend].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackendTest : DatabaseTest() {

    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
        )
    )

    private val orderedList = arrayOf(
        TEST_ACCESS_RIGHT.copy(itemMetadata = TEST_Metadata.copy(id = "aaaa")),
        TEST_ACCESS_RIGHT.copy(itemMetadata = TEST_Metadata.copy(id = "aaaa2")),
    )

    private val deletedEntry = TEST_ACCESS_RIGHT.copy(itemMetadata = TEST_Metadata.copy(id = "to_be_deleted"))
    private val noAction = TEST_ACCESS_RIGHT_NO_ACTION.copy(itemMetadata = TEST_Metadata.copy(id = "no_action"))

    private val entries = arrayOf(
        TEST_ACCESS_RIGHT,
        TEST_ACCESS_RIGHT.copy(itemMetadata = TEST_Metadata.copy(id = "test_2a")),
        TEST_ACCESS_RIGHT.copy(itemMetadata = TEST_Metadata.copy(id = "test_2b")),
        noAction,
        deletedEntry,
    ).plus(orderedList)

    @BeforeClass
    fun fillDatabase() {
        backend.insertAccessRightEntries(entries.toList())
    }

    @DataProvider(name = DATA_FOR_ROUNDTRIP)
    fun createDataForRoundtrip() =
        arrayOf(
            arrayOf(
                listOf(entries[0].itemMetadata.id),
                setOf(entries[0]),
            ),
            arrayOf(
                listOf(noAction.itemMetadata.id),
                setOf(noAction),
            ),
            arrayOf(
                listOf(entries[1].itemMetadata.id, entries[2].itemMetadata.id),
                setOf(entries[1], entries[2]),
            ),
            arrayOf(
                listOf(entries[0].itemMetadata.id, "invalidId"),
                setOf(entries[0]),
            ),
            arrayOf(
                listOf("invalidId"),
                setOf<Item>(),
            ),
        )

    @Test(dataProvider = DATA_FOR_ROUNDTRIP)
    fun testRoundtrip(
        queryIds: List<String>,
        expectedItems: Set<Item>
    ) {
        // when
        val received = backend.getAccessRightEntries(queryIds)
        // then
        assertThat(received.toSet(), `is`(expectedItems))
    }

    @Test
    fun testGetList() {
        // when
        val received = backend.getAccessRightList(limit = 2, offset = 0)
        // then
        assertThat(received, `is`(orderedList.toList()))

        // no limit
        val noLimit = backend.getAccessRightList(limit = 0, offset = 0)
        assertThat(noLimit, `is`(emptyList()))

        assertTrue(backend.containsAccessRightId(orderedList.first().itemMetadata.id))
    }

    @Test
    fun testDeleteEntry() {
        // when
        val deleted = backend.deleteAccessRightEntries(listOf(deletedEntry.itemMetadata.id))
        // then
        assertThat("One entry should have been deleted", deleted, `is`(1))
    }

    companion object {
        const val DATA_FOR_ROUNDTRIP = "DATA_FOR_ROUNDTRIP"

        val TEST_Metadata = ItemMetadata(
            id = "that-test",
            accessState = AccessState.OPEN,
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            licenseConditions = "some conditions",
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            provenanceLicense = "provenance license",
            publicationType = PublicationType.ARTICLE,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
        )

        private val TEST_RESTRICTION = Restriction(
            type = RestrictionType.DATE,
            attribute = Attribute(
                type = AttributeType.FROM_DATE,
                values = listOf("2022-01-01"),
            )
        )

        private val TEST_ACTION = Action(
            type = ActionType.READ,
            permission = true,
            restrictions = listOf(TEST_RESTRICTION),
        )

        val TEST_ACCESS_RIGHT = Item(
            itemMetadata = TEST_Metadata,
            actions = listOf(TEST_ACTION),
        )

        val TEST_ACCESS_RIGHT_NO_ACTION = Item(
            itemMetadata = TEST_Metadata,
            actions = emptyList(),
        )
    }
}