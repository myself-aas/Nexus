package com.example.security

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object MfaHelper {
    private const val CHAR_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun generateSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(10)
        random.nextBytes(bytes)
        return bytes.toBase32()
    }

    private fun ByteArray.toBase32(): String {
        var i = 0
        var index = 0
        var digit: Int
        var currByte: Byte
        var nextByte: Byte
        val base32 = StringBuilder((this.size + 7) * 8 / 5)

        while (i < this.size) {
            currByte = this[i]
            if (index > 3) {
                nextByte = if (i + 1 < this.size) this[i + 1] else 0
                digit = currByte.toInt() and (0xFF ushr index)
                index = (index + 5) % 8
                digit = digit shl index
                digit = digit or ((nextByte.toInt() and 0xFF) ushr (8 - index))
                i++
            } else {
                digit = (currByte.toInt() ushr (8 - (index + 5))) and 0x1F
                index = (index + 5) % 8
                if (index == 0) {
                    i++
                }
            }
            base32.append(CHAR_MAP[digit])
        }
        return base32.toString()
    }

    private fun String.fromBase32(): ByteArray {
        val cleaned = this.uppercase().replace("[^A-Z2-7]".toRegex(), "")
        val result = ByteArray(cleaned.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        for (element in cleaned) {
            val valIndex = CHAR_MAP.indexOf(element)
            if (valIndex < 0) continue
            buffer = (buffer shl 5) or valIndex
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result[count++] = (buffer shr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return result
    }

    fun getOtp(secret: String, timeIndex: Long): String {
        return try {
            val key = secret.fromBase32()
            val data = ByteArray(8)
            var value = timeIndex
            var i = 8
            while (i-- > 0) {
                data[i] = (value and 0xFF).toByte()
                value = value ushr 8
            }
            val signKey = SecretKeySpec(key, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)
            val offset = (hash[hash.size - 1] and 0xF).toInt()
            var truncatedHash: Long = 0
            for (j in 0..3) {
                truncatedHash = truncatedHash shl 8
                truncatedHash = truncatedHash or (hash[offset + j].toInt() and 0xFF).toLong()
            }
            truncatedHash = truncatedHash and 0x7FFFFFFF
            truncatedHash %= 1000000
            String.format("%06d", truncatedHash)
        } catch (e: Exception) {
            "000000"
        }
    }

    fun verifyCode(secret: String, code: String): Boolean {
        if (code.length != 6) return false
        val currentTime = System.currentTimeMillis() / 1000 / 30
        for (i in -1..1) {
            if (getOtp(secret, currentTime + i) == code) {
                return true
            }
        }
        return false
    }

    fun getQrCodeUri(email: String, secret: String): String {
        return "otpauth://totp/ClaudeAI:$email?secret=$secret&issuer=ClaudeAI"
    }
}
