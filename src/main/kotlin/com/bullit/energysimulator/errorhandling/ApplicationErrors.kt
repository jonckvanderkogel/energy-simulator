package com.bullit.energysimulator.errorhandling

import arrow.core.NonEmptyList

typealias ApplicationErrors = NonEmptyList<ApplicationError>

sealed interface ApplicationError {
    val message: String
    val throwable: Throwable?
}

abstract class AbstractApplicationError(
    override val message: String,
    override val throwable: Throwable? = null
) : ApplicationError {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractApplicationError

        if (message != other.message) return false
        return throwable == other.throwable
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + (throwable?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "${this::class.simpleName}(message='$message', throwable=$throwable)"
    }
}

fun <T : ApplicationError> NonEmptyList<T>.joinMessages(separator: CharSequence = ", "): String =
    this.map { it }.joinToString(separator = separator)

class DatabaseInteractionError(t: Throwable) : AbstractApplicationError(
    message = t.message ?: "Exception while interacting with Postgres",
    throwable = t
)

class ElasticsearchInteractionError(t: Throwable) : AbstractApplicationError(
    message = t.message ?: "Exception while interacting with Elasticsearch",
    throwable = t
)

class NoResponseError() : AbstractApplicationError(
    message = "No response for request"
)

