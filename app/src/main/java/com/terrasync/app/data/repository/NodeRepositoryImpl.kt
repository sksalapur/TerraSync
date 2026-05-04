package com.terrasync.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.terrasync.app.domain.model.SoilNode
import com.terrasync.app.domain.repository.NodeRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val COLLECTION_SITES = "sites"
private const val COLLECTION_NODES = "nodes"
private const val FIELD_RECORDED_AT = "recordedAt"

/**
 * Concrete [NodeRepository] backed by Firestore.
 *
 * Firestore path: sites/{siteId}/nodes/{nodeId}
 *
 * [observeNodes] uses [callbackFlow] to bridge the Firestore
 * [addSnapshotListener] callback API to a Kotlin [Flow].
 * The listener is automatically removed when the collecting coroutine is cancelled
 * via [awaitClose], preventing memory leaks.
 */
@Singleton
class NodeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : NodeRepository {

    private fun nodesCollection(siteId: String) =
        firestore.collection(COLLECTION_SITES)
                 .document(siteId)
                 .collection(COLLECTION_NODES)

    override suspend fun saveNode(node: SoilNode): Result<String> = runCatching {
        val ref = nodesCollection(node.siteId).add(node).await()
        ref.id
    }

    override fun observeNodes(siteId: String): Flow<List<SoilNode>> = callbackFlow {
        val query = nodesCollection(siteId)
            .orderBy(FIELD_RECORDED_AT, Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Close the flow with the Firestore error — the ViewModel catches this
                close(error)
                return@addSnapshotListener
            }
            val nodes = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(SoilNode::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(nodes)
        }

        // Remove the listener when the Flow collector cancels (e.g. ViewModel cleared)
        awaitClose { listener.remove() }
    }

    override suspend fun deleteNode(siteId: String, nodeId: String): Result<Unit> = runCatching {
        nodesCollection(siteId).document(nodeId).delete().await()
    }

    override suspend fun deleteNodes(siteId: String, nodeIds: List<String>): Result<Unit> = runCatching {
        val batch = firestore.batch()
        nodeIds.forEach { id ->
            batch.delete(nodesCollection(siteId).document(id))
        }
        batch.commit().await()
    }

    override suspend fun getNodeById(siteId: String, nodeId: String): Result<SoilNode> = runCatching {
        val doc = nodesCollection(siteId).document(nodeId).get().await()
        doc.toObject(SoilNode::class.java)?.copy(id = doc.id)
            ?: error("Node '$nodeId' not found.")
    }

    override suspend fun updateNode(node: SoilNode): Result<Unit> = runCatching {
        nodesCollection(node.siteId).document(node.id).set(node).await()
    }
}
