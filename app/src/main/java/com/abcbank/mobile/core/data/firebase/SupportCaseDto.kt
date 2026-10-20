package com.abcbank.mobile.core.data.firebase

import com.google.firebase.Timestamp

/**
 * A `supportCases/{caseId}` document (ARCHITECTURE.md section 04): written by disputes and the
 * support bot's hand-off, read by the support hub. [messages] are maps of from, body and sentAt.
 */
data class SupportCaseDto(
    val transactionId: String? = null,
    val topic: String? = null,
    val status: String? = null,
    val createdAt: Timestamp? = null,
    val messages: List<Message>? = null
) {
    data class Message(val from: String?, val body: String?, val sentAt: Timestamp?)

    fun toMap(): Map<String, Any> = firestoreFields(
        TRANSACTION_ID to transactionId,
        TOPIC to topic,
        STATUS to status,
        CREATED_AT to createdAt,
        MESSAGES to messages?.map { message ->
            firestoreFields(FROM to message.from, BODY to message.body, SENT_AT to message.sentAt)
        }
    )

    companion object {
        const val TRANSACTION_ID = "transactionId"
        const val TOPIC = "topic"
        const val STATUS = "status"
        const val CREATED_AT = "createdAt"
        const val MESSAGES = "messages"
        const val FROM = "from"
        const val BODY = "body"
        const val SENT_AT = "sentAt"

        const val OPEN = "OPEN"
        const val IN_REVIEW = "IN_REVIEW"
        const val RESOLVED = "RESOLVED"
        const val CUSTOMER = "CUSTOMER"
        const val BANK = "BANK"

        fun fromMap(map: Map<String, Any?>): SupportCaseDto = SupportCaseDto(
            transactionId = map.stringField(TRANSACTION_ID),
            topic = map.stringField(TOPIC),
            status = map.stringField(STATUS),
            createdAt = map.timestampField(CREATED_AT),
            messages = (map[MESSAGES] as? List<*>)?.map { item ->
                val fields = (item as? Map<*, *>)?.entries?.associate { (key, value) ->
                    key.toString() to
                        value
                }.orEmpty()
                Message(
                    fields.stringField(FROM),
                    fields.stringField(BODY),
                    fields.timestampField(SENT_AT)
                )
            }
        )
    }
}
