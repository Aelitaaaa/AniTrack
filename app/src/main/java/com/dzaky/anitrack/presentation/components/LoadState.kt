package com.dzaky.anitrack.presentation.components

import com.dzaky.anitrack.data.CatalogException

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

fun Throwable.catalogMessage(): String = (this as? CatalogException)?.explanation
    ?: "Something went wrong while loading anime. Please try again."

const val StorageError = "Couldn't save or read your library. Please try again."
