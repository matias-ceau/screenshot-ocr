"""
Chat conversation detection and formatting module.
"""

import re
from typing import List, Dict, Tuple, Optional
from dataclasses import dataclass
from datetime import datetime


@dataclass
class ChatMessage:
    """Represents a single chat message."""
    speaker: str
    message: str
    timestamp: Optional[str] = None
    line_number: int = 0


class ChatDetector:
    """Detects and formats chat conversations from OCR text."""

    def __init__(self):
        """Initialize the chat detector with common patterns."""
        # Common chat patterns
        self.patterns = [
            # Pattern: "Name: message" or "Name : message"
            re.compile(r'^([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$'),
            # Pattern: "[Time] Name: message" or "(Time) Name: message"
            re.compile(r'^[\[\(]([0-9:APM\s]+)[\]\)]\s*([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$'),
            # Pattern: "Name [Time]: message"
            re.compile(r'^([A-Z][a-zA-Z\s]{0,30})\s*[\[\(]([0-9:APM\s]+)[\]\)]\s*:\s*(.+)$'),
            # Pattern: "Name (Time) message" (no colon)
            re.compile(r'^([A-Z][a-zA-Z\s]{0,30})\s*[\[\(]([0-9:APM\s]+)[\]\)]\s+(.+)$'),
            # Pattern: Messenger/WhatsApp style "Name" on one line, message on next
            re.compile(r'^([A-Z][a-zA-Z\s]{0,30})$'),
            # Pattern: Time-stamped messages "HH:MM Name: message"
            re.compile(r'^([0-9]{1,2}:[0-9]{2}\s*(?:AM|PM)?)\s+([A-Z][a-zA-Z\s]{0,30})\s*:\s*(.+)$'),
        ]

        # Common time patterns
        self.time_patterns = [
            re.compile(r'\b([0-9]{1,2}:[0-9]{2}\s*(?:AM|PM)?)\b'),
            re.compile(r'\b([0-9]{1,2}:[0-9]{2}:[0-9]{2})\b'),
        ]

    def is_likely_chat(self, text: str) -> bool:
        """
        Determine if the text is likely a chat conversation.

        Args:
            text: Input text

        Returns:
            True if text appears to be a chat conversation
        """
        lines = [line.strip() for line in text.split('\n') if line.strip()]

        if len(lines) < 3:
            return False

        chat_indicators = 0
        total_lines = len(lines)

        for line in lines:
            # Check for chat patterns
            for pattern in self.patterns[:3]:  # Check first 3 most common patterns
                if pattern.match(line):
                    chat_indicators += 1
                    break

            # Check for timestamps
            for time_pattern in self.time_patterns:
                if time_pattern.search(line):
                    chat_indicators += 0.5
                    break

        # If more than 30% of lines match chat patterns, it's likely a chat
        confidence = chat_indicators / total_lines
        return confidence > 0.3

    def extract_messages(self, text: str) -> List[ChatMessage]:
        """
        Extract chat messages from text.

        Args:
            text: Input text

        Returns:
            List of ChatMessage objects
        """
        lines = text.split('\n')
        messages = []
        current_message = None

        for i, line in enumerate(lines):
            line = line.strip()
            if not line:
                continue

            matched = False

            # Try each pattern
            # Pattern 1: "Name: message"
            match = self.patterns[0].match(line)
            if match and not matched:
                speaker, message = match.groups()
                if len(speaker) > 0 and len(speaker) < 30:
                    current_message = ChatMessage(
                        speaker=speaker.strip(),
                        message=message.strip(),
                        line_number=i
                    )
                    messages.append(current_message)
                    matched = True

            # Pattern 2: "[Time] Name: message"
            if not matched:
                match = self.patterns[1].match(line)
                if match:
                    timestamp, speaker, message = match.groups()
                    current_message = ChatMessage(
                        speaker=speaker.strip(),
                        message=message.strip(),
                        timestamp=timestamp.strip(),
                        line_number=i
                    )
                    messages.append(current_message)
                    matched = True

            # Pattern 3: "Name [Time]: message"
            if not matched:
                match = self.patterns[2].match(line)
                if match:
                    speaker, timestamp, message = match.groups()
                    current_message = ChatMessage(
                        speaker=speaker.strip(),
                        message=message.strip(),
                        timestamp=timestamp.strip(),
                        line_number=i
                    )
                    messages.append(current_message)
                    matched = True

            # Pattern 5: "HH:MM Name: message"
            if not matched:
                match = self.patterns[5].match(line)
                if match:
                    timestamp, speaker, message = match.groups()
                    current_message = ChatMessage(
                        speaker=speaker.strip(),
                        message=message.strip(),
                        timestamp=timestamp.strip(),
                        line_number=i
                    )
                    messages.append(current_message)
                    matched = True

            # If no pattern matched and we have a current message, it might be continuation
            if not matched and current_message:
                # Check if this looks like a continuation (doesn't start with capital + colon)
                if not re.match(r'^[A-Z][a-zA-Z\s]*:', line):
                    current_message.message += '\n' + line

        return messages

    def format_conversation(self, messages: List[ChatMessage],
                           include_timestamps: bool = True) -> str:
        """
        Format chat messages into a readable conversation.

        Args:
            messages: List of ChatMessage objects
            include_timestamps: Whether to include timestamps

        Returns:
            Formatted conversation string
        """
        if not messages:
            return ""

        formatted = []
        formatted.append("=" * 60)
        formatted.append("CHAT CONVERSATION")
        formatted.append("=" * 60)
        formatted.append("")

        for msg in messages:
            # Format: [Time] Speaker: Message
            if include_timestamps and msg.timestamp:
                header = f"[{msg.timestamp}] {msg.speaker}:"
            else:
                header = f"{msg.speaker}:"

            formatted.append(header)

            # Indent the message
            message_lines = msg.message.split('\n')
            for line in message_lines:
                formatted.append(f"  {line}")

            formatted.append("")  # Empty line between messages

        formatted.append("=" * 60)
        formatted.append(f"Total messages: {len(messages)}")
        formatted.append("=" * 60)

        return '\n'.join(formatted)

    def get_conversation_summary(self, messages: List[ChatMessage]) -> Dict[str, any]:
        """
        Get summary statistics about the conversation.

        Args:
            messages: List of ChatMessage objects

        Returns:
            Dictionary with conversation statistics
        """
        if not messages:
            return {
                'total_messages': 0,
                'participants': [],
                'message_count_by_speaker': {}
            }

        speakers = {}
        for msg in messages:
            speaker = msg.speaker
            if speaker not in speakers:
                speakers[speaker] = 0
            speakers[speaker] += 1

        return {
            'total_messages': len(messages),
            'participants': list(speakers.keys()),
            'message_count_by_speaker': speakers,
            'has_timestamps': any(msg.timestamp for msg in messages)
        }

    def process_text(self, text: str) -> Tuple[bool, str]:
        """
        Process text and format as chat if detected.

        Args:
            text: Input text

        Returns:
            Tuple of (is_chat, formatted_text)
        """
        is_chat = self.is_likely_chat(text)

        if is_chat:
            messages = self.extract_messages(text)
            if messages:
                formatted = self.format_conversation(messages)
                summary = self.get_conversation_summary(messages)

                # Add summary at the top
                summary_text = (
                    f"Detected chat conversation with {summary['total_messages']} messages\n"
                    f"Participants: {', '.join(summary['participants'])}\n\n"
                )

                return True, summary_text + formatted
            else:
                return False, text
        else:
            return False, text
