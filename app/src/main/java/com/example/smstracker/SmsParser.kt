package com.example.smstracker

data class Txn(
    val timestamp: Long,
    val type: String,      // "DEBIT" or "CREDIT"
    val amount: Double,
    val merchant: String,
    val account: String,
    val balance: String,
    val sender: String,
    val body: String
)

/** Parses typical Indian bank / UPI / card SMS alerts. Returns null for non-transaction SMS. */
object SmsParser {
    private val amountRe = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val debitRe = Regex("""(?i)\b(debited|spent|withdrawn|purchase|paid|sent|debit)\b""")
    private val creditRe = Regex("""(?i)\b(credited|received|deposited|refund(?:ed)?|credit)\b""")
    private val skipRe = Regex(
        """(?i)\b(otp|one time password|verification code|will be debited|mandate|reminder|is due|due on|pre-approved|offer|cashback offer)\b"""
    )
    private val acctRe = Regex("""(?i)(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending)?\s*[x*]*\s*([0-9]{3,6})""")
    private val balRe = Regex(
        """(?i)(?:avl\.?\s*bal(?:ance)?|available\s*bal(?:ance)?|bal(?:ance)?)\s*(?:is|:)?\s*(?:rs\.?|inr|₹)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )
    private val merchantRe = Regex("""(?i)(?:\bat|\bto|\bfrom|\bby|info[:\-]?|towards)\s+([A-Za-z0-9@&_\-*. ]{2,40})""")
    private val cutRe = Regex("""(?i)\s+(on|ref|via|using|avl|bal|upi|if|not|dated|txn|from|is|has|was)\b.*""")

    fun parse(sender: String, body: String, timestamp: Long): Txn? {
        if (skipRe.containsMatchIn(body)) return null

        val amountMatch = amountRe.findAll(body).firstOrNull { m ->
            val before = body.substring(maxOf(0, m.range.first - 18), m.range.first).lowercase()
            !before.contains("bal")
        } ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        val d = debitRe.find(body)
        val c = creditRe.find(body)
        val type = when {
            d == null && c == null -> return null
            d == null -> "CREDIT"
            c == null -> "DEBIT"
            d.range.first < c.range.first -> "DEBIT"
            else -> "CREDIT"
        }

        val account = acctRe.find(body)?.groupValues?.get(1) ?: ""
        val balance = balRe.find(body)?.groupValues?.get(1)?.replace(",", "") ?: ""

        var merchant = ""
        for (m in merchantRe.findAll(body)) {
            val cand = m.groupValues[1].replace(cutRe, "").trim().trimEnd('.', ',', '-')
            val low = cand.lowercase()
            if (cand.length < 2) continue
            if (low.startsWith("a/c") || low.startsWith("acct") || low.startsWith("account") ||
                low.startsWith("your") || low.startsWith("card") || low.startsWith("rs") ||
                low.startsWith("inr") || low.first().isDigit() && low.all { it.isDigit() }
            ) continue
            merchant = cand
            break
        }

        return Txn(timestamp, type, amount, merchant, account, balance, sender, body.replace("\n", " ").trim())
    }
}
