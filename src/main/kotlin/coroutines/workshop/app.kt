package coroutines.workshop

import Decrypter
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.CoroutineContext

// Class representing finished decryption.
// It is intended to being used to send data from worker coroutines.
data class Decryption(val decrypter: Decrypter, val input: String, val output: String)

// Coroutine context containing only thread pool of fixed size.
val executor: CoroutineContext by lazy {
    newFixedThreadPoolContext(nThreads = Decrypter.maxClientCount, name = "decrypters_executor")
}

fun main(args: Array<String>) = runBlocking {
    val api = Api(url = "http://localhost:9000", context = DefaultDispatcher) // remember to change `localhost` to proper url

    val token = api.register(Register("Władimir Iljicz Kotlin", "Kotlin")).await().token

    val reset = suspend {
        TODO("You may use this stub in step 3")
    }

    while (isActive) {
        try {
            with(Decrypter()) {
                val password = api.requestPassword(PasswordRequest(token)).await().encryptedPassword
                decrypt(decode(prepare(password)))
                        .also { api.validate(Validate(token, password, it)) }
            }
        } catch (e: Throwable) {
            println("$e\n")
        }
    }
}

suspend fun Api.passwords(token: String): ReceiveChannel<String> = TODO("You may use this stub in step 3")

suspend fun Decrypter.process(
        password: String,
        answerChannel: SendChannel<Decryption>
): Job = TODO("You may use this stub in step 3")