package trade

import spock.lang.Specification
import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Shared

/**
 * ABOUTME: InboundSwiftSpec tests the technical ingestion, deduplication, and correlation of SWIFT messages.
 */
class InboundSwiftSpec extends Specification {
    @Shared protected ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "ingest#SwiftMessage creates InboundSwiftRaw with SHA-256 hash"() {
        given:
        def sampleMt730 = """{1:F01VIETBANK1XXX0000000000}{2:O7301200260515VIETBANK1XXX0000000000N}{4:
:20:APP-REF-001
:21:ADV-REF-001
:30:260515
:71B:CHARGES DETS
-}"""

        when:
        def result = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([
                rawContent: sampleMt730,
                sourceChannel: "SRC_MANUAL_UPLOAD",
                sourceFileName: "MT730_test.txt"
            ]).call()

        then:
        result.rawMessageId != null
        def raw = ec.entity.find("trade.swift.InboundSwiftRaw")
            .condition("rawMessageId", result.rawMessageId).one()
        if (raw.parseStatusEnumId != "PARSE_SUCCESS") println "DEBUG: Parse failure: ${raw.parseStatusEnumId} - ${raw.parseErrorText}"
        raw.contentHash != null
        raw.contentHash.length() == 64 // SHA-256 hex
        raw.parseStatusEnumId == "PARSE_SUCCESS"
        raw.messageType == "730"
    }
}
