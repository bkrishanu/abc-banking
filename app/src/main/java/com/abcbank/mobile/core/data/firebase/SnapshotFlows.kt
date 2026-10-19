package com.abcbank.mobile.core.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Live Firestore reads as Flows (ARCHITECTURE.md section 04, State). Metadata changes are included
// so a screen hears when data moves between the offline cache and the server. Written here rather
// than adding a coroutines-play-services dependency, like TaskAwait.

internal fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

internal fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        when {
            error != null -> close(error)
            snapshot != null -> trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}
