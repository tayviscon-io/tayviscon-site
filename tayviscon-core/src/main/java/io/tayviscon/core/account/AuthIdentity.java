package io.tayviscon.core.account;

import java.util.UUID;

/** Связка OAuth-идентичности провайдера с идентификатором локального аккаунта. */
public record AuthIdentity(String provider, String externalId, UUID accountId) {}
