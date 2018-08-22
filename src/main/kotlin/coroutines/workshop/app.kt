package coroutines.workshop

import Decrypter
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext

// Class representing finished decryption.
// It is intended to being used to send data from worker coroutines.
data class Decryption(val decrypter: Decrypter, val input: String, val output: String, val parent: Job)

// Coroutine context containing only thread pool of fixed size.
val executor: CoroutineContext by lazy {
    newFixedThreadPoolContext(nThreads = Decrypter.maxClientCount, name = "decrypters_executor")
}

fun main(args: Array<String>) = runBlocking {
    val api = Api(url = "http://localhost:9000", context = DefaultDispatcher) // remember to change `localhost` to proper url

    val token = api.register(Register("Władimir Iljicz Kotlin", "Kotlin")).await().token

    val newPasswords = api.passwords(token)

    var allDecryptions: Job? = null

    lateinit var finishedDecryptions: Channel<Decryption>

    val reset = suspend {
        allDecryptions?.cancel()
        finishedDecryptions = Channel(Decrypter.maxClientCount)
        allDecryptions = Job().also { parent ->
            repeat(Decrypter.maxClientCount) {
                Decrypter().process(newPasswords.receive(), parent, finishedDecryptions)
            }
        }
    }

    reset()

    while (isActive) {
        for ((decrypter, input, output, parent) in finishedDecryptions) {
            decrypter.process(newPasswords.receive(), parent, finishedDecryptions)
            api.validate(Validate(token, input, output))
        }
        reset()
    }
}

suspend fun Api.passwords(token: String): ReceiveChannel<String> = produce {
    while (isActive) {
        requestPassword(PasswordRequest(token))
                .await()
                .encryptedPassword
                .also { send(it) }
    }
}

suspend fun Decrypter.process(
        password: String,
        parent: Job,
        answerChannel: SendChannel<Decryption>
) = launch(executor, parent = parent) {
    try {
        val output = decrypt(decode(prepare(password)))
        answerChannel.send(Decryption(this@process, password, output, parent))
    } catch (e: Throwable) {
        println("$e\n")
        if (!answerChannel.isClosedForSend) answerChannel.close()
    }
}