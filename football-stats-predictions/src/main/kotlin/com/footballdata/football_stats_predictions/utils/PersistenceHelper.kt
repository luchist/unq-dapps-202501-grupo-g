package com.footballdata.football_stats_predictions.utils

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
class PersistenceHelper {
    /**
     * Find in cache or fetch from an external source, then save to the database.
     */
    fun <T : Any, ID> getCachedOrFetch(
        repository: JpaRepository<T, ID>,
        findFunction: () -> T?,
        fetchFunction: () -> Any,
        entityMapper: (Any) -> T
    ): T {
        // Find in cache
        val cachedEntity = findFunction()

        // If it exists, return it
        if (cachedEntity != null) {
            return cachedEntity
        }

        // If it does not exist, fetch from the external source
        val fetchedData = fetchFunction()

        // Map the fetched data to the entity type and save it to the database
        val entity = entityMapper(fetchedData)
        return repository.save(entity)
    }
}