package com.bullit.energysimulator

import arrow.core.*
import com.bullit.energysimulator.errorhandling.AbstractApplicationError
import com.bullit.energysimulator.errorhandling.ApplicationErrors
import com.bullit.energysimulator.errorhandling.NoResponseError
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

suspend inline fun <reified T> Mono<T>.toEither(crossinline errorFun: (throwable: Throwable) -> AbstractApplicationError): Either<ApplicationErrors, T> =
    map<Either<ApplicationErrors, T>> { it.right() }
        .onErrorResume { Mono.just(errorFun(it).leftNel()) }
        .awaitFirstOrElse {
            when (Unit) {
                is T -> Unit.right() as Either<ApplicationErrors, T>
                else -> NoResponseError().leftNel()
            }
        }

fun <T> List<Either<ApplicationErrors, T>>.invert(): Either<ApplicationErrors, List<T>> =
    fold(
        emptyList<T>().right() as Either<ApplicationErrors, List<T>>
    ) { acc, current ->
        when (current) {
            is Either.Left -> when (acc) {
                is Either.Left -> (acc.value + current.value).left()
                is Either.Right -> current
            }

            is Either.Right -> acc.flatMap { accumulatedList ->
                current.map {
                    accumulatedList.plus(it)
                }
            }
        }
    }

fun <T> List<Either<ApplicationErrors, List<T>>>.flatInvert(): Either<ApplicationErrors, List<T>> =
    fold(
        emptyList<T>().right() as Either<ApplicationErrors, List<T>>
    ) { acc, current ->
        when (current) {
            is Either.Left -> when (acc) {
                is Either.Left -> (acc.value + current.value).left()
                is Either.Right -> current
            }

            is Either.Right -> acc.flatMap { accumulatedList ->
                current.map {
                    accumulatedList.plus(it)
                }
            }
        }
    }

fun List<Either<ApplicationErrors, Unit>>.flatten() =
        fold(
            Unit.right() as Either<ApplicationErrors, Unit>
        ) { acc, current ->
            when (current) {
                is Either.Left -> when (acc) {
                    is Either.Left -> (acc.value + current.value).left()
                    is Either.Right -> current
                }
                is Either.Right -> acc
            }
        }