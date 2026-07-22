package com.medibridge.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.medibridge.common.exception.OAuthException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Verifies a Google ID token server-side.
 *
 * <p>This class is the entire security boundary for Google Sign-In. The browser
 * sends a signed JWT issued by Google; we check that
 * <ul>
 *   <li>the signature matches Google's published keys (proves Google issued it),</li>
 *   <li>the {@code aud} claim is <em>our</em> client id (proves it was minted for
 *       this application, not some other site that also uses Google login),</li>
 *   <li>the issuer is Google and the token has not expired.</li>
 * </ul>
 *
 * <p>Skipping the audience check is the classic mistake: without it, anyone can
 * take a token issued for their own app and use it to sign in as that email here.
 *
 * <p>The email itself is never taken from the request body - only from the
 * verified token payload.
 */
@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${medibridge.google.client-id:}") String clientId) {
        this.clientId = clientId;

        this.verifier = (clientId == null || clientId.isBlank())
                ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                        .setAudience(Collections.singletonList(clientId))
                        .build();

        if (this.verifier == null) {
            log.warn("Google Sign-In is disabled: medibridge.google.client-id is not set");
        }
    }

    public boolean isEnabled() {
        return verifier != null;
    }

    /** @throws BadCredentialsException if the token is absent, forged, expired or for another audience. */
    public GoogleUser verify(String idTokenString) {
        if (verifier == null) {
            throw new OAuthException(
                    "Google Sign-In is not configured on this server");
        }
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new OAuthException("Google credential is required");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            log.debug("Google token verification threw: {}", e.getMessage());
            throw new OAuthException("Could not verify Google sign-in");
        }

        if (idToken == null) {
            // Bad signature, wrong audience, wrong issuer, or expired.
            throw new OAuthException("Invalid or expired Google sign-in");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // An unverified Google email could belong to someone else entirely.
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new OAuthException(
                    "Your Google email address is not verified");
        }

        return new GoogleUser(
                payload.getSubject(),
                payload.getEmail(),
                (String) payload.get("name"),
                (String) payload.get("picture"));
    }

    public record GoogleUser(String sub, String email, String name, String pictureUrl) {
    }
}
