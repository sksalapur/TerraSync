package com.terrasync.app.domain.repository

import com.terrasync.app.domain.model.Site

/**
 * Domain-layer contract for geotechnical site data operations.
 * Implementations live in the data layer — no Firebase types leak through.
 */
interface SiteRepository {

    /**
     * Persist a new [Site] document to Firestore.
     * @return [Result.success] with the generated Firestore document ID.
     */
    suspend fun createSite(site: Site): Result<String>

    /**
     * Query Firestore for a site whose [Site.inviteCode] matches [code].
     * @return [Result.success] with the [Site] if found, [Result.failure] if not found or error.
     */
    suspend fun findSiteByInviteCode(code: String): Result<Site>

    /**
     * Join an existing site.
     * @return [Result.success] if the user successfully joined the site.
     */
    suspend fun joinSite(siteId: String): Result<Unit>

    /**
     * Fetch a single site by its Firestore document ID.
     */
    suspend fun getSiteById(siteId: String): Result<Site>

    /**
     * Fetch all sites belonging to the current user (used by SiteDashboard list).
     */
    suspend fun getSites(): Result<List<Site>>

    /** Update the name of a site document. */
    suspend fun updateSiteName(siteId: String, newName: String): Result<Unit>

    /** Delete a site document. */
    suspend fun deleteSite(siteId: String): Result<Unit>
}

