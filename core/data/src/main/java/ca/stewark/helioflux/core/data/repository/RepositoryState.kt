package ca.stewark.helioflux.core.data.repository

import ca.stewark.helioflux.core.model.DataFreshness
import ca.stewark.helioflux.core.model.DataSourceKey

sealed interface RepositoryState<out T> {
    data object Loading : RepositoryState<Nothing>
    data class Available<T>(val data: T, val source: DataSourceKey, val freshness: DataFreshness) : RepositoryState<T>
    data class Empty(val source: DataSourceKey, val freshness: DataFreshness) : RepositoryState<Nothing>
    data class Failure<T>(val source: DataSourceKey, val message: String, val retainedData: T?, val freshness: DataFreshness?) : RepositoryState<T>
}
