package com.medibridge.common.security;

import java.lang.annotation.*;

/**
 * Injects the authenticated {@link SecurityUser} into a controller method.
 *
 * <pre>{@code
 * @GetMapping("/records")
 * public List<RecordResponse> myRecords(@CurrentUser SecurityUser me) { ... }
 * }</pre>
 *
 * <p>Always take the caller's identity from here, never from a request
 * parameter or body - that is what stops a patient reading another patient's
 * records by editing an id.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
