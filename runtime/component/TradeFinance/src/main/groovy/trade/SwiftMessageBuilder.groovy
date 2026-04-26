package trade

import org.moqui.context.ExecutionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// ABOUTME: SwiftMessageBuilder provides a robust DSL for constructing SWIFT MT messages.
// ABOUTME: Handles block headers, field formatting, character set enforcement, and line wrapping.

class SwiftMessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SwiftMessageBuilder.class)
    
    private final ExecutionContext ec
    private String messageType
    private String senderBic
    private String receiverBic
    private String senderRef
    private final Map<String, Object> fields = [:]
    
    SwiftMessageBuilder(ExecutionContext ec) {
        this.ec = ec
    }
    
    SwiftMessageBuilder setMessageType(String type) { this.messageType = type; this }
    SwiftMessageBuilder setSenderBic(String bic) { this.senderBic = bic; this }
    SwiftMessageBuilder setReceiverBic(String bic) { this.receiverBic = bic; this }
    SwiftMessageBuilder setSenderRef(String ref) { this.senderRef = ref; this }
    
    SwiftMessageBuilder addField(String tag, Object value) {
        if (value != null && value.toString().trim() != "") {
            fields[tag] = value
        }
        this
    }
    
    /**
     * Formats a value for SWIFT compliance.
     * - Character set enforcement (X charset)
     * - Line wrapping (max 35 or 65 chars)
     * - Decimal replacement ( . -> , )
     */
    String formatValue(Object value, int maxLines = 1, int maxChars = 35) {
        if (value == null) return ""
        String clean = value.toString()
            .replaceAll(/[^A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\r\n]/, "") // X charset enforcement
            .replaceAll(/\./, ",") // SWIFT decimal convention
            
        if (maxLines == 1) {
            return clean.length() > maxChars ? clean.substring(0, maxChars) : clean
        }
        
        // Wrap lines
        def lines = []
        def currentLine = new StringBuilder()
        clean.split(/\r?\n/).each { rawLine ->
            rawLine.split(" ").each { word ->
                if (currentLine.length() + word.length() + 1 > maxChars) {
                    if (currentLine.length() > 0) lines << currentLine.toString()
                    currentLine = new StringBuilder(word)
                } else {
                    if (currentLine.length() > 0) currentLine.append(" ")
                    currentLine.append(word)
                }
            }
            if (currentLine.length() > 0) {
                lines << currentLine.toString()
                currentLine = new StringBuilder()
            }
        }
        
        return lines.take(maxLines).join("\r\n")
    }

    String build() {
        if (!messageType || !senderBic || !receiverBic) {
            throw new IllegalArgumentException("Message Type, Sender BIC, and Receiver BIC are required")
        }

        StringBuilder sb = new StringBuilder()
        
        // Block 1: Basic Header
        sb.append("{1:F01").append(senderBic.padRight(12, 'X')).append("0000000000}")
        
        // Block 2: Application Header
        String inputOutput = "I" // Input by default
        sb.append("{2:").append(inputOutput).append(messageType)
          .append(receiverBic.padRight(12, 'X')).append("N}")
        
        // Block 4: Text Block
        sb.append("{4:\r\n")
        
        // Tag 20: Sender's Reference (Mandatory)
        sb.append(":20:").append(senderRef ?: "NONREF").append("\r\n")
        
        // Other tags
        fields.each { tag, value ->
            if (tag == "20") return // Skip tag 20 if already added manually
            sb.append(":").append(tag).append(":").append(value.toString()).append("\r\n")
        }
        
        sb.append("-}")
        
        return sb.toString()
    }
}
