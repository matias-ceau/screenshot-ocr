package com.screenshot.ocr

import com.screenshot.ocr.models.ChatMessage
import com.screenshot.ocr.models.MessageAlignment

/**
 * Detects and formats chat conversations from extracted text
 */
class ChatDetector {

    private val chatPatterns = listOf(
        // "[ALIGNMENT] Name: message" - with alignment marker
        Regex("""^\[(LEFT|RIGHT)\]\s*([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$"""),
        // "[ALIGNMENT] [Time] Name: message"
        Regex("""^\[(LEFT|RIGHT)\]\s*([A-Z][a-zA-Z\s]{0,30})\s*[\[\(]([0-9:APM\s]+)[\]\)]\s*:\s*(.+)$"""),
        // "[ALIGNMENT] Name [Time]: message"
        Regex("""^\[(LEFT|RIGHT)\]\s*([A-Z][a-zA-Z\s]{0,30})\s*[\[\(]([0-9:APM\s]+)[\]\)]\s*:\s*(.+)$"""),
        // "Name: message" - without alignment marker (fallback)
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

    private val quotePattern = Regex("""\[QUOTE:\s*([^\]]+)\]\s*(.*?)\s*\[/QUOTE\]""", RegexOption.DOT_MATCHES_ALL)

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

            // Pattern 0: "[ALIGNMENT] Name: message"
            chatPatterns[0].matchEntire(trimmedLine)?.let { match ->
                val (alignmentStr, speaker, messageContent) = match.destructured
                val alignment = when (alignmentStr.uppercase()) {
                    "LEFT" -> MessageAlignment.LEFT
                    "RIGHT" -> MessageAlignment.RIGHT
                    else -> MessageAlignment.UNKNOWN
                }

                val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                if (speaker.length in 1..30) {
                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        lineNumber = lineNum,
                        alignment = alignment,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 1: "[ALIGNMENT] Name [Time]: message"
            if (!matched) {
                chatPatterns[1].matchEntire(trimmedLine)?.let { match ->
                    val groups = match.groupValues
                    val alignmentStr = groups.getOrNull(1) ?: ""
                    val speaker = groups.getOrNull(2) ?: ""
                    val timestamp = groups.getOrNull(3) ?: ""
                    val messageContent = groups.getOrNull(4) ?: ""

                    val alignment = when (alignmentStr.uppercase()) {
                        "LEFT" -> MessageAlignment.LEFT
                        "RIGHT" -> MessageAlignment.RIGHT
                        else -> MessageAlignment.UNKNOWN
                    }

                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum,
                        alignment = alignment,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 2: "[ALIGNMENT] Name [Time]: message" (alternative format)
            if (!matched) {
                chatPatterns[2].matchEntire(trimmedLine)?.let { match ->
                    val groups = match.groupValues
                    val alignmentStr = groups.getOrNull(1) ?: ""
                    val speaker = groups.getOrNull(2) ?: ""
                    val timestamp = groups.getOrNull(3) ?: ""
                    val messageContent = groups.getOrNull(4) ?: ""

                    val alignment = when (alignmentStr.uppercase()) {
                        "LEFT" -> MessageAlignment.LEFT
                        "RIGHT" -> MessageAlignment.RIGHT
                        else -> MessageAlignment.UNKNOWN
                    }

                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum,
                        alignment = alignment,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 3: "Name: message" (fallback without alignment)
            if (!matched) {
                chatPatterns[3].matchEntire(trimmedLine)?.let { match ->
                    val (speaker, messageContent) = match.destructured
                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    if (speaker.length in 1..30) {
                        currentMessage = ChatMessage(
                            speaker = speaker.trim(),
                            message = actualMessage.trim(),
                            lineNumber = lineNum,
                            quotedSpeaker = quotedSpeaker,
                            quotedMessage = quotedMsg
                        )
                        messages.add(currentMessage!!)
                        matched = true
                    }
                }
            }

            // Pattern 4: "[Time] Name: message"
            if (!matched) {
                chatPatterns[4].matchEntire(trimmedLine)?.let { match ->
                    val (timestamp, speaker, messageContent) = match.destructured
                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 5: "Name [Time]: message"
            if (!matched) {
                chatPatterns[5].matchEntire(trimmedLine)?.let { match ->
                    val (speaker, timestamp, messageContent) = match.destructured
                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // Pattern 6: "HH:MM Name: message"
            if (!matched) {
                chatPatterns[6].matchEntire(trimmedLine)?.let { match ->
                    val (timestamp, speaker, messageContent) = match.destructured
                    val (quotedSpeaker, quotedMsg, actualMessage) = extractQuote(messageContent)

                    currentMessage = ChatMessage(
                        speaker = speaker.trim(),
                        message = actualMessage.trim(),
                        timestamp = timestamp.trim(),
                        lineNumber = lineNum,
                        quotedSpeaker = quotedSpeaker,
                        quotedMessage = quotedMsg
                    )
                    messages.add(currentMessage!!)
                    matched = true
                }
            }

            // If no pattern matched and we have a current message, it's a continuation
            if (!matched && currentMessage != null) {
                // Check if this looks like a continuation (doesn't start with alignment marker or capital + colon)
                if (!Regex("""^(\[(?:LEFT|RIGHT)\]\s*)?[A-Z][a-zA-Z\s]*:""").matches(trimmedLine)) {
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
     * Extract quote information from a message
     * Returns (quotedSpeaker, quotedMessage, remainingMessage)
     */
    private fun extractQuote(messageContent: String): Triple<String?, String?, String> {
        val match = quotePattern.find(messageContent)
        return if (match != null) {
            val quotedSpeaker = match.groupValues.getOrNull(1)?.trim()
            val quotedMessage = match.groupValues.getOrNull(2)?.trim()
            val remainingMessage = messageContent.replace(match.value, "").trim()
            Triple(quotedSpeaker, quotedMessage, remainingMessage)
        } else {
            Triple(null, null, messageContent)
        }
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
        val alignmentCounts = messages.groupingBy { it.alignment }.eachCount()

        sb.appendLine("Participants: ${speakers.keys.joinToString(", ")}")
        sb.appendLine("Total messages: ${messages.size}")

        // Show alignment distribution if detected
        if (alignmentCounts.containsKey(MessageAlignment.LEFT) || alignmentCounts.containsKey(MessageAlignment.RIGHT)) {
            val leftSpeakers = messages.filter { it.alignment == MessageAlignment.LEFT }
                .map { it.speaker }.distinct()
            val rightSpeakers = messages.filter { it.alignment == MessageAlignment.RIGHT }
                .map { it.speaker }.distinct()

            if (leftSpeakers.isNotEmpty()) {
                sb.appendLine("Left-aligned: ${leftSpeakers.joinToString(", ")}")
            }
            if (rightSpeakers.isNotEmpty()) {
                sb.appendLine("Right-aligned: ${rightSpeakers.joinToString(", ")}")
            }
        }

        val quotedMessages = messages.count { it.quotedSpeaker != null }
        if (quotedMessages > 0) {
            sb.appendLine("Messages with quotes: $quotedMessages")
        }

        sb.appendLine()
        sb.appendLine("=" * 60)
        sb.appendLine()

        for (msg in messages) {
            // Format alignment indicator
            val alignmentIndicator = when (msg.alignment) {
                MessageAlignment.LEFT -> "◄ "
                MessageAlignment.RIGHT -> "► "
                MessageAlignment.UNKNOWN -> ""
            }

            // Format: [Alignment] [Time] Speaker:
            val header = buildString {
                append(alignmentIndicator)
                if (msg.timestamp != null) {
                    append("[${msg.timestamp}] ")
                }
                append("${msg.speaker}:")
            }

            sb.appendLine(header)

            // If there's a quote, show it first
            if (msg.quotedSpeaker != null && msg.quotedMessage != null) {
                sb.appendLine("  ┌─ Quoting ${msg.quotedSpeaker}:")
                msg.quotedMessage.lines().forEach { line ->
                    sb.appendLine("  │ $line")
                }
                sb.appendLine("  └─")
            }

            // Show the actual message
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
