package com.medibridge.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates a video consultation URL without integrating any provider SDK.
 *
 * <p>Jitsi Meet rooms are created implicitly by visiting the URL - no account,
 * API key, or OAuth flow is involved, which is why this is a few lines rather
 * than a service integration. Point {@code medibridge.meeting.base-url} at
 * Zoom/Meet/Teams later and nothing here changes.
 *
 * <p>The room name embeds a random UUID rather than the appointment id: a
 * guessable room name would let anyone join a stranger's consultation.
 */
@Service
public class MeetingLinkService {

    private final String baseUrl;
    private final String roomPrefix;

    public MeetingLinkService(
            @Value("${medibridge.meeting.base-url}") String baseUrl,
            @Value("${medibridge.meeting.room-prefix}") String roomPrefix) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.roomPrefix = roomPrefix;
    }

    public String generate() {
        return baseUrl + roomPrefix + UUID.randomUUID().toString().replace("-", "");
    }
}
