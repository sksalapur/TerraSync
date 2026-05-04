package com.terrasync.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.terrasync.app.domain.model.Site
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_SITES   = "sites"
private const val FIELD_INVITE_CODE  = "inviteCode"
private const val FIELD_MEMBER_IDS   = "memberIds"

/**
 * Raw Firestore access layer. Handles network I/O only — no business logic.
 * Converts Firebase Task callbacks → suspend functions via [kotlinx.coroutines.tasks.await].
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val sitesCollection get() = firestore.collection(COLLECTION_SITES)

    /** Write a new site document and return its generated Firestore ID. */
    suspend fun createSite(site: Site): String {
        val ref = sitesCollection.add(site).await()
        return ref.id
    }

    /**
     * Query sites collection for a document whose [FIELD_INVITE_CODE] matches [code].
     * Returns null if no match found.
     */
    suspend fun findSiteByInviteCode(code: String): Site? {
        val snapshot = sitesCollection
            .whereEqualTo(FIELD_INVITE_CODE, code.uppercase())
            .limit(1)
            .get()
            .await()
        return snapshot.documents
            .firstOrNull()
            ?.toObject(Site::class.java)
            ?.copy(id = snapshot.documents.first().id)
    }

    /** Fetch a single site document by Firestore document ID. */
    suspend fun fetchSiteById(siteId: String): Site? {
        val doc = sitesCollection.document(siteId).get().await()
        return doc.toObject(Site::class.java)?.copy(id = doc.id)
    }

    /** Adds the [userId] to the site's [FIELD_MEMBER_IDS] array. */
    suspend fun joinSite(siteId: String, userId: String) {
        sitesCollection.document(siteId)
            .update(FIELD_MEMBER_IDS, com.google.firebase.firestore.FieldValue.arrayUnion(userId))
            .await()
    }

    /** Fetch all sites where [FIELD_MEMBER_IDS] contains [userId]. */
    suspend fun fetchSites(userId: String): List<Site> {
        val snapshot = sitesCollection
            .whereArrayContains(FIELD_MEMBER_IDS, userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Site::class.java)?.copy(id = doc.id)
        }
    }

    /** Update the name of a site document. */
    suspend fun updateSiteName(siteId: String, newName: String) {
        sitesCollection.document(siteId).update("name", newName).await()
    }

    /** Delete a site document. */
    suspend fun deleteSite(siteId: String) {
        sitesCollection.document(siteId).delete().await()
    }
}

