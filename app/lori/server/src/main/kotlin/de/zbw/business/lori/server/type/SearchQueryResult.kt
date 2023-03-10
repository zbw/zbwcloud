package de.zbw.business.lori.server.type

/**
 * Helper type that encapsulates the results of a search query.
 *
 * Created on 10-28-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class SearchQueryResult(
    val numberOfResults: Int,
    val results: List<Item>,
    val accessState: Set<AccessState>,
    val invalidSearchKey: List<String>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasSearchTokenWithNoKey: Boolean,
    val hasZbwUserAgreement: Boolean,
    val paketSigels: Set<String>,
    val publicationType: Set<PublicationType>,
    val zdbIds: Set<String>,
)
