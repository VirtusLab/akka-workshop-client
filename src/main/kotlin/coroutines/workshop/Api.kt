package coroutines.workshop

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlin.coroutines.experimental.CoroutineContext

data class Register(val name: String)
data class Registered(val token: String)

data class PasswordRequest(val token: String)
data class EncryptedPassword(val encryptedPassword: String)

data class Validate(val token: String, val encryptedPassword: String, val decryptedPassword: String)

interface Api {
    @POST("/register")
    fun register(@Body request: Register): Deferred<Registered>

    @POST("/send-encrypted-password")
    fun requestPassword(@Body request: PasswordRequest): Deferred<EncryptedPassword>

    @POST("/validate")
    fun validate(@Body request: Validate): Deferred<Any>

    companion object {
        operator fun invoke(url: String, context: CoroutineContext = Unconfined): Api =
                Retrofit.Builder()
                        .addConverterFactory(MoshiConverterFactory.create())
                        .addCallAdapterFactory(DeferredAdapterFactory(context))
                        .baseUrl(url)
                        .build()
                        .create(Api::class.java)
    }
}