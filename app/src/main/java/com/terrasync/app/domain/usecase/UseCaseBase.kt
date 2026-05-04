package com.terrasync.app.domain.usecase

/**
 * Abstract base class for all use cases.
 *
 * A use case encapsulates a single business operation and acts as the
 * orchestration layer between the presentation and data layers.
 *
 * Subclasses provide [Params] (or [Unit] if no input is required) and [Result].
 *
 * Example:
 * ```kotlin
 * class GetSitesUseCase @Inject constructor(
 *     private val repo: SiteRepository
 * ) : UseCaseBase<Unit, kotlin.Result<List<Site>>>() {
 *     override suspend fun execute(params: Unit) = repo.getSites()
 * }
 * ```
 *
 * @param Params The input parameter type. Use [Unit] for parameterless use cases.
 * @param Result The output type (typically [kotlin.Result] wrapping a domain model).
 */
abstract class UseCaseBase<in Params, out Result> {

    /** Executes the use case business logic. Always called from a coroutine scope. */
    protected abstract suspend fun execute(params: Params): Result

    /** Invoke operator allows calling a use case instance like a function. */
    suspend operator fun invoke(params: Params): Result = execute(params)
}
