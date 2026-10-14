package com.flexirate.config;

import com.flexirate.dto.EventDto;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

/**
 * Decorates normal DTO binding with recovery at the boundary of a single event.
 */
public final class EventBindingModule extends SimpleModule {
    /**
     * Decorates only EventDto binding, leaving invalid users and request structure as whole-request errors.
     */
    public EventBindingModule() {
        super("event-item-recovery");
        setDeserializerModifier(new ValueDeserializerModifier() {
            /** Retains Jackson's default field mapper and adds recovery only for the event item type. */
            @Override
            public ValueDeserializer<?> modifyDeserializer(DeserializationConfig deserializationConfig,
                                                           BeanDescription.Supplier beanDescription, ValueDeserializer<?> defaultDeserializer) {
                // Apply recovery only to individual events; other DTOs use normal Jackson validation.
                if (beanDescription.getBeanClass() == EventDto.class) {
                    return new RecoveringEventDeserializer(defaultDeserializer);
                }
                return defaultDeserializer;
            }
        });
    }

}
