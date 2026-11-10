package ua.ofkkt.offlineshowcase.domain

/** Слайд 11: після скількох мілісекунд кеш «застарів» (у слайді — 30 хв). */
const val CACHE_TIMEOUT_MS: Long = 30 * 60 * 1000L

/** Слайд 11: shouldRefresh() = минуло більше часу, ніж CACHE_TIMEOUT_MS. */
fun shouldRefresh(lastSync: Long, now: Long, timeoutMs: Long = CACHE_TIMEOUT_MS): Boolean =
    now - lastSync > timeoutMs

/** Слайд 12: стратегії розв'язання конфліктів, які можна реалізувати в навчальному проєкті. */
enum class ConflictStrategy(val label: String) {
    ServerWins("Last-Write-Wins"),
    Timestamp("Timestamp-based"),
}

data class Resolution(val winner: Product, val localWon: Boolean)

/**
 * Чиста функція (її легко тестувати без Android).
 * - ServerWins: серверна версія повністю замінює локальну.
 * - Timestamp: виграє версія з пізнішим updatedAt (при рівності — сервер).
 */
fun resolveConflict(local: Product?, remote: Product, strategy: ConflictStrategy): Resolution {
    if (local == null) return Resolution(remote, localWon = false)
    return when (strategy) {
        ConflictStrategy.ServerWins -> Resolution(remote, localWon = false)
        ConflictStrategy.Timestamp ->
            if (local.updatedAt > remote.updatedAt) Resolution(local, localWon = true)
            else Resolution(remote, localWon = false)
    }
}

/** Конфліктом вважаємо випадок, коли локальні й серверні значення різні. */
fun isConflict(local: Product?, remote: Product): Boolean =
    local != null && (local.price != remote.price || local.name != remote.name)
