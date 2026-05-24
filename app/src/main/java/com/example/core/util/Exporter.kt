package com.example.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.core.database.AppDatabase
import com.example.domain.model.Category
import com.example.domain.model.PaymentHistory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderRule
import com.example.domain.model.Subscription
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

data class ImportResult(
    val subscriptionsCount: Int,
    val categoriesCount: Int,
    val paymentMethodsCount: Int,
    val historiesCount: Int
)

object Exporter {

    suspend fun exportToJSON(context: Context, database: AppDatabase): Pair<Int, String> {
        val subscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val histories = database.paymentHistoryDao().getAllHistory().first()
        val categories = database.categoryDao().getAllCategories().first()
        val paymentMethods = database.paymentMethodDao().getAllPaymentMethods().first()

        val rootJson = JSONObject()

        // 1. Subscriptions
        val subArray = JSONArray()
        for (sub in subscriptions) {
            val sObj = JSONObject().apply {
                put("id", sub.id)
                put("name", sub.name)
                put("amount", sub.amount)
                put("currency", sub.currency)
                put("categoryId", sub.categoryId)
                put("billingCycle", sub.billingCycle)
                put("customCycleValue", sub.customCycleValue)
                put("nextDueDate", sub.nextDueDate)
                put("paymentMethodId", sub.paymentMethodId ?: -1)
                put("isAutoRenew", sub.isAutoRenew)
                put("status", sub.status)
                put("importance", sub.importance)
                put("note", sub.note ?: "")
                put("managementUrl", sub.managementUrl ?: "")
                put("isPaused", sub.isPaused)
                put("createdAt", sub.createdAt)
                put("updatedAt", sub.updatedAt)
            }
            subArray.put(sObj)
        }
        rootJson.put("subscriptions", subArray)

        // 2. Histories
        val histArray = JSONArray()
        for (hist in histories) {
            val hObj = JSONObject().apply {
                put("id", hist.id)
                put("subscriptionId", hist.subscriptionId)
                put("paidDate", hist.paidDate)
                put("amount", hist.amount)
                put("currency", hist.currency)
                put("note", hist.note ?: "")
                put("createdAt", hist.createdAt)
            }
            histArray.put(hObj)
        }
        rootJson.put("histories", histArray)

        // 3. Categories
        val catArray = JSONArray()
        for (cat in categories) {
            val cObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("icon", cat.icon)
                put("color", cat.color)
                put("createdAt", cat.createdAt)
                put("updatedAt", cat.updatedAt)
            }
            catArray.put(cObj)
        }
        rootJson.put("categories", catArray)

        // 4. Payment Methods
        val pmArray = JSONArray()
        for (pm in paymentMethods) {
            val pObj = JSONObject().apply {
                put("id", pm.id)
                put("name", pm.name)
                put("type", pm.type)
                put("lastFourDigits", pm.lastFourDigits ?: "")
                put("note", pm.note ?: "")
                put("createdAt", pm.createdAt)
                put("updatedAt", pm.updatedAt)
            }
            pmArray.put(pObj)
        }
        rootJson.put("payment_methods", pmArray)

        val timestamp = System.currentTimeMillis()
        val filename = "kotnest_backup_$timestamp.json"
        
        // Write JSON to file in cache and trigger share sheet
        val backupFile = File(context.cacheDir, filename)
        backupFile.writeText(rootJson.toString(4))

