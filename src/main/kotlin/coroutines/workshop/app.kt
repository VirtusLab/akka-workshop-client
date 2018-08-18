package coroutines.workshop

import Decrypter
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

// Class representing finished decryption.
// It is intended to being used to send data from worker coroutines.
data class Decryption(val decrypter: Decrypter, val input: String, val output: String)

// Coroutine context containing only thread pool of fixed size.
val executor: CoroutineContext by lazy {
    newFixedThreadPoolContext(nThreads = Decrypter.maxClientCount, name = "decrypters_executor")
}

fun main(args: Array<String>) = runBlocking {
    val api = Api(url = "http://localhost:9000", context = DefaultDispatcher) // remember to change `localhost` to proper url

    val reset = suspend {
        TODO("You may use this stub in step 3")
    }

    TODO("Start in step 1 by connecting with server and registering yourself")

    while (isActive) {
        coroutineContext.cancel()
        // TODO("You may use this stub in step 2")
    }
}

suspend fun Api.passwords(token: String): ReceiveChannel<String> = TODO("You may use this stub in step 3")

suspend fun Decrypter.process(
        password: String,
        answerChannel: SendChannel<Decryption>
): Job = TODO("You may use this stub in step 3")