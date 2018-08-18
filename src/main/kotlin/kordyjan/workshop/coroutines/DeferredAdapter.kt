package kordyjan.workshop.coroutines

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.coroutines.experimental.CoroutineContext

class DeferredAdapter<R>(private val returnType: Type, private val context: CoroutineContext) : CallAdapter<R, Deferred<R?>> {
    override fun adapt(call: Call<R>): Deferred<R?> = async(context) {
        call.execute().body()
    }

    override fun responseType() = returnType
}

class DeferredAdapterFactory(private val context: CoroutineContext) : CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? =
            if (getRawType(returnType) == Deferred::class.java)
                DeferredAdapter<Any>(getParameterUpperBound(0, returnType as ParameterizedType), context)
            else
                null
}