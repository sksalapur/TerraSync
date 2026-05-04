package com.terrasync.app.domain.repository

import com.terrasync.app.domain.model.SoilNode
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for real-time soil node operations.
 *
 * [observeNodes] returns a [Flow] backed by a Firestore snapshot listener
 * so the dashboard UI updates the moment any node is written.
 */
interface NodeRepository {

    /**
     * Writes a [SoilNode] document to sites/{siteId}/nodes/ and returns
     * the generated Firestore document ID.
     */
    suspend fun saveNode(node: SoilNode): Result<String>

    /**
     * Real-time stream of all nodes under a site, ordered by [SoilNode.recordedAt] descending.
     * Emits the current list immediately on subscription, then on every Firestore update.
     */
    fun observeNodes(siteId: String): Flow<List<SoilNode>>

    /** Deletes a single node by its Firestore document ID. */
    suspend fun deleteNode(siteId: String, nodeId: String): Result<Unit>

    /** Deletes multiple nodes by their Firestore document IDs. */
    suspend fun deleteNodes(siteId: String, nodeIds: List<String>): Result<Unit>

    /** Fetches a single node by ID for editing. */
    suspend fun getNodeById(siteId: String, nodeId: String): Result<SoilNode>

    /** Overwrites an existing node document. */
    suspend fun updateNode(node: SoilNode): Result<Unit>
}
