package com.microsoft.identity.nativeauth

import android.content.Context
import com.microsoft.applications.events.EventProperties
import com.microsoft.applications.events.HttpClient
import com.microsoft.applications.events.ILogger
import com.microsoft.applications.events.LogManager
import com.microsoft.applications.events.OfflineRoom
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.logging.DiagnosticContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.platform.Device
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

class AriaSpanExporter(context: Context, ariaToken: String?) : SpanExporter {
    companion object {
        internal val TAG = AriaSpanExporter::class.java.toString()
        init {
            System.loadLibrary("maesdk")
        }
    }
    private val logger: ILogger

    init {
        HttpClient(context)
        OfflineRoom.connectContext(context)
        logger = LogManager.initialize(ariaToken)
    }

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        for (span in spans) {
            val eventProperties = getEventPropertiesFromSpan(span)
            logger.logEvent(eventProperties)
        }

        return flush()
    }

    private fun getEventPropertiesFromSpan(span: SpanData): EventProperties {
        val eventProperties = EventProperties(span.name)
        eventProperties.setProperty("span_kind", span.kind.name)
        eventProperties.setProperty("trace_id", span.traceId)
        eventProperties.setProperty("span_id", span.spanId)
        eventProperties.setProperty("span_status", span.status.statusCode.name)
        eventProperties.setProperty("event_duration_ms", (span.endEpochNanos - span.startEpochNanos) / 1_000_000)
        eventProperties.setProperty("msal_library", DiagnosticContext.INSTANCE.requestContext[AuthenticationConstants.SdkPlatformFields.PRODUCT])
        eventProperties.setProperty("msal_version", Device.getProductVersion())

        span.attributes.forEach { attributeKey, attributeValue ->
            fillProperties(eventProperties, attributeKey, attributeValue)
        }

        return eventProperties
    }

    private fun fillProperties(
        eventProperties: EventProperties,
        attributeKey: AttributeKey<*>,
        attributeValue: Any) {
        val methodTag = "$TAG:fillProperties"

        when (attributeKey.type) {
            AttributeType.STRING -> eventProperties.setProperty(attributeKey.key, attributeValue as String)
            AttributeType.BOOLEAN -> eventProperties.setProperty(attributeKey.key, attributeValue as Boolean)
            AttributeType.LONG -> eventProperties.setProperty(attributeKey.key, attributeValue as Long)
            AttributeType.DOUBLE -> eventProperties.setProperty(attributeKey.key, attributeValue as Long)
            else -> Logger.error(methodTag, "Unsupported attribute of type: ${attributeKey.type}", null)
        }
    }

    override fun flush(): CompletableResultCode {
        return try {
            LogManager.flush()
            CompletableResultCode.ofSuccess()
        } catch(t: Throwable) {
            CompletableResultCode.ofFailure()
        }
    }

    override fun shutdown(): CompletableResultCode {
        return try {
            LogManager.flushAndTeardown()
            CompletableResultCode.ofSuccess()
        } catch(t: Throwable) {
            CompletableResultCode.ofFailure()
        }
    }

}