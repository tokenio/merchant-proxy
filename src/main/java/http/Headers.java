package http;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class Headers {
    private static final ThreadLocal<Map<String, String>> localHeaders = new ThreadLocal<>();

    public static void setHeaders(Map<String, String> headers) {
        localHeaders.set(headers);
    }

    public static Map<String, String> getHeaders() {
        return Optional.ofNullable(localHeaders.get()).orElse(Collections.emptyMap());
    }

    public static Optional<String> getHeader(String key) {
        return Optional.ofNullable(getHeaders().getOrDefault(key, null));
    }
}
