package ru.mihendy.subtracker.ui.list

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.mihendy.subtracker.data.SubscriptionRepository
import timber.log.Timber

class SubscriptionListService(
    private val repository: SubscriptionRepository,
    private val scope: CoroutineScope,
) {
    fun connect(
        state: StateFlow<SubscriptionListState>,
        dispatch: (SubscriptionListAction) -> Unit,
    ) {
        scope.launch {
            repository.items.filterNotNull().collect {
                dispatch(SubscriptionListAction.ItemsChanged(it))
            }
        }
        scope.launch loads@ {
            state.map { it.loadId }.distinctUntilChanged().collect { id ->
                this@loads.launch {
                    val action = try {
                        repository.load()
                        SubscriptionListAction.Loaded(id, checkNotNull(repository.items.value))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Не удалось загрузить подписки")
                        SubscriptionListAction.LoadFailed(id)
                    }
                    dispatch(action)
                }
            }
        }
        scope.launch mutations@ {
            state.map { it.mutation }.distinctUntilChanged().collect { mutation ->
                if (mutation != null) {
                    // Сборщик продолжает наблюдать за состоянием, пока выполняется запись.
                    this@mutations.launch {
                        val saved = try {
                            when (mutation) {
                                is ListMutation.Delete -> repository.delete(mutation.subscription.uid)
                                is ListMutation.Restore -> repository.save(mutation.subscription)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.e(e, "Не удалось изменить список подписок")
                            false
                        }
                        dispatch(SubscriptionListAction.MutationFinished(mutation, saved))
                    }
                }
            }
        }
    }
}
