package com.abcbank.mobile.core.data.firebase

import com.google.firebase.Timestamp

/**
 * A `notifications/{notificationId}` document (ARCHITECTURE.md section 04). Written by the seed,
 * transfers, applications and deposits; read by Home's bell and the notification center.
 */
data class NotificationDto(
    val kind: String? = null,
    val title: String? = null,
    val body: String? = null,
    val read: Boolean? = null,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        KIND to kind,
        TITLE to title,
        BODY to body,
        READ to read,
        CREATED_AT to createdAt
    )

    companion object {
        const val KIND = "kind"
        const val TITLE = "title"
        const val BODY = "body"
        const val READ = "read"
        const val CREATED_AT = "createdAt"

        const val TRANSACTIONAL = "TRANSACTIONAL"
        const val PROMOTIONAL = "PROMOTIONAL"

        /** A new, unread transactional alert. */
        fun transactional(title: String, body: String, createdAt: Timestamp) =
            NotificationDto(TRANSACTIONAL, title, body, read = false, createdAt = createdAt)

        fun fromMap(map: Map<String, Any?>): NotificationDto = NotificationDto(
            kind = map.stringField(KIND),
            title = map.stringField(TITLE),
            body = map.stringField(BODY),
            read = map.booleanField(READ),
            createdAt = map.timestampField(CREATED_AT)
        )
    }
}
