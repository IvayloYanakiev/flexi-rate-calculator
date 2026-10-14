package com.flexirate.config;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Reads one event safely: copy its JSON, use Jackson's normal binding, and return null for bad types.
 * For example, duration_minutes: [15] becomes a skipped event while the next event can still be read.
 */
final class RecoveringEventDeserializer extends DelegatingDeserializer {
    /**
     * Keeps the framework-provided deserializer so record/property mappings remain automatic.
     */
    RecoveringEventDeserializer(ValueDeserializer<?> delegateDeserializer) {
        super(delegateDeserializer);
    }

    /**
     * Preserves recovery when Jackson contextualizes or replaces its underlying deserializer.
     */
    @Override
    protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> delegateDeserializer) {
        return new RecoveringEventDeserializer(delegateDeserializer);
    }

    /**
     * Consumes one complete event, binds it in isolation, and yields a null placeholder on type failures.
     */
    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        try (TokenBuffer eventTokenBuffer = deserializationContext.bufferForInputBuffering(jsonParser)) {
            // Syntax/duplicate-key/resource-limit errors here invalidate the document.
            // Finish consuming this item before binding, so a bad field cannot swallow the next item.
            eventTokenBuffer.copyCurrentStructure(jsonParser);
            try (JsonParser eventJsonParser = eventTokenBuffer.asParserOnFirstToken(jsonParser.objectReadContext(), jsonParser)) {
                return _delegatee.deserialize(eventJsonParser, deserializationContext);
            } catch (MismatchedInputException | InputCoercionException bindingException) {
                // Keep one null entry; BillingService counts it as one skipped event.
                return null;
            }
        }
    }
}
