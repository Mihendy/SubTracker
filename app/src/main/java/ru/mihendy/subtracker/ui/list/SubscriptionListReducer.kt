package ru.mihendy.subtracker.ui.list

fun reduceSubscriptionList(
    state: SubscriptionListState,
    action: SubscriptionListAction,
): SubscriptionListState = when (action) {
    SubscriptionListAction.Reload -> state.copy(
        loadId = state.loadId + 1,
        loading = true,
        loadFailed = false,
    )

    is SubscriptionListAction.Loaded ->
        if (state.loading && action.id == state.loadId) {
            state.copy(items = action.items, loading = false, loadFailed = false)
        } else state

    is SubscriptionListAction.LoadFailed ->
        if (state.loading && action.id == state.loadId) {
            state.copy(loading = false, loadFailed = true)
        } else state

    is SubscriptionListAction.ItemsChanged -> state.copy(items = action.items)
    is SubscriptionListAction.Open -> state.copy(editor = OpenEditor(action.id))
    is SubscriptionListAction.EditorOpened ->
        if (action.editor == state.editor) state.copy(editor = null) else state

    is SubscriptionListAction.AskToDelete ->
        if (state.mutation == null) state.copy(confirming = action.subscription) else state

    SubscriptionListAction.CancelDelete -> state.copy(confirming = null)
    SubscriptionListAction.ConfirmDelete -> {
        val subscription = state.confirming
        if (subscription == null || state.mutation != null) state
        else state.copy(confirming = null, mutation = ListMutation.Delete(subscription), notice = null)
    }

    is SubscriptionListAction.MutationFinished ->
        if (action.mutation != state.mutation) state
        else state.copy(
            mutation = null,
            notice = when (val mutation = action.mutation) {
                is ListMutation.Delete ->
                    if (action.saved) {
                        ListNotice.Deleted(mutation.subscription)
                    } else {
                        ListNotice.DeleteFailed(mutation)
                    }
                is ListMutation.Restore ->
                    if (action.saved) null else ListNotice.RestoreFailed(mutation)
            },
        )

    is SubscriptionListAction.NoticeClosed ->
        if (action.notice != state.notice) state
        else state.copy(
            notice = null,
            mutation = if (action.undo && action.notice is ListNotice.Deleted) {
                ListMutation.Restore(action.notice.subscription)
            } else state.mutation,
        )
}