        shareFile(context, backupFile, "application/json", "KotNest JSON Backup")
        return Pair(subscriptions.size, filename)
    }

    suspend fun importFromJSON(inputStream: InputStream, database: AppDatabase): ImportResult? {
        return try {
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)

            // Validate that we have at least subscriptions or relevant arrays
            if (!root.has("subscriptions") && !root.has("categories") && !root.has("payment_methods") && !root.has("histories")) {
                return null
            }

            val existingCategories = database.categoryDao().getAllCategories().first()
            val existingPaymentMethods = database.paymentMethodDao().getAllPaymentMethods().first()

            val categoryIdMap = mutableMapOf<Int, Int>() // originalId -> mappedLocalId
            val paymentMethodIdMap = mutableMapOf<Int, Int>()

            var importedCategoriesCount = 0
            var importedPaymentMethodsCount = 0
            var importedSubscriptionsCount = 0
            var importedHistoriesCount = 0

            // 1. Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val cObj = catArray.getJSONObject(i)
                    val originalId = cObj.getInt("id")
                    val name = cObj.getString("name").trim()

                    val existing = existingCategories.find { it.name.equals(name, ignoreCase = true) }
                    if (existing != null) {
                        categoryIdMap[originalId] = existing.id
                    } else {
                        val newCat = Category(
                            name = name,
                            icon = cObj.optString("icon", "📦"),
                            color = cObj.optString("color", "#00BCD4"),
                            createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        val insertedId = database.categoryDao().insertCategory(newCat)
                        categoryIdMap[originalId] = insertedId.toInt()
                        importedCategoriesCount++
                    }
                }
            }

            // 2. Payment Methods
            if (root.has("payment_methods")) {
                val pmArray = root.getJSONArray("payment_methods")
                for (i in 0 until pmArray.length()) {
                    val pObj = pmArray.getJSONObject(i)
                    val originalId = pObj.getInt("id")
                    val name = pObj.getString("name").trim()

                    val existing = existingPaymentMethods.find { it.name.equals(name, ignoreCase = true) }
                    if (existing != null) {
                        paymentMethodIdMap[originalId] = existing.id
                    } else {
                        val newPm = PaymentMethod(
                            name = name,
                            type = pObj.optString("type", "Other"),
                            lastFourDigits = pObj.optString("lastFourDigits", "").ifEmpty { null },
                            note = pObj.optString("note", "").ifEmpty { null },
                            createdAt = pObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = pObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        val insertedId = database.paymentMethodDao().insertPaymentMethod(newPm)
                        paymentMethodIdMap[originalId] = insertedId.toInt()
                        importedPaymentMethodsCount++
                    }
                }
            }

            // 3. Subscriptions
            if (root.has("subscriptions")) {
                val subArray = root.getJSONArray("subscriptions")
                val existingSubs = database.subscriptionDao().getAllSubscriptions().first()
                for (i in 0 until subArray.length()) {
                    val sObj = subArray.getJSONObject(i)
                    val originalPmId = sObj.optInt("paymentMethodId", -1)
                    val originalCatId = sObj.getInt("categoryId")

                    val mappedCatId = categoryIdMap[originalCatId] ?: originalCatId
                    val mappedPmId = if (originalPmId == -1) null else (paymentMethodIdMap[originalPmId] ?: originalPmId)

                    val subName = sObj.getString("name").trim()
                    val existingSub = existingSubs.find { it.name.equals(subName, ignoreCase = true) }

                    val sub = Subscription(
                        id = existingSub?.id ?: 0, // Overwrite existing to avoid duplications
                        name = subName,
                        amount = sObj.getDouble("amount"),
                        currency = sObj.optString("currency", "VND"),
                        categoryId = mappedCatId,
                        billingCycle = sObj.getString("billingCycle"),
                        customCycleValue = sObj.optInt("customCycleValue", 1),
                        nextDueDate = sObj.getLong("nextDueDate"),
                        paymentMethodId = mappedPmId,
                        isAutoRenew = sObj.optBoolean("isAutoRenew", true),
                        status = sObj.optString("status", "Upcoming"),
                        importance = sObj.optString("importance", "Medium"),
                        note = sObj.optString("note", "").ifEmpty { null },
                        managementUrl = sObj.optString("managementUrl", "").ifEmpty { null },
                        isPaused = sObj.optBoolean("isPaused", false),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    )
                    database.subscriptionDao().insertSubscription(sub)
                    importedSubscriptionsCount++
                }
            }

            // 4. Histories
            if (root.has("histories")) {
                val histArray = root.getJSONArray("histories")
                for (i in 0 until histArray.length()) {
                    val hObj = histArray.getJSONObject(i)
                    val originalSubId = hObj.getInt("subscriptionId")
                    
                    database.paymentHistoryDao().insertHistory(
                        PaymentHistory(
                            subscriptionId = originalSubId,
                            paidDate = hObj.getLong("paidDate"),
                            amount = hObj.getDouble("amount"),
                            currency = hObj.getString("currency"),
                            note = hObj.optString("note", "").ifEmpty { null },
                            createdAt = hObj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    importedHistoriesCount++
                }
            }

            ImportResult(
                subscriptionsCount = importedSubscriptionsCount,
                categoriesCount = importedCategoriesCount,
                paymentMethodsCount = importedPaymentMethodsCount,
                historiesCount = importedHistoriesCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportToCSV(context: Context, database: AppDatabase) {
        val subscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val histories = database.paymentHistoryDao().getAllHistory().first()
        val categories = database.categoryDao().getAllCategories().first()
        val rates = database.exchangeRateDao().getAllRatesDirect()
        val categoryMap = categories.associateBy { it.id }
        val convertUseCase = com.example.domain.usecase.ConvertAmountToVndUseCase()

        val csvBuilder = java.lang.StringBuilder()
        
        // Header
        csvBuilder.append("Type,ID,Name,Amount,Currency,Estimated VND,Category,Billing Cycle,Next Due Date/Paid Date,Status/Note,Paused,Updated At\n")

        // Append Subscriptions with estimated values mapped cleanly
        for (sub in subscriptions) {
            val catName = categoryMap[sub.categoryId]?.name ?: "Other"
            val nextDue = DateUtils.formatEpoch(sub.nextDueDate, "yyyy-MM-dd")
            val updated = DateUtils.formatEpoch(sub.updatedAt, "yyyy-MM-dd HH:mm")
            val conversionResult = convertUseCase(sub.amount, sub.currency, rates)
            val estVndStr = if (conversionResult.isRateAvailable) conversionResult.estimatedVndAmount?.toInt()?.toString() ?: "" else "N/A"
            csvBuilder.append(
                "Subscription,${sub.id},\"${sub.name.replace("\"", "\"\"")}\",${sub.amount},${sub.currency},$estVndStr,\"$catName\",\"${sub.billingCycle}\",$nextDue,${sub.status},${sub.isPaused},$updated\n"
            )
        }

        // Header for Histories
        csvBuilder.append("\nType,History ID,Subscription ID,Amount,Currency,Paid Date,Note,Created At\n")
        for (hist in histories) {
            val paidD = DateUtils.formatEpoch(hist.paidDate, "yyyy-MM-dd")
            val created = DateUtils.formatEpoch(hist.createdAt, "yyyy-MM-dd HH:mm")
            csvBuilder.append(
                "PaymentHistory,${hist.id},${hist.subscriptionId},${hist.amount},${hist.currency},$paidD,\"${(hist.note ?: "").replace("\"", "\"\"")}\",$created\n"
            )
        }

        val csvFile = File(context.cacheDir, "kotnest_report_${System.currentTimeMillis()}.csv")
        csvFile.writeText(csvBuilder.toString())

        shareFile(context, csvFile, "text/csv", "KotNest CSV Report")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
