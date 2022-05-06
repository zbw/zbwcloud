package de.zbw.api.lori.server.route

import com.google.gson.Gson
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.RightRest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RightRoutesKtTest {

    @Test
    fun testGetMetadataOK() {
        // given
        val rightId = "someId"
        val expected = TEST_RIGHT
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getRightsByIds(listOf(rightId)) } returns listOf(expected.toBusiness())
        }
        val servicePool = getServicePool(backend)
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/right/$rightId")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<RightRest>() {}.type
                val received: RightRest = GSON.fromJson(content, groupListType)
                assertThat(received, `is`(expected))
            }
        }
    }

    @Test
    fun testGetMetadataNotFound() {
        // given
        val testId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getRightsByIds(listOf(testId)) } returns emptyList()
        }
        val servicePool = getServicePool(backend)
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/right/$testId")) {
                assertThat(
                    "Should return NotFound",
                    response.status(),
                    `is`(HttpStatusCode.NotFound)
                )
            }
        }
    }

    @Test
    fun testGetMetadataInternal() {
        // given
        val rightId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getRightsByIds(listOf(rightId)) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/right/$rightId")) {
                assertThat(
                    "Should return InternalServerError",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    @Test
    fun testDeleteRightOK() {

        // given
        val rightId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsRight(rightId) } returns false
            every { deleteRight(rightId) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/right/$rightId")
            ) {
                assertThat("Should return OK", response.status(), `is`(HttpStatusCode.OK))
                verify(exactly = 1) { backend.itemContainsRight(rightId) }
                verify(exactly = 1) { backend.deleteRight(rightId) }
            }
        }
    }

    @Test
    fun testDeleteRightConflict() {

        // given
        val rightId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsRight(rightId) } returns true
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/right/$rightId")
            ) {
                assertThat("Should return Conflict", response.status(), `is`(HttpStatusCode.Conflict))
                verify(exactly = 1) { backend.itemContainsRight(rightId) }
            }
        }
    }

    @Test
    fun testDeleteRightException() {

        // given
        val rightId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsRight(rightId) } returns false
            every { deleteRight(rightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/right/$rightId")
            ) {
                assertThat(
                    "Should return 500 because of internal SQL exception",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    @Test
    fun testPostRightOK() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } returns false
            every { insertRight(any()) } returns "foo"
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat("Should return CREATED", response.status(), `is`(HttpStatusCode.Created))
            }
        }
    }

    @Test
    fun testPostRightConflict() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } returns true
        }
        val servicePool = getServicePool(backend)
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat("Should return Conflict", response.status(), `is`(HttpStatusCode.Conflict))
            }
        }
    }

    @Test
    fun testPostRightInternalError() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat(
                    "Should return 500 because of internal SQL exception",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError),
                )
            }
        }
    }

    @Test
    fun testPostRightBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
                }
            ) {
                assertThat(
                    "Should return 400 because of wrong JSON data.",
                    response.status(),
                    `is`(HttpStatusCode.BadRequest),
                )
            }
        }
    }

    @Test
    fun testPutRightNoContent() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } returns true
            every { upsertRight(any()) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat("Should return NO_CONTENT", response.status(), `is`(HttpStatusCode.NoContent))
            }
        }
    }

    @Test
    fun testPutRightCreated() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } returns false
            every { insertRight(any()) } returns "1"
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat("Should return CREATED", response.status(), `is`(HttpStatusCode.Created))
            }
        }
    }

    @Test
    fun testPutRightBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } returns true
            every { upsertRight(any()) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
                }
            ) {
                assertThat("Should return BAD_REQUEST", response.status(), `is`(HttpStatusCode.BadRequest))
            }
        }
    }

    @Test
    fun testPutRightInternalError() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { rightContainsId(TEST_RIGHT.rightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/right") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_RIGHT))
                }
            ) {
                assertThat(
                    "Should return Internal Server Error",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)

        val CONFIG = LoriConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
            digitalArchiveAddress = "https://archiveaddress",
            digitalArchiveCommunity = "5678",
            digitalArchiveUsername = "testuser",
            digitalArchivePassword = "password",
            digitalArchiveBasicAuth = "basicauth",
        )

        val TEST_RIGHT = RightRest(
            rightId = "rightId",
            accessState = RightRest.AccessState.open,
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
            licenseConditions = "license",
            provenanceLicense = "provenance",
            startDate = TODAY.minusDays(1),
        )

        val GSON: Gson = Gson().newBuilder()
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonDeserializer { json, _, _ ->
                    LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
                }
            )
            .registerTypeAdapter(
                OffsetDateTime::class.java,
                JsonDeserializer { json, _, _ ->
                    ZonedDateTime.parse(json.asString, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                        .toOffsetDateTime()
                }
            )
            .registerTypeAdapter(
                OffsetDateTime::class.java,
                JsonSerializer<OffsetDateTime> { obj, _, _ ->
                    JsonPrimitive(obj.toString())
                }
            )
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonSerializer<LocalDate> { obj, _, _ ->
                    JsonPrimitive(obj.toString())
                }
            )
            .create()

        fun jsonAsString(any: Any): String = GSON.toJson(any)
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")

        fun getServicePool(backend: LoriServerBackend) = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend,
            tracer = tracer,
        )
    }
}