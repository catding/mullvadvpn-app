package net.mullvad.mullvadvpn.service

import kotlin.math.min
import kotlinx.coroutines.delay
import net.mullvad.mullvadvpn.model.GetAccountDataResult
import net.mullvad.mullvadvpn.util.JobTracker
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class AccountCache(val daemon: MullvadDaemon, val settingsListener: SettingsListener) {
    companion object {
        public val EXPIRY_FORMAT = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss z")
    }

    private val jobTracker = JobTracker()
    private val subscriptionId = settingsListener.accountNumberNotifier.subscribe { accountNumber ->
        handleNewAccountNumber(accountNumber)
    }

    private var accountNumber: String? = null
    private var accountExpiry: DateTime? = null

    var onAccountDataChange: ((String?, DateTime?) -> Unit)? = null
        set(value) {
            synchronized(this) {
                field = value
                notifyChange()
            }
        }

    fun fetchAccountExpiry() {
        accountNumber?.let { account ->
            jobTracker.newBackgroundJob("fetch") {
                var retryAttempt = 0

                while (onAccountDataChange != null) {
                    val result = daemon.getAccountData(account)

                    if (result is GetAccountDataResult.Ok) {
                        handleNewExpiry(account, result.accountData.expiry)
                        break
                    } else if (result is GetAccountDataResult.InvalidAccount) {
                        break
                    }

                    retryAttempt += 1
                    delay(calculateRetryFetchDelay(retryAttempt))
                }
            }
        }
    }

    fun onDestroy() {
        settingsListener.accountNumberNotifier.unsubscribe(subscriptionId)
        jobTracker.cancelAllJobs()
    }

    private fun handleNewAccountNumber(newAccountNumber: String?) {
        synchronized(this) {
            accountNumber = newAccountNumber
            accountExpiry = null

            notifyChange()
            fetchAccountExpiry()
        }
    }

    private fun handleNewExpiry(accountNumberUsedForFetch: String, expiryString: String) {
        synchronized(this) {
            if (accountNumber === accountNumberUsedForFetch) {
                accountExpiry = DateTime.parse(expiryString, EXPIRY_FORMAT)
                notifyChange()
            }
        }
    }

    private fun notifyChange() {
        onAccountDataChange?.invoke(accountNumber, accountExpiry)
    }

    private fun calculateRetryFetchDelay(retryAttempt: Int): Long {
        // delay in seconds = 2 ^ retryAttempt capped at 2^13 (8192)
        val exponent = min(retryAttempt, 13)

        return (1L shl exponent) * 1000L
    }
}
