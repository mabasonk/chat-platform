package com.jse.chat.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jse.chat.model.ChatMessage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatIntegrationTest {

    private final List<WebSocketStompClient> stompClients = new CopyOnWriteArrayList<>();

    private static final long SUBSCRIPTION_STABILISE_MS = 500;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String wsUrl;
    private HttpClient httpClient;


    @BeforeEach
    void setUp() {

        baseUrl = "http://localhost:" + port;
        wsUrl  = "ws://localhost:" + port;
        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {

        stompClients.forEach(WebSocketStompClient::stop);
        stompClients.clear();
        httpClient.close();
    }

    @Test
    @DisplayName("10 concurrent users send 3 messages each — all 300 deliveries received")
    public void multipleUsersCanChatConcurrently() throws Exception {

        int userCount       = 10;
        int messagesPerUser = 3;
        int totalMessages   = userCount * messagesPerUser;           // 30 sent
        int totalDeliveries = totalMessages * userCount;             // 300 expected

        List<ChatMessage> receivedMessages   = new CopyOnWriteArrayList<>();
        CountDownLatch    subscribersReady   = new CountDownLatch(userCount);
        CountDownLatch    allDelivered       = new CountDownLatch(totalDeliveries);
        List<StompSession> sessions          = new CopyOnWriteArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<?>> connectFutures = new CopyOnWriteArrayList<>();
            for (int i = 0; i < userCount; i++) {
                connectFutures.add(executor.submit(() -> {
                    StompSession session = connectAndSubscribe(message -> {
                        receivedMessages.add(message);
                        allDelivered.countDown();
                    });
                    sessions.add(session);
                    subscribersReady.countDown();
                }));
            }

            for (Future<?> f : connectFutures) {
                f.get(15, TimeUnit.SECONDS);
            }

            boolean allConnected = subscribersReady.await(15, TimeUnit.SECONDS);
            Assertions.assertThat(allConnected)
                    .as("All %d users should connect within 15 seconds", userCount)
                    .isTrue();

            Thread.sleep(SUBSCRIPTION_STABILISE_MS);

            List<Future<?>> sendFutures = new CopyOnWriteArrayList<>();
            for (int i = 0; i < userCount; i++) {
                String username = "User-" + i;
                sendFutures.add(executor.submit(() -> {
                    for (int m = 0; m < messagesPerUser; m++) {
                        sendMessage(username, "Message-" + m + " from " + username);
                    }
                }));
            }

            for (Future<?> f : sendFutures) {
                f.get(10, TimeUnit.SECONDS);
            }

            // Step 3 — wait for all broadcast deliveries
            boolean success = allDelivered.await(20, TimeUnit.SECONDS);

            Assertions.assertThat(success)
                    .as("All %d deliveries should complete — received %d so far",
                            totalDeliveries, receivedMessages.size())
                    .isTrue();

            Assertions.assertThat(receivedMessages)
                    .hasSize(totalDeliveries);

        } finally {
            sessions.forEach(s -> { if (s.isConnected()) s.disconnect(); });
        }
    }

    @Test
    @DisplayName("No messages are dropped when 20 users send simultaneously")
    public void noMessagesDroppedUnderConcurrentLoad() throws Exception {

        int userCount = 20;
        AtomicInteger accepted = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new CopyOnWriteArrayList<>();

            for (int i = 0; i < userCount; i++) {
                String username = "LoadUser-" + i;
                futures.add(executor.submit(() -> {
                    int status = sendMessage(username, "Load message from " + username);
                    if (status == 202) accepted.incrementAndGet();
                }));
            }

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        }

        assertThat(accepted.get())
                .as("All %d messages should be accepted with HTTP 202", userCount)
                .isEqualTo(userCount);

        Thread.sleep(1_000);

        long loadMessages = getHistory().stream()
                .filter(m -> m.username().startsWith("LoadUser-"))
                .count();

        assertThat(loadMessages)
                .as("All %d messages should appear in history", userCount)
                .isEqualTo(userCount);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Connects a STOMP WebSocket session and subscribes to /topic/messages.
     * Each received message is passed to the provided handler.
     */
    private StompSession connectAndSubscribe(Consumer<ChatMessage> onMessage) {

        try {
            // Raw WebSocket — no SockJS transport negotiation
            WebSocketStompClient stompClient = new WebSocketStompClient(
                    new StandardWebSocketClient()  // ← not SockJsClient
            );

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);



            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setObjectMapper(mapper);

            stompClient.setMessageConverter(converter);
            stompClients.add(stompClient);

            CompletableFuture<StompSession> sessionFuture = new CompletableFuture<>();

            stompClient.connectAsync(wsUrl + "/ws-raw", new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders headers) {
                    session.subscribe("/topic/messages", new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return ChatMessage.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            onMessage.accept((ChatMessage) payload);
                        }
                    });
                    sessionFuture.complete(session);
                }
            });

            return sessionFuture.get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    /**
     * Sends a chat message via HTTP POST and returns the response status code.
     */
    private int sendMessage(String username, String content) {

        try {
            String body = """
                    {"username": "%s", "content": "%s"}
                    """.formatted(username, content);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<Void> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message for " + username, e);
        }
    }

    /**
     * Fetches the full chat history via HTTP GET.
     */
    private List<ChatMessage> getHistory() {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat/history"))
                    .GET()
                    .build();

            String body = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString())
                    .body();

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule());

            return mapper.readValue(
                    body,
                    mapper.getTypeFactory().constructCollectionType(List.class, ChatMessage.class)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch history", e);
        }
    }
}
