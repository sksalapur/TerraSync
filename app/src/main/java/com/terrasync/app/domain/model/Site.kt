package com.terrasync.app.domain.model

/**
 * Pure Kotlin domain model for a geotechnical site.
 * No Android/Firebase imports — fully testable on plain JVM.
 *
 * @param id          Firestore document ID (empty until persisted).
 * @param name        Human-readable site name (e.g. "NTPC Sipat Bore-Log Area").
 * @param location    Free-text GPS or address descriptor.
 * @param createdAt   Unix epoch timestamp (ms) of site creation.
 * @param ownerId     UID of the Firebase Auth user who created this site.
 * @param adminId     UID of the current admin (defaults to creator).
 * @param inviteCode  Unique 6-character alphanumeric code for joining the site.
 * @param memberIds   List of UIDs of users who have joined this site.
 */
data class Site(
    val id: String         = "",
    val name: String       = "",
    val location: String   = "",
    val createdAt: Long    = 0L,
    val ownerId: String    = "",
    val adminId: String    = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
)

