package com.travel.planning.ai.resilience;

import com.travel.planning.retrieval.RagDemoProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

/** Bounds connect/read phases to the remaining AI request budget. */
public class AiDeadlineRequestFactory extends SimpleClientHttpRequestFactory {
    private final AiRequestContext context;
    private final RagDemoProperties properties;
    public AiDeadlineRequestFactory(AiRequestContext context, RagDemoProperties properties) {
        this.context = context; this.properties = properties;
    }
    @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
        super.prepareConnection(connection, method);
        long remaining = Math.min(context.remainingMillis(), Math.max(2, properties.getCallTimeoutMillis()));
        if (remaining < 2) throw new SocketTimeoutException("AI deadline exceeded");
        int connect = (int) Math.min(5000, remaining / 2);
        int read = (int) Math.min(Integer.MAX_VALUE, remaining - connect);
        connection.setConnectTimeout(Math.max(1, connect)); connection.setReadTimeout(Math.max(1, read));
    }
}
