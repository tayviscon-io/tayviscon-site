package io.tayviscon.core.account;

import java.util.Set;
import java.util.UUID;

/** Локальный аккаунт сайта, привязанный к одной или нескольким OAuth-идентичностям. */
public record Account(UUID id, String displayName, String avatarUrl, Set<String> roles) {}
