package com.example.domain.usecase

class SmartDueSuggestionEngine {
    fun suggest(dueName: String): SuggestionResult? {
        val name = dueName.trim()
        if (name.length < 2) return null

        val nameLower = name.lowercase()

        // 1 = Cloud / Storage
        // 2 = Entertainment
        // 3 = Education
        // 4 = Server / Tech
        // 5 = Internet / Phone
        // 6 = Work
        // 7 = Personal
        // 8 = Other

        return when {
            nameLower.contains("google one") || nameLower.contains("icloud") || nameLower.contains("dropbox") || nameLower.contains("onedrive") || nameLower.contains("drive") -> {
                SuggestionResult(
                    suggestedCategoryId = 1,
                    suggestedCategoryName = "Cloud / Storage",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3, 7),
                    confidence = 0.9,
                    explanationText = "Suggested Cloud / Storage category with Monthly billing and Medium importance."
                )
            }
            nameLower.contains("spotify") || nameLower.contains("apple music") || nameLower.contains("youtube") || nameLower.contains("deezer") -> {
                SuggestionResult(
                    suggestedCategoryId = 2,
                    suggestedCategoryName = "Entertainment",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Low",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.9,
                    explanationText = "Suggested Entertainment category with Monthly billing and Low importance."
                )
            }
            nameLower.contains("netflix") || nameLower.contains("hbo") || nameLower.contains("disney") || nameLower.contains("prime video") || nameLower.contains("hulu") -> {
                SuggestionResult(
                    suggestedCategoryId = 2,
                    suggestedCategoryName = "Entertainment",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.9,
                    explanationText = "Suggested Entertainment category with Monthly billing and Medium importance."
                )
            }
            nameLower.contains("domain") || nameLower.contains("godaddy") || nameLower.contains("namecheap") || nameLower.contains("name.com") -> {
                SuggestionResult(
                    suggestedCategoryId = 4,
                    suggestedCategoryName = "Server / Tech",
                    suggestedBillingCycle = "Yearly",
                    suggestedImportance = "High",
                    suggestedReminderDays = listOf(1, 3, 7),
                    confidence = 0.85,
                    explanationText = "Suggested Server / Tech category with Yearly billing and High importance (crucial domain renew)."
                )
            }
            nameLower.contains("vps") || nameLower.contains("aws") || nameLower.contains("digitalocean") || nameLower.contains("linode") || nameLower.contains("azure") || nameLower.contains("heroku") -> {
                SuggestionResult(
                    suggestedCategoryId = 4,
                    suggestedCategoryName = "Server / Tech",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "High",
                    suggestedReminderDays = listOf(1, 3, 7),
                    confidence = 0.9,
                    explanationText = "Suggested Server / Tech category with Monthly billing and High importance."
                )
            }
            nameLower.contains("hosting") || nameLower.contains("bluehost") || nameLower.contains("hostinger") || nameLower.contains("vultr") -> {
                SuggestionResult(
                    suggestedCategoryId = 4,
                    suggestedCategoryName = "Server / Tech",
                    suggestedBillingCycle = "Yearly",
                    suggestedImportance = "High",
                    suggestedReminderDays = listOf(1, 3, 7),
                    confidence = 0.85,
                    explanationText = "Suggested Server / Tech category with Yearly billing and High importance."
                )
            }
            nameLower.contains("internet") || nameLower.contains("wifi") || nameLower.contains("fpt") || nameLower.contains("viettel") || nameLower.contains("vnpt") -> {
                SuggestionResult(
                    suggestedCategoryId = 5,
                    suggestedCategoryName = "Internet / Phone",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "High",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.9,
                    explanationText = "Suggested Internet / Phone category with Monthly billing and High importance."
                )
            }
            nameLower.contains("phone bill") || nameLower.contains("phone") || nameLower.contains("sim") || nameLower.contains("mobifone") || nameLower.contains("vinaphone") -> {
                SuggestionResult(
                    suggestedCategoryId = 5,
                    suggestedCategoryName = "Internet / Phone",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.85,
                    explanationText = "Suggested Internet / Phone category with Monthly billing."
                )
            }
            nameLower.contains("course") || nameLower.contains("udemy") || nameLower.contains("coursera") || nameLower.contains("edx") || nameLower.contains("school") || nameLower.contains("tuition") -> {
                SuggestionResult(
                    suggestedCategoryId = 3,
                    suggestedCategoryName = "Education",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.8,
                    explanationText = "Suggested Education category with Monthly billing."
                )
            }
            nameLower.contains("chatgpt") || nameLower.contains("copilot") || nameLower.contains("claude") || nameLower.contains("midjourney") || nameLower.contains("gemini") || nameLower.contains("openai") -> {
                SuggestionResult(
                    suggestedCategoryId = 6,
                    suggestedCategoryName = "Work",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.9,
                    explanationText = "Suggested Work AI assistant with Monthly billing and Medium importance."
                )
            }
            nameLower.contains("cloudflare") || nameLower.contains("dns") || nameLower.contains("github") || nameLower.contains("gitlab") || nameLower.contains("docker") || nameLower.contains("npm") -> {
                SuggestionResult(
                    suggestedCategoryId = 4,
                    suggestedCategoryName = "Server / Tech",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "High",
                    suggestedReminderDays = listOf(1, 3, 7),
                    confidence = 0.85,
                    explanationText = "Suggested Server / Tech developer tools with High importance."
                )
            }
            // Add some supplementary keywords
            nameLower.contains("gym") || nameLower.contains("fitness") || nameLower.contains("yoga") -> {
                SuggestionResult(
                    suggestedCategoryId = 7,
                    suggestedCategoryName = "Personal",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.8,
                    explanationText = "Suggested Personal wellness category and Monthly billing."
                )
            }
            nameLower.contains("zoom") || nameLower.contains("slack") || nameLower.contains("notion") || nameLower.contains("figma") || nameLower.contains("adobe") || nameLower.contains("office 365") || nameLower.contains("microsoft") -> {
                SuggestionResult(
                    suggestedCategoryId = 6,
                    suggestedCategoryName = "Work",
                    suggestedBillingCycle = "Monthly",
                    suggestedImportance = "Medium",
                    suggestedReminderDays = listOf(1, 3),
                    confidence = 0.8,
                    explanationText = "Suggested Work or productivity tools with Monthly billing."
                )
            }
            else -> null
        }
    }
}

data class SuggestionResult(
    val suggestedCategoryId: Int,
    val suggestedCategoryName: String,
    val suggestedBillingCycle: String,
    val suggestedImportance: String,
    val suggestedReminderDays: List<Int>,
    val confidence: Double,
    val explanationText: String
)
