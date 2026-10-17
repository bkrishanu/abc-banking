package com.abcbank.mobile.core.model

/** The four demo personas (DEMO_DATA.md section 02). [key] is the personaId stored in Firestore. */
enum class PersonaId(val key: String) {
    SAMANTA("samanta"),
    MARCUS("marcus"),
    GRACE("grace"),
    DANIEL("daniel")
}
