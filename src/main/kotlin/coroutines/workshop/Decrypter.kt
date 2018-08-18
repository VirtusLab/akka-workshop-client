import java.util.*

sealed class DecryptionState

data class PasswordPrepared(val password: String) : DecryptionState()

data class PasswordDecoded(val password: String) : DecryptionState()

class Decrypter {
    companion object {
        val maxClientCount = 4

        private val random = Random()

        private var clientsCount = 0

        private var clients = setOf<Int>()

        private var currentId = 0

        private fun randomAlphanumericString(n: Int): String {
            val chars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9') // "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return generateSequence { random.nextInt(chars.size) }
                    .map { chars[it] }
                    .take(n)
                    .joinToString(separator = "")
        }

        private fun getNewId(): Int = synchronized(this) {
            currentId.also {
                clients += it
                currentId = it + 1
            }
        }

        private fun isClientAccepted(): Boolean = synchronized(this) {
            if (clientsCount < maxClientCount)
                true.also { clientsCount += 1 }
            else
                false
        }

        private val message =
                """Internal state of decryptor get corrupted.
              |This is not your fault - this is intended "problem" :)
              |Beware that all current decryptor instances also get corrupted and will produce bad results.
              |In order to correctly use decryptor library please create new instances.
            """.trimMargin()

        private fun decrypt(id: Int, password: String, probabilityOfFailure: Double = 0.05): String =
                try {
                    Thread.sleep(1000)

                    while (!isClientAccepted()) {
                        Thread.sleep(100)
                    }

                    synchronized(this) {
                        val r = random.nextDouble()
                        if (r < probabilityOfFailure) {
                            clients = setOf()
                            throw IllegalStateException(message)
                        }
                        if (id in clients)
                            Base64.getDecoder().decode(password).let { String(it) }
                        else
                            randomAlphanumericString(20)
                    }
                } finally {
                    synchronized(this) {
                        clientsCount -= 1
                    }
                }
    }

    val id = getNewId()

    fun prepare(password: String): PasswordPrepared =
            PasswordPrepared(decrypt(id, password))

    fun decode(state: PasswordPrepared): PasswordDecoded = PasswordDecoded(decrypt(id, state.password))

    fun decrypt(state: PasswordDecoded): String = decrypt(id, state.password)
}