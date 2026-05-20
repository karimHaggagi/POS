package com.example.model


sealed class RequestState<out T> {
    data object Idle : RequestState<Nothing>()
    data object Loading : RequestState<Nothing>()
    data class Success<out T>(val data: T) : RequestState<T>()
    data class Error(val message: String) : RequestState<Nothing>()

    fun isIdle(): Boolean = this is Idle
    fun isLoading(): Boolean = this is Loading
    fun isError(): Boolean = this is Error
    fun isSuccess(): Boolean = this is Success

    fun getSuccessData() = (this as Success).data
    fun getSuccessDataOrNull() = if (this.isSuccess()) this.getSuccessData() else null
    fun getErrorMessage(): String = (this as Error).message
}

suspend fun <T> RequestState<T>.onSuccess(block: suspend (T) -> Unit): RequestState<T> {
    when (this) {
        is RequestState.Success -> block(this.data)
        else -> {}
    }
    return this
}

suspend fun <T> RequestState<T>.onFailure(block: suspend (String) -> Unit): RequestState<T> {
    when (this) {
        is RequestState.Error -> block(this.message)
        else -> {}
    }
    return this
}

fun <T, R> RequestState<T>.map(block: (T) -> R): RequestState<R> {
    return when(this){
        is RequestState.Success -> RequestState.Success(block(this.data))
        is RequestState.Error -> RequestState.Error(this.message)
        else -> RequestState.Idle
    }
}