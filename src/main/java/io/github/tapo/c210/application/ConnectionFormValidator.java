package io.github.tapo.c210.application;

import io.github.tapo.c210.domain.StreamQuality;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Validates and normalizes the manual connection form without network access. */
public final class ConnectionFormValidator {
    public ConnectionFormValidation validate(ConnectionFormInput input) {
        Objects.requireNonNull(input, "input must not be null");
        var errors = new ArrayList<String>();
        var host = trimToEmpty(input.host());
        if (!isIpv4(host)) {
            errors.add("IPアドレスはIPv4形式で入力してください");
        }

        var onvifPort = parsePort(input.onvifPort(), "ONVIFポート", errors);
        var rtspPort = parsePort(input.rtspPort(), "RTSPポート", errors);
        var username = trimToEmpty(input.username());
        if (username.isBlank()) {
            errors.add("ユーザー名を入力してください");
        }
        if (input.password() == null || input.password().isBlank()) {
            errors.add("パスワードを入力してください");
        }
        if (input.streamQuality() == null) {
            errors.add("ストリーム画質を選択してください");
        }

        if (!errors.isEmpty()) {
            return new ConnectionFormValidation(errors, Optional.empty());
        }

        return new ConnectionFormValidation(
                List.of(),
                Optional.of(new ValidatedConnectionForm(
                        host,
                        onvifPort,
                        rtspPort,
                        username,
                        input.password(),
                        input.streamQuality(),
                        input.remember())));
    }

    private static Integer parsePort(String raw, String label, List<String> errors) {
        try {
            var port = Integer.parseInt(trimToEmpty(raw));
            if (port < 1 || port > 65535) {
                errors.add(label + "は1〜65535の範囲で入力してください");
                return null;
            }
            return port;
        } catch (NumberFormatException exception) {
            errors.add(label + "は1〜65535の数値で入力してください");
            return null;
        }
    }

    private static boolean isIpv4(String host) {
        var octets = host.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (var octet : octets) {
            try {
                if (octet.isBlank() || (octet.length() > 1 && octet.startsWith("0"))) {
                    return false;
                }
                var value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
