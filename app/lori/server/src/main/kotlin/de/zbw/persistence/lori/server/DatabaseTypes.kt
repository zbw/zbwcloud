package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class FacetTransient(
    val accessState: AccessState?,
    val licenceContract: String?,
    val nonStandardsOCL: Boolean,
    val nonStandardsOCLUrl: String?,
    val paketSigel: String?,
    val publicationType: PublicationType,
    val ocl: String?,
    val oclRestricted: Boolean,
    val zdbId: String?,
    val zbwUserAgreement: Boolean,
)

data class FacetTransientSet(
    val accessState: Set<AccessState>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasZbwUserAgreement: Boolean,
    val paketSigels: Set<String>,
    val publicationType: Set<PublicationType>,
    val zdbIds: Set<String>,
)
