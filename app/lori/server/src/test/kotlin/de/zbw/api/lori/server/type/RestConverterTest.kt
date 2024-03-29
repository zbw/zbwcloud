package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.connector.DAConnectorTest.Companion.TEST_SUBCOMMUNITY
import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.lori.model.AccessStateWithCountRest
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PaketSigelWithCountRest
import de.zbw.lori.model.PublicationTypeWithCountRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateIdWithCountRest
import de.zbw.lori.model.ZdbIdWithCountRest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RestConverterTest {

    @Test
    fun testItemConversion() {
        // given
        val expected = Item(
            metadata = TEST_METADATA,
            rights = listOf(TEST_RIGHT)
        )

        val restObject = ItemRest(
            metadata = MetadataRest(
                metadataId = TEST_METADATA.metadataId,
                author = TEST_METADATA.author,
                band = TEST_METADATA.band,
                collectionName = TEST_METADATA.collectionName,
                collectionHandle = TEST_METADATA.collectionHandle,
                communityHandle = TEST_METADATA.communityHandle,
                communityName = TEST_METADATA.communityName,
                createdBy = TEST_METADATA.createdBy,
                createdOn = TEST_METADATA.createdOn,
                doi = TEST_METADATA.doi,
                handle = TEST_METADATA.handle,
                isbn = TEST_METADATA.isbn,
                issn = TEST_METADATA.issn,
                lastUpdatedBy = TEST_METADATA.lastUpdatedBy,
                lastUpdatedOn = TEST_METADATA.lastUpdatedOn,
                paketSigel = TEST_METADATA.paketSigel,
                ppn = TEST_METADATA.ppn,
                publicationType = TEST_METADATA.publicationType.toRest(),
                publicationDate = TEST_METADATA.publicationDate,
                rightsK10plus = TEST_METADATA.rightsK10plus,
                subCommunitiesHandles = TEST_METADATA.subCommunitiesHandles,
                storageDate = TEST_METADATA.storageDate,
                title = TEST_METADATA.title,
                titleJournal = TEST_METADATA.titleJournal,
                titleSeries = TEST_METADATA.titleSeries,
                zdbId = TEST_METADATA.zdbId,
            ),
            rights = listOf(
                RightRest(
                    rightId = TEST_RIGHT.rightId,
                    accessState = TEST_RIGHT.accessState?.toRest(),
                    authorRightException = TEST_RIGHT.authorRightException,
                    basisAccessState = TEST_RIGHT.basisAccessState?.toRest(),
                    basisStorage = TEST_RIGHT.basisStorage?.toRest(),
                    createdBy = TEST_RIGHT.createdBy,
                    createdOn = TEST_RIGHT.createdOn,
                    endDate = TEST_RIGHT.endDate,
                    lastAppliedOn = TEST_RIGHT.lastAppliedOn,
                    lastUpdatedBy = TEST_RIGHT.lastUpdatedBy,
                    lastUpdatedOn = TEST_RIGHT.lastUpdatedOn,
                    licenceContract = TEST_RIGHT.licenceContract,
                    nonStandardOpenContentLicence = TEST_RIGHT.nonStandardOpenContentLicence,
                    nonStandardOpenContentLicenceURL = TEST_RIGHT.nonStandardOpenContentLicenceURL,
                    notesGeneral = TEST_RIGHT.notesGeneral,
                    notesFormalRules = TEST_RIGHT.notesFormalRules,
                    notesProcessDocumentation = TEST_RIGHT.notesProcessDocumentation,
                    notesManagementRelated = TEST_RIGHT.notesManagementRelated,
                    openContentLicence = TEST_RIGHT.openContentLicence,
                    restrictedOpenContentLicence = TEST_RIGHT.restrictedOpenContentLicence,
                    startDate = TEST_RIGHT.startDate,
                    templateDescription = TEST_RIGHT.templateDescription,
                    templateName = TEST_RIGHT.templateName,
                    zbwUserAgreement = TEST_RIGHT.zbwUserAgreement,
                )
            ),
        )

        // when + then
        assertThat(restObject.toBusiness(), `is`(expected))
        assertThat(restObject.toBusiness().toRest(), `is`(restObject))
    }

    @Test
    fun testDAItemConverter() {
        // given
        val expected = ItemMetadata(
            metadataId = "5",
            author = "Colbjørnsen, Terje",
            band = null,
            collectionHandle = "colHandle",
            collectionName = "Collectionname",
            communityName = "Communityname",
            communityHandle = "comHandle",
            createdBy = null,
            createdOn = null,
            doi = null,
            handle = "some_handle",
            isbn = null,
            issn = null,
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            paketSigel = null,
            ppn = null,
            publicationType = PublicationType.ARTICLE,
            publicationDate = LocalDate.of(2022, 9, 1),
            rightsK10plus = null,
            storageDate = OffsetDateTime.of(
                2022,
                1,
                19,
                7,
                57,
                26,
                0,
                ZoneOffset.UTC,
            ),
            subCommunitiesHandles = listOf("11159/1114"),
            title = "some_title",
            titleJournal = "some_journal",
            titleSeries = "some_series",
            zdbId = null,
        )

        // when
        val receivedItem = TEST_DA_ITEM.toBusiness()
        // then
        assertThat(receivedItem, `is`(expected))

        // when + then
        val receivedItem2 = TEST_DA_ITEM.copy(handle = null)
        assertThat(receivedItem2, `is`(receivedItem2))
    }

    @Test
    fun testParseToDate() {
        // when + then
        assertThat(
            RestConverter.parseToDate("2022"),
            `is`(LocalDate.of(2022, 1, 1))
        )
        assertThat(
            RestConverter.parseToDate("2022-09"),
            `is`(LocalDate.of(2022, 9, 1))
        )
        assertThat(
            RestConverter.parseToDate("2022-09-02"),
            `is`(LocalDate.of(2022, 9, 2))
        )
        assertThat(
            RestConverter.parseToDate("2022/09"),
            `is`(LocalDate.of(2022, 9, 1))
        )
        assertThat(
            RestConverter.parseToDate("foo"),
            `is`(LocalDate.of(1970, 1, 1))
        )
    }

    @Test
    fun testGroupConverter() {
        val givenGroup = Group(
            name = "some name",
            description = "description",
            entries = listOf(
                GroupEntry(
                    organisationName = "some orga",
                    ipAddresses = "123.456.1.127",
                ),
            ),
        )
        assertThat(
            (givenGroup.toRest()).toBusiness(),
            `is`(givenGroup),
        )
    }

    @DataProvider(name = DATA_FOR_PARSE_TO_GROUP)
    fun createDataForParseToGroup() =
        arrayOf(
            arrayOf(
                true,
                "\"Organisation\",\"IP-Address\",\"Foobar\"\n\"organisation1\",\"192.168.82.1.124\"",
                true,
                emptyList<GroupEntry>(),
                "wrong delimiter with headers leads to error",
            ),
            arrayOf(
                false,
                "organisation1;192.168.82.1",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                ),
                "simple case one line",
            ),
            arrayOf(
                false,
                "organisation1;192.168.82.1\norganisation2;192.68.254.*,195.37.13.*,195.37.209.160-191,195.37.234.33-46",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.68.254.*,195.37.13.*,195.37.209.160-191,195.37.234.33-46"
                    ),
                ),
                "simple case two lines",
            ),
            arrayOf(
                false,
                "\n\norganisation1;192.168.82.1\norganisation2;192.168.82.1\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1"
                    ),
                ),
                "empty newline at the end",
            ),
            arrayOf(
                false,
                "organisation1;",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = ""
                    ),
                ),
                "IP address is missing",
            ),
            arrayOf(
                false,
                "",
                false,
                emptyList<GroupEntry>(),
                "Nothing to parse",
            ),
            arrayOf(
                false,
                "organisation1;192.168.82.1\norganisation2;192.168.82.1;",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1"
                    ),
                ),
                "parse correct even with trailing comma",
            ),
            arrayOf(
                true,
                "\nOrganisation;IP-Address\norganisation1;192.168.82.1\norganisation2;192.168.82.1\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1"
                    ),
                ),
                "with header line",
            ),
            arrayOf(
                true,
                "\nOrganisation,IP-Address\norganisation1,192.168.82.1\norganisation2,192.168.82.1\n\n",
                true,
                emptyList<GroupEntry>(),
                "error due to wrong separator",
            ),
            arrayOf(
                true,
                "\nOrganisation;IP-Address;Foobar\norganisation1;192.168.82.1;124\norganisation2;192.168.82.1;1234\n\n",
                false,
                listOf(
                    GroupEntry(
                        organisationName = "organisation1",
                        ipAddresses = "192.168.82.1"
                    ),
                    GroupEntry(
                        organisationName = "organisation2",
                        ipAddresses = "192.168.82.1"
                    ),
                ),
                "more columns than expected will be accepted as well.",
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_TO_GROUP)
    fun testParseToGroup(
        hasCSVHeader: Boolean,
        ipAddressesCSV: String,
        expectsError: Boolean,
        expected: List<GroupEntry>,
        description: String,
    ) {
        if (expectsError) {
            try {
                assertThat(
                    description,
                    RestConverter.parseToGroup(
                        hasCSVHeader,
                        ipAddressesCSV,
                    ),
                    `is`(
                        expected
                    )
                )
                Assert.fail()
            } catch (_: IllegalArgumentException) {
            }
        } else {
            assertThat(
                description,
                RestConverter.parseToGroup(
                    hasCSVHeader,
                    ipAddressesCSV,
                ),
                `is`(
                    expected
                )
            )
        }
    }

    @Test
    fun testBookmarkConversion() {
        assertThat(
            TEST_BOOKMARK.toString(),
            `is`(TEST_BOOKMARK.toRest().toBusiness().toString())
        )
    }

    @Test
    fun testSearchQuery2ItemInformation() {
        val givenItem = Item(
            metadata = TEST_METADATA,
            rights = listOf(TEST_RIGHT)
        )
        val given = SearchQueryResult(
            numberOfResults = 2,
            results = listOf(
                givenItem,
            ),
            accessState = mapOf(
                AccessState.OPEN to 2,
            ),
            invalidSearchKey = listOf("foo"),
            hasLicenceContract = false,
            hasOpenContentLicence = true,
            hasSearchTokenWithNoKey = false,
            hasZbwUserAgreement = false,
            paketSigels = mapOf("sigel1" to 1),
            publicationType = mapOf(PublicationType.BOOK to 1, PublicationType.THESIS to 1),
            templateIds = mapOf(1 to ("name" to 2)),
            zdbIds = mapOf("zdb1" to 1),
        )
        val expected = ItemInformation(
            itemArray = listOf(givenItem.toRest()),
            totalPages = 2,
            accessStateWithCount = listOf(
                AccessStateWithCountRest(AccessState.OPEN.toRest(), 2)
            ),
            hasLicenceContract = given.hasLicenceContract,
            hasOpenContentLicence = given.hasOpenContentLicence,
            hasSearchTokenWithNoKey = given.hasSearchTokenWithNoKey,
            hasZbwUserAgreement = given.hasZbwUserAgreement,
            invalidSearchKey = given.invalidSearchKey,
            numberOfResults = given.numberOfResults,
            paketSigelWithCount = listOf(
                PaketSigelWithCountRest(count = 1, paketSigel = "sigel1")
            ),
            publicationTypeWithCount = listOf(
                PublicationTypeWithCountRest(
                    count = 1,
                    publicationType = PublicationType.BOOK.toRest(),
                ),
                PublicationTypeWithCountRest(
                    count = 1,
                    publicationType = PublicationType.THESIS.toRest(),
                ),
            ),
            zdbIdWithCount = listOf(
                ZdbIdWithCountRest(
                    count = 1,
                    zdbId = "zdb1",
                )
            ),
            templateIdWithCount = listOf(
                TemplateIdWithCountRest(
                    count = 2,
                    templateId = "1",
                    templateName = "name",
                )
            )
        )

        assertThat(
            given.toRest(1),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PARSE_TO_GROUP = "DATA_FOR_PARSE_TO_GROUP"
        val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_METADATA = ItemMetadata(
            metadataId = "that-test",
            author = "Colbjørnsen, Terje",
            band = "band",
            collectionHandle = "handleCol",
            collectionName = "Collectioname",
            communityHandle = "handleCom",
            communityName = "Communityname",
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
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
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
            paketSigel = "sigel",
            ppn = "ppn",
            publicationType = PublicationType.BOOK,
            publicationDate = LocalDate.of(2022, 9, 1),
            rightsK10plus = "some rights",
            storageDate = OffsetDateTime.now(),
            subCommunitiesHandles = listOf("handle1", "handle2"),
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zdbId = null,
        )

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
            endDate = TODAY,
            groupIds = null,
            lastAppliedOn = OffsetDateTime.of(
                2022,
                3,
                4,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
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
            startDate = TODAY.minusDays(1),
            licenceContract = "some contract",
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            templateDescription = "foo",
            templateId = null,
            templateName = "name",
            zbwUserAgreement = true,
        )

        val TEST_DA_ITEM = DAItem(
            id = 5,
            name = "name",
            handle = "handle",
            type = "type",
            link = "link",
            expand = listOf("foo"),
            lastModified = "2020-10-04",
            parentCollection = DACollection(
                id = 3,
                name = "Collectionname",
                handle = "colHandle",
                type = null,
                link = "link",
                expand = emptyList(),
                logo = null,
                parentCommunity = null,
                copyrightText = null,
                introductoryText = null,
                shortDescription = null,
                sidebarText = null,
                items = emptyList(),
                license = null,
                numberItems = 4,
                parentCommunityList = emptyList(),
            ),
            parentCollectionList = emptyList(),
            parentCommunityList = listOf(
                DACommunity(
                    id = 1,
                    name = "Communityname",
                    handle = "comHandle",
                    type = null,
                    countItems = null,
                    link = "link",
                    expand = emptyList(),
                    logo = null,
                    parentCommunity = null,
                    copyrightText = null,
                    introductoryText = null,
                    shortDescription = null,
                    sidebarText = null,
                    subcommunities = listOf(TEST_SUBCOMMUNITY),
                    collections = emptyList(),
                )
            ),
            metadata = listOf(
                DAMetadata(
                    key = "dc.identifier.uri",
                    value = "some_handle",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.type",
                    value = "article",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.contributor.author",
                    value = "Colbjørnsen, Terje",
                    language = null,
                ),
                DAMetadata(
                    key = "dc.date.issued",
                    value = "2022-09",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.date.accessioned",
                    value = "2022-01-19T07:57:26Z",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.title",
                    value = "some_title",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.journalname",
                    value = "some_journal",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.seriesname",
                    value = "some_series",
                    language = "DE",
                ),
            ),
            bitstreams = emptyList(),
            archived = "archived",
            withdrawn = "withdrawn",
        )

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
