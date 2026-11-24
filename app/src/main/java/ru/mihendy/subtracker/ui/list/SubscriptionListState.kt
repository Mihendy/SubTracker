package ru.mihendy.subtracker.ui.list

import ru.mihendy.subtracker.data.Subscription
import ru.mihendy.subtracker.data.SubscriptionId

data class SubscriptionListState(
    val items: List<Subscription>? = null,
    val loadId: Long = 1,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val confirming: Subscription? = null,
    val mutation: ListMutation? = null,
    val notice: ListNotice? = null,
    val editor: OpenEditor? = null,
)

class OpenEditor(val id: SubscriptionId?)

sealed interface ListMutation {
    class Delete(val subscription: Subscription) : ListMutation
    class Restore(val subscription: Subscription) : ListMutation
}

sealed interface ListNotice {
    class Deleted(val subscription: Subscription) : ListNotice
    data class DeleteFailed(val request: ListMutation.Delete) : ListNotice
    data class RestoreFailed(val request: ListMutation.Restore) : ListNotice
}

sealed interface SubscriptionListAction {
    data object Reload : SubscriptionListAction
    data class Loaded(val id: Long, val items: List<Subscription>) : SubscriptionListAction
    data class LoadFailed(val id: Long) : SubscriptionListAction
    data class ItemsChanged(val items: List<Subscription>) : SubscriptionListAction
    data class Open(val id: SubscriptionId?) : SubscriptionListAction
    data class EditorOpened(val editor: OpenEditor) : SubscriptionListAction
    data class AskToDelete(val subscription: Subscription) : SubscriptionListAction
    data object CancelDelete : SubscriptionListAction
    data object ConfirmDelete : SubscriptionListAction
    data class MutationFinished(val mutation: ListMutation, val saved: Boolean) : SubscriptionListAction
    data class NoticeClosed(val notice: ListNotice, val undo: Boolean) : SubscriptionListAction
}
