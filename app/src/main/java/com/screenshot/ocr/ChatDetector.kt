package com.screenshot.ocr

import com.screenshot.ocr.models.ChatMessage

/**
 * Detects and formats chat conversations from extracted text
 */
class ChatDetector {

    private val chatPatterns = listOf(
        // "Name: message"
        Regex("""^([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$"""),
        // "[Time] Name: message" or "(Time) Name: message"
        Regex("""^[\[\(]([0-9:APM\s]+)[\]\)]\s*([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$"""),
        // "Name [Time]: message"
        Regex("""^([A-Z][a-zA-Z\s]{0,30})\s*[\[\(]([0-9:APM\s]+)[\]\)]\s*:\s*(.+)$"""),
        // "HH:MM Name: message"
        Regex("""^([0-9]{1,2}:[0-9]{2}\s*(?:AM|PM)?)\s+([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$"""),
        // WhatsApp/Telegram: "[HH:MM] Name:" followed by message on next line
        Regex("""^[\[\(]([0-9]{1,2}:[0-9]{2})[\]\)]\s*([A-Z][a-zA-Z\s]+):?\s*$""")
    )

    private val timePatterns = listOf(
        Regex("""\b([0-9]{1,2}:[0-9]{2}\s*(?:AM|PM)?)\b"""),
        Regex("""\b([0-9]{1,2}:[0-9]{2}:[0-9]{2})\b""")
    )

    /**
     * Determine if text is likely a chat conversation
     */
    fun isLikelyChat(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 3) return false

        var chatIndicators = 0
        val totalLines = lines.size

        for (line in lines) {
            // Check for chat patterns
            if (chatPatterns.any { it.matches(line) }) {
                chatIndicators++
            }

            // Check for timestamps
            if (timePatterns.any { it.containsMatchIn(line) }) {
                chatIndicators++
            }
        }

        // If more than 30% of lines match chat patterns
        return (chatIndicators.toFloat() / totalLines) > 0.3f
    }

    /**
     * Extract chat messages from text
     */
    fun extractMessages(text: String): List<ChatMessage> {
        val lines = text.lines()
        val messages = mutableListOf<ChatMessage>()
        var currentMessage: ChatMessage? = null

        for ((lineNum, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            var matched = false

            // Pattern 1: "Name: message"
            chatPatterns[0].matchEntire(trimmedLine)?.let { match ->
                val (speaker, message) = match.destructured
                if (speaker.length in 1..30) {
                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = message.trim(),
                        lineNumber = lineNum
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 2: "[Time] Name: message"
            if (!matched) {
                chatPatterns[1].matchEntire(trimmedLine)?.let { match ->
                    val (timestamp, speaker, message) = match.destructured
                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = message.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 3: "Name [Time]: message"
            if (!matched) {
                chatPatterns[2].matchEntire(trimmedLine)?.let { match ->
                    val (speaker, timestamp, message) = match.destructured
                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = message.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 4: "HH:MM Name: message"
            if (!matched) {
                chatPatterns[3].matchEntire(trimmedLine)?.let { match ->
                    val (timestamp, speaker, message) = match.destructured
                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = message.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // If no pattern matched and we have a current message, it's a continuation
            if (!matched && currentMessage != null) {
                // Check if this looks like a continuation (doesn't start with capital + colon)
                if (!Regex("""^[A-Z][a-zA-Z\s]*:""").matches(trimmedLine)) {
                    currentMessage = currentMessage!!.copy(
                        message = currentMessage!!.message + "\n" + trimmedLine
                    )
                    // Update the message in the list
                    if (messages.isNotEmpty()) {
                        messages[messages.lastIndex] = currentMessage!!
                    }
                }
            }
        }

        return messages
    }

    /**
     * Format messages into a readable conversation
     */
    fun formatConversation(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=" * 60)
        sb.appendLine("CHAT CONVERSATION")
        sb.appendLine("=" * 60)
        sb.appendLine()

        // Get conversation statistics
        val speakers = messages.groupingBy { it.speaker }.eachCount()
        sb.appendLine("Participants: ${speakers.keys.joinToString(", ")}")
        sb.appendLine("Total messages: ${messages.size}")
        sb.appendLine()
        sb.appendLine("=" * 60)
        sb.appendLine()

        for (msg in messages) {
            // Format: [Time] Speaker:
            val header = if (msg.timestamp != null) {
                "[${msg.timestamp}] ${msg.speaker}:"
            } else {
                "${msg.speaker}:"
            }

            sb.appendLine(header)

            // Indent the message
            msg.message.lines().forEach { line ->
                sb.appendLine("  $line")
            }

            sb.appendLine() // Empty line between messages
        }

        sb.appendLine("=" * 60)

        return sb.toString()
    }

    /**
     * Detect and format chat from text
     * Returns (isChat, messages)
     */
    fun detectAndFormat(text: String): Pair<Boolean, List<ChatMessage>> {
        val isChat = isLikelyChat(text)
        return if (isChat) {
            val messages = extractMessages(text)
            Pair(true, messages)
        } else {
            Pair(false, emptyList())
        }
    }

    private operator fun String.times(n: Int): String = this.repeat(n)
}
