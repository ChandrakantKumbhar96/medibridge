package com.medibridge.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Backs the chat-service's get_reschedule_status tool - see spring_client.py. */
public record RescheduleStatusResponse(

        @JsonProperty("reschedule_count")
        Integer rescheduleCount,

        @JsonProperty("max_reschedules")
        Integer maxReschedules,

        @JsonProperty("can_reschedule")
        Boolean canReschedule
) {
}
