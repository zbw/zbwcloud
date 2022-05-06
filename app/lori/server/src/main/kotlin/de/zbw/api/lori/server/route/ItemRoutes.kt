package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.ItemCountByRight
import de.zbw.lori.model.ItemEntry
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for items.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.itemRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/item") {
        post {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/item")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val item: ItemEntry =
                        call.receive(ItemEntry::class)
                            .takeIf { it.metadataId != null && it.rightId != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("item", item.toString())
                    if (backend.itemContainsEntry(item.metadataId, item.rightId)) {
                        span.setStatus(StatusCode.ERROR, "Conflict: Resource with this primary key already exists.")
                        call.respond(HttpStatusCode.Conflict, "Resource with this relation already exists.")
                    } else {
                        backend.insertItemEntry(item.metadataId, item.rightId)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError)
                } finally {
                    span.end()
                }
            }
        }

        route("/metadata") {
            delete("{metadataId}") {
                val span = tracer
                    .spanBuilder("lori.LoriService.DELETE/api/v1/item/metadata/{metadataId}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val metadataId = call.parameters["metadataId"]
                        span.setAttribute("metadataId", metadataId ?: "null")
                        if (metadataId == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the url."
                            )
                            call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                        } else {
                            backend.deleteItemEntriesByMetadataId(metadataId)
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    } finally {
                        span.end()
                    }
                }
            }
        }

        route("/right") {
            delete("{rightId}") {
                val span = tracer
                    .spanBuilder("lori.LoriService.DELETE/api/v1/item/right/{rightId}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["rightId"]
                        span.setAttribute("rightId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the url."
                            )
                            call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                        } else {
                            backend.deleteItemEntriesByRightId(rightId)
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    } finally {
                        span.end()
                    }
                }
            }
        }

        route("/count/right") {
            get("{rightId}") {
                val span = tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/item/count/right/{rightId}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["rightId"]
                        span.setAttribute("rightId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the url."
                            )
                            call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                        } else {
                            val count = backend.countItemByRightId(rightId)
                            span.setStatus(StatusCode.OK)
                            call.respond(
                                ItemCountByRight(
                                    rightId = rightId,
                                    count = count,
                                )
                            )
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    } finally {
                        span.end()
                    }
                }
            }
        }

        delete("{metadataId}/{rightId}") {
            val span = tracer
                .spanBuilder("lori.LoriService.DELETE/api/v1/item/{metadataId}/{rightId}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val metadataId = call.parameters["metadataId"]
                    val rightId = call.parameters["rightId"]
                    span.setAttribute("metadataId", metadataId ?: "null")
                    span.setAttribute("rightId", rightId ?: "null")
                    if (metadataId == null || rightId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        backend.deleteItemEntry(metadataId, rightId)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.OK)
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        route("/list") {
            get {
                val span = tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/item/list")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                        if (limit < 1 || limit > 100) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Limit parameter is expected to be between (0,100]"
                            )
                            call.respond(HttpStatusCode.BadRequest, "Limit parameter is expected to be between (0,100]")
                        } else if (offset < 0) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Offset parameter is expected to be larger or equal zero"
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Offset parameter is expected to be larger or equal zero"
                            )
                        } else {
                            val items = backend.getItemList(limit, offset)
                            span.setStatus(StatusCode.OK)
                            call.respond(items.map { it.toRest() })
                        }
                    } catch (e: NumberFormatException) {
                        span.setStatus(StatusCode.ERROR, "NumberFormatException: ${e.message}")
                        call.respond(HttpStatusCode.BadRequest, "Parameters have a bad format")
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
}