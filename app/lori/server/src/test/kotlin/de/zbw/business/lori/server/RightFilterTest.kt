package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.TemporalValidity
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnectorTest
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing filters concerning rights.
 *
 * Created on 10-12-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightFilterTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            gson = mockk(),
        ),
        mockk(),
    )

    private val itemRightRestricted = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "restricted right",
        collectionName = "subject1 subject2",
        publicationType = PublicationType.PROCEEDINGS,
    )
    private val itemRightRestrictedOpen = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "restricted and open right",
        collectionName = "subject3",
        publicationType = PublicationType.PROCEEDINGS,
    )
    private val tempValFilterPresent = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "validity filter present",
        collectionName = "validity",
    )

    private val tempValFilterPast = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "validity filter post",
        collectionName = "validity",
    )

    private val tempValFilterFuture = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "validity filter future",
        collectionName = "validity",
    )

    private val startEndDateFilter = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "start and end date At",
        collectionName = "startAndEnd",
    )

    private val formalRuleLicenceContract = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "formal rule filter licence contract",
        collectionName = "formalRuleLicence formal",
    )

    private val formalRuleUserAgreement = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "formal rule filter user agreement",
        collectionName = "formalRuleUserAgreement formal",
    )

    private val formalRuleOCL = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "formal rule filter ocl",
        collectionName = "ocl formal",
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        itemRightRestricted to listOf(TEST_RIGHT.copy(accessState = AccessState.RESTRICTED)),
        itemRightRestrictedOpen to listOf(
            TEST_RIGHT.copy(accessState = AccessState.RESTRICTED),
            TEST_RIGHT.copy(accessState = AccessState.OPEN),
        ),
        tempValFilterPresent to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2021, 6, 1),
                endDate = LocalDate.of(2021, 9, 1),
            )
        ),
        tempValFilterPast to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2021, 2, 1),
                endDate = LocalDate.of(2021, 3, 1),
            )
        ),
        tempValFilterFuture to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2021, 10, 1),
                endDate = LocalDate.of(2021, 12, 1),
            )
        ),
        startEndDateFilter to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2000, 10, 1),
                endDate = LocalDate.of(2000, 12, 1),
            ),
        ),
        formalRuleLicenceContract to listOf(
            TEST_RIGHT.copy(
                licenceContract = "licence",
                zbwUserAgreement = false,
            ),
        ),
        formalRuleUserAgreement to listOf(
            TEST_RIGHT.copy(
                zbwUserAgreement = true,
                licenceContract = null,
            ),
        ),
        formalRuleOCL to listOf(
            TEST_RIGHT.copy(
                openContentLicence = "foobar",
                licenceContract = null,
                zbwUserAgreement = false,
            ),
        ),
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns DatabaseConnectorTest.NOW.toInstant()
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
        getInitialMetadata().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertRight(right)
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_SEARCH_WITH_RIGHT_FILTER)
    fun createDataForSearchWithRightFilter() = arrayOf(
        arrayOf(
            "col:subject1",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.RESTRICTED)),
            ),
            setOf(itemRightRestricted, itemRightRestrictedOpen),
            2,
            "Filter for Access State Restricted for Item that has only one right"
        ),
        arrayOf(
            "col:subject1",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN)),
            ),
            setOf(itemRightRestrictedOpen),
            1,
            "Filter for Access State Open and expect one result with a similar collection name",
        ),
        arrayOf(
            "col:subject3",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN)),
            ),
            setOf(itemRightRestrictedOpen),
            1,
            "Filter for Access State Restricted for item that has multiple items",
        ),
        arrayOf(
            "col:startAndEnd",
            emptyList<MetadataSearchFilter>(),
            listOf(
                StartDateFilter(
                    LocalDate.of(2000, 10, 1)
                ),
            ),
            setOf(startEndDateFilter),
            1,
            "Filter for End Date",
        ),
        arrayOf(
            "col:startAndEnd",
            emptyList<MetadataSearchFilter>(),
            listOf(
                EndDateFilter(
                    LocalDate.of(2000, 12, 1)
                ),
            ),
            setOf(startEndDateFilter),
            1,
            "Filter for End Date",
        ),
    )

    @Test(dataProvider = DATA_FOR_SEARCH_WITH_RIGHT_FILTER)
    fun testSearchWithRightFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            givenSearchTerm,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            searchResult.numberOfResults,
            `is`(
                expectedNumberOfResults
            ),
        )
    }

    @DataProvider(name = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun createDataForGetItemWithRightFilter() =
        arrayOf(
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                        )
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED, AccessState.RESTRICTED)),
                ),
                setOf(itemRightRestricted, itemRightRestrictedOpen),
                "Filter for all access states"
            ),
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                        )
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.RESTRICTED)),
                ),
                setOf(itemRightRestricted, itemRightRestrictedOpen),
                "Filter for Access State Restricted"
            ),
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                        )
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN)),
                ),
                setOf(itemRightRestrictedOpen),
                "Filter for Access State Open for Item that has only one right"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    StartDateFilter(
                        LocalDate.of(2000, 10, 1)
                    ),
                ),
                setOf(startEndDateFilter),
                "Filter for Start Date"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    EndDateFilter(
                        LocalDate.of(2000, 12, 1)
                    ),
                ),
                setOf(startEndDateFilter),
                "Filter for End Date"
            ),
        )

    @Test(dataProvider = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun testRightFilterWithoutSearchTerm(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            null,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size)
        )
    }

    @DataProvider(name = DATA_FOR_NO_SEARCH_TEMP_VAL_FILTER)
    fun createDataForNoSearchTemValFilter() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.PRESENT)
                    )
                ),
                setOf(tempValFilterPresent),
                "Filter for all items that have an active right information"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.PAST)
                    )
                ),
                setOf(tempValFilterPast, startEndDateFilter),
                "Filter for all items that have an active right in the past"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.FUTURE)
                    )
                ),
                setOf(
                    tempValFilterFuture,
                    itemRightRestricted,
                    itemRightRestrictedOpen,
                    formalRuleLicenceContract,
                    formalRuleOCL,
                    formalRuleUserAgreement,
                ),
                "Filter for all items that have an active right in the future"
            ),
        )

    @Test(dataProvider = DATA_FOR_NO_SEARCH_TEMP_VAL_FILTER)
    fun testNoSearchTemporalValidityFilter(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            null,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size)
        )
    }

    @DataProvider(name = DATA_FOR_SEARCH_TEMP_VAL_FILTER)
    fun createDataForSearchTemValFilter() =
        arrayOf(
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.PRESENT)
                    )
                ),
                setOf(tempValFilterPresent),
                "Filter for all items that have an active right information"
            ),
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.PAST)
                    )
                ),
                setOf(tempValFilterPast),
                "Filter for all items that have an active right in the past"
            ),
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    TemporalValidityFilter(
                        temporalValidity = listOf(TemporalValidity.FUTURE)
                    )
                ),
                setOf(tempValFilterFuture),
                "Filter for all items that have an active right in the future"
            ),
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        date = LocalDate.of(2021, 10, 1)
                    )
                ),
                setOf(tempValFilterFuture),
                "Filter for items having an active right information at a certain point in time"
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_TEMP_VAL_FILTER)
    fun testSearchTemporalValidityFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            givenSearchTerm,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size)
        )
    }

    @DataProvider(name = DATA_FOR_SEARCH_FORMAL_RULE_FILTER)
    fun createDataForFormalRuleFilterTest() =
        arrayOf(
            arrayOf(
                "col:formalRuleLicence",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.LICENCE_CONTRACT)
                    )
                ),
                setOf(formalRuleLicenceContract),
                "formal rule licence contract"
            ),
            arrayOf(
                "col:formalRuleUserAgreement",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.ZBW_USER_AGREEMENT)
                    )
                ),
                setOf(formalRuleUserAgreement),
                "formal rule zbw agreement"
            ),
            arrayOf(
                "col:ocl",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.OPEN_CONTENT_LICENCE)
                    )
                ),
                setOf(formalRuleOCL),
                "formal rule ocl"
            ),
            arrayOf(
                "col:formal",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(
                            FormalRule.OPEN_CONTENT_LICENCE,
                            FormalRule.LICENCE_CONTRACT,
                            FormalRule.ZBW_USER_AGREEMENT,
                        )
                    )
                ),
                setOf(formalRuleOCL, formalRuleUserAgreement, formalRuleLicenceContract),
                "formal rule all"
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_FORMAL_RULE_FILTER)
    fun testSearchFormalRuleFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            givenSearchTerm,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size)
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_WITH_RIGHT_FILTER = "DATA_FOR_SEARCH_WITH_RIGHT_FILTER"
        const val DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER = "DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER"
        const val DATA_FOR_SEARCH_TEMP_VAL_FILTER = "DATA_FOR_SEARCH_TEMP_VAL_FILTER"
        const val DATA_FOR_NO_SEARCH_TEMP_VAL_FILTER = "DATA_FOR_NO_SEARCH_TEMP_VAL_FILTER"
        const val DATA_FOR_SEARCH_FORMAL_RULE_FILTER = "DATA_FOR_SEARCH_FORMAL_RULE_FILTER"

        val TEST_RIGHT = ItemRight(
            rightId = "123",
            accessState = AccessState.CLOSED,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            endDate = RestConverterTest.TODAY,
            lastUpdatedBy = "user2",
            lastUpdatedOn = OffsetDateTime.of(
                2022,
                3,
                2,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            startDate = RestConverterTest.TODAY.minusDays(1),
            licenceContract = "some contract",
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            zbwUserAgreement = true,
            groupIds = null,
        )
    }
}
