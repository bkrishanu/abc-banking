package com.abcbank.mobile.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `DeviceKeyManager` from ARCHITECTURE.md section 04: an EC P-256 signing key generated inside
 * Android Keystore on first use. The private key never leaves the Keystore; it is not
 * exportable and is removed with the app.
 */
@Singleton
class AndroidDeviceKeyStore @Inject constructor() : DeviceKeyStore {
    @Synchronized
    override fun publicKey(): ByteArray? = try {
        existingOrNewKey()
    } catch (error: GeneralSecurityException) {
        null
    } catch (error: ProviderException) {
        null
    } catch (error: IOException) {
        null
    }

    private fun existingOrNewKey(): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getCertificate(ALIAS)?.let { return it.publicKey.encoded }
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generator.generateKeyPair().public.encoded
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "abc_device_binding"
        const val CURVE = "secp256r1"
    }
}
