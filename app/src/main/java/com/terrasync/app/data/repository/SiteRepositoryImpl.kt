package com.terrasync.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.terrasync.app.data.remote.FirestoreDataSource
import com.terrasync.app.domain.model.Site
import com.terrasync.app.domain.repository.SiteRepository
import javax.inject.Inject

/**
 * Concrete implementation of [SiteRepository].
 * Delegates raw I/O to [FirestoreDataSource] and wraps each call in [runCatching]
 * so no raw Firestore exceptions escape to the domain or presentation layers.
 */
class SiteRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreDataSource,
    private val auth: FirebaseAuth,
) : SiteRepository {

    override suspend fun createSite(site: Site): Result<String> = runCatching {
        dataSource.createSite(site)
    }

    override suspend fun findSiteByInviteCode(code: String): Result<Site> = runCatching {
        dataSource.findSiteByInviteCode(code)
            ?: error("No site found with invite code '$code'.")
    }

    override suspend fun joinSite(siteId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User not authenticated.")
        dataSource.joinSite(siteId, uid)
    }

    override suspend fun getSiteById(siteId: String): Result<Site> = runCatching {
        dataSource.fetchSiteById(siteId)
            ?: error("Site '$siteId' not found.")
    }

    override suspend fun getSites(): Result<List<Site>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User not authenticated.")
        dataSource.fetchSites(uid)
    }

    override suspend fun updateSiteName(siteId: String, newName: String): Result<Unit> = runCatching {
        dataSource.updateSiteName(siteId, newName)
    }

    override suspend fun deleteSite(siteId: String): Result<Unit> = runCatching {
        dataSource.deleteSite(siteId)
    }
}

