package com.footballdata.football_stats_predictions.utils

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
class PersistenceHelper {
    /**
     * Función genérica para buscar datos en caché o recuperarlos del origen y persistirlos
     */
    fun <T : Any, ID> getCachedOrFetch(
        repository: JpaRepository<T, ID>,
        findFunction: () -> T?,
        fetchFunction: () -> Any,
        entityMapper: (Any) -> T
    ): T {
        // 1. Buscar en la base de datos
        val cachedEntity = findFunction()

        // 2. Si existe en caché, devolverlo
        if (cachedEntity != null) {
            return cachedEntity
        }

        // 3. Si no existe, obtener desde la fuente externa
        val fetchedData = fetchFunction()

        // 4. Mapear los datos a una entidad y guardarla
        val entity = entityMapper(fetchedData)
        return repository.save(entity)
    }
}