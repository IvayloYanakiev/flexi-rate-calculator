package com.flexirate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configures strict document parsing with narrowly scoped recovery for individual event DTOs.
 */
@Configuration
public class JsonConfiguration {
    /**
     * Registers event recovery and disables silent scalar conversions that could turn invalid input into usage.
     */
    @Bean
    public JsonMapper jsonMapper() {
        JsonFactory jsonFactory = JsonFactory.builder().streamReadConstraints(
                        StreamReadConstraints.builder()
                                .maxNestingDepth(16)
                                .maxStringLength(2048)
                                .maxNameLength(128)
                                .maxNumberLength(32)
                                .build()
                )
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        return JsonMapper.builder(jsonFactory)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .withCoercionConfig(LogicalType.Textual, coercionConfig -> coercionConfig
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .addModule(new EventBindingModule())
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }
}
