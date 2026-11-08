package com.travel.media.service;

import com.cloudinary.Cloudinary;
import com.sun.net.httpserver.HttpServer;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

/** Runs the real SDK over HTTP; an external Postman simulation can replace the in-process stand-in. */
class CloudinaryStorageHttpTest {
    HttpServer server;
    CloudinaryStorage storage;
    String base;
    volatile String mode = "ok";
    Set<String> assets = ConcurrentHashMap.newKeySet();

    @BeforeEach void setup() throws Exception {
        base = System.getProperty("cloudinary.simulation-url");
        if (base == null) {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                String text = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
                int code = 200;
                String response = "{\"result\":\"ok\"}";
                if (path.equals("/__control")) mode = exchange.getRequestURI().getQuery().replace("mode=", "");
                else if (mode.equals("error")) { code = 500; response = "{\"error\":{\"message\":\"Storage outage\"}}"; }
                else if (mode.equals("malformed")) response = "{}";
                else if (path.endsWith("/upload") || path.endsWith("/destroy")) {
                    String id = field(text, "public_id");
                    if (id == null || field(text, "api_key") == null || field(text, "signature") == null || field(text, "timestamp") == null) {
                        code = 400; response = "{\"error\":{\"message\":\"Signed fields missing\"}}";
                    } else if (path.endsWith("/upload")) {
                        assets.add(id);
                        response = "{\"public_id\":\""+id+"\",\"secure_url\":\"https://res.cloudinary.com/test/image/upload/"+id+".png\",\"bytes\":68,\"width\":2,\"height\":2}";
                    } else if (!"true".equals(field(text, "invalidate"))) {
                        code = 400; response = "{\"error\":{\"message\":\"CDN invalidation missing\"}}";
                    } else response = "{\"result\":\""+(assets.remove(id) ? "ok" : "not found")+"\"}";
                } else { code = 501; response = "{}"; }
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            });
            server.start(); base = "http://127.0.0.1:"+server.getAddress().getPort();
        }
        var client = new Cloudinary(Map.of("cloud_name", "test-cloud", "api_key", "test-key",
                "api_secret", "test-secret", "upload_prefix", base, "secure", false));
        storage = new CloudinaryStorage(new org.springframework.beans.factory.support.StaticListableBeanFactory(Map.of("cloudinary", client)).getBeanProvider(Cloudinary.class));
        mode("ok");
    }

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void sdkUploadsSignedMultipartAndDeletesWithInvalidation() throws Exception {
        var file = AtomicMediaServiceTest.png(1);
        var image = new ImageUploadValidator(new com.travel.media.config.MediaProperties()).read(file);
        String id = "memory-test/"+UUID.randomUUID();
        assertThat(storage.upload(image, id).getPublicId()).isEqualTo(id);
        storage.destroy(id);
        storage.destroy(id); // Cleanup remains safe after an acknowledged earlier deletion.
    }

    @Test void providerFailureIsNotReportedAsSuccessfulUpload() throws Exception {
        mode("error");
        var image = new ImageUploadValidator(new com.travel.media.config.MediaProperties()).read(AtomicMediaServiceTest.png(1));
        assertThatThrownBy(() -> storage.upload(image, "memory-test/"+UUID.randomUUID())).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException)e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_FAILED);
        assertThatThrownBy(() -> storage.destroy("memory-test/missing")).isInstanceOf(Exception.class);
    }

    @Test void invalidProviderMetadataIsRejected() throws Exception {
        mode("malformed");
        var image = new ImageUploadValidator(new com.travel.media.config.MediaProperties()).read(AtomicMediaServiceTest.png(1));
        assertThatThrownBy(() -> storage.upload(image, "memory-test/"+UUID.randomUUID())).isInstanceOf(AppException.class);
    }

    void mode(String mode) throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/__control?mode="+mode))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(200);
    }
    static String field(String text, String name) {
        if (text.contains("Content-Disposition:")) {
            var matcher = Pattern.compile("name=\""+name+"\".*?\\r\\n\\r\\n([^\\r\\n]*)", Pattern.DOTALL).matcher(text);
            return matcher.find() ? matcher.group(1) : null;
        }
        for (String pair : text.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(name))
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        }
        return null;
    }
}
