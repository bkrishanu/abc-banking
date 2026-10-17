package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.model.CustomerId

/** The customer ID of the last successful login, so the lock screen only asks for the MPIN. */
interface RememberedCustomerStore {
    suspend fun rememberCustomer(customerId: CustomerId)

    suspend fun rememberedCustomer(): CustomerId?
}
