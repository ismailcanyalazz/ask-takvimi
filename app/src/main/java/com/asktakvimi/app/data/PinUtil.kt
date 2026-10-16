package com.asktakvimi.app.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PIN kodu asla düz metin olarak saklanmaz.
 * Rastgele bir tuz (salt) ile SHA-256 özeti alınır, yalnızca özet kaydedilir.
 */
object PinUtil {

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        // Aynı PIN'in farklı cihazlarda aynı özeti üretmemesi için tuz eklenir.
        digest.update(salt.toByteArray(Charsets.UTF_8))
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }

    fun verify(pin: String, settings: AppSettings): Boolean {
        val salt = settings.pinSalt ?: return false
        val expected = settings.pinHash ?: return false
        return hash(pin, salt) == expected
    }
}
