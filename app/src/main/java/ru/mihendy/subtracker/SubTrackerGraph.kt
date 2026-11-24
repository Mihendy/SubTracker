package ru.mihendy.subtracker

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.mihendy.subtracker.data.SubscriptionRepository
import ru.mihendy.subtracker.data.SubscriptionStorage
import ru.mihendy.subtracker.ui.list.SubscriptionListService
import ru.mihendy.subtracker.ui.list.SubscriptionListStore

/** Зависимости, общие для всех экранов на время жизни процесса. */
class SubTrackerGraph(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val repository = SubscriptionRepository(
        SubscriptionStorage(context),
        Dispatchers.IO.limitedParallelism(1),
    )
    val subscriptionList = SubscriptionListStore()

    init {
        SubscriptionListService(repository, scope)
            .connect(subscriptionList.state, subscriptionList::dispatch)
    }
}
