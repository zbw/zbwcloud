package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRule
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemporalValidity
import de.zbw.business.lori.server.TemporalValidityFilter
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Helper object for parsing all sort of query parameters.
 *
 * Created on 09-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object QueryParameterParser {
    fun parsePublicationDateFilter(s: String?): PublicationDateFilter? {
        if (s == null) {
            return null
        }
        val noToYear = s.matches("\\d+-".toRegex())
        val noFromYear = s.matches("-\\d+".toRegex())
        val both = s.matches("\\d+-\\d+".toRegex())
        return if (noFromYear || noToYear || both) {
            PublicationDateFilter(
                noFromYear.takeIf { !it }
                    ?.let { s.substringBefore("-").toInt() }
                    ?: PublicationDateFilter.MIN_YEAR,
                noToYear.takeIf { !it }
                    ?.let { s.substringAfter("-").toInt() }
                    ?: PublicationDateFilter.MAX_YEAR
            )
        } else {
            null
        }
    }

    fun parsePublicationTypeFilter(s: String?): PublicationTypeFilter? {
        if (s == null) {
            return null
        }
        val receivedPubTypes: List<PublicationType> = s.split(",".toRegex()).mapNotNull {
            try {
                PublicationType.valueOf(it)
            } catch (iae: IllegalArgumentException) {
                null
            }
        }
        return receivedPubTypes.takeIf { it.isNotEmpty() }?.let { PublicationTypeFilter(it) }
    }

    fun parseAccessStateFilter(s: String?): AccessStateFilter? {
        if (s == null) {
            return null
        }
        val accessStates: List<AccessState> = s.split(",".toRegex()).mapNotNull {
            try {
                AccessState.valueOf(it)
            } catch (iae: IllegalArgumentException) {
                null
            }
        }
        return accessStates.takeIf { it.isNotEmpty() }?.let { AccessStateFilter(it) }
    }

    fun parseTemporalValidity(s: String?): TemporalValidityFilter? {
        if (s == null) {
            return null
        }
        val temporalValidity: List<TemporalValidity> = s.split(",".toRegex()).mapNotNull {
            try {
                TemporalValidity.valueOf(it)
            } catch (iae: IllegalArgumentException) {
                null
            }
        }
        return temporalValidity.takeIf { it.isNotEmpty() }?.let { TemporalValidityFilter(it) }
    }

    fun parseStartDateFilter(s: String?): StartDateFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { StartDateFilter(it) }

    fun parseEndDateFilter(s: String?): EndDateFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { EndDateFilter(it) }

    private fun parseDate(s: String): LocalDate? {
        return try {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (dte: DateTimeException) {
            null
        }
    }

    fun parseFormalRuleFilter(s: String?): FormalRuleFilter? =
        s?.let { input ->
            val formalRules: List<FormalRule> = input.split(",".toRegex()).mapNotNull {
                try {
                    FormalRule.valueOf(it.uppercase())
                } catch (iae: IllegalArgumentException) {
                    null
                }
            }
            return formalRules.takeIf { it.isNotEmpty() }?.let { FormalRuleFilter(it) }
        }
}