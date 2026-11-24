package ru.mihendy.subtracker.ui.list

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SubscriptionListStore {
    private val current = MutableStateFlow(SubscriptionListState())
    val state = current.asStateFlow()

    fun dispatch(action: SubscriptionListAction) {
        current.update { reduceSubscriptionList(it, action) }
    }
}
