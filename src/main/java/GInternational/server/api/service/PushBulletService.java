package GInternational.server.api.service;

import GInternational.server.api.entity.WebSocketMessage;
import GInternational.server.api.repository.WebSocketMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okio.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(value = "clientServerTransactionManager")
@RequiredArgsConstructor
public class PushBulletService {

    private final String accessToken = "o.aMZRjSs0OtgF5aPe2sY2cV76lO8cfC7k";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebSocketMessageRepository webSocketMessageRepository;

    public void startWebSocket() {
        Request request = new Request.Builder()
                .url("wss://stream.pushbullet.com/websocket/" + accessToken)
                .build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket Connected!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("Received: " + text);
                handleIncomingMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                String hexMessage = bytes.hex();
                System.out.println("Received bytes: " + hexMessage);
                handleIncomingMessage(hexMessage);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                System.out.println("Closing : " + code + " / " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.out.println("Error : " + t.getMessage());
            }
        };

        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    private void handleIncomingMessage(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            String type = rootNode.path("type").asText();
            System.out.println("Message type: " + type);
            if ("push".equals(type)) {
                JsonNode pushNode = rootNode.path("push");
                String pushType = pushNode.path("type").asText();
                System.out.println("Push type: " + pushType);
                if ("sms_changed".equals(pushType)) {
                    JsonNode notificationNode = pushNode.path("notifications").get(0);
                    long timestamp = notificationNode.path("timestamp").asLong();
                    String phoneNumber = notificationNode.path("title").asText();
                    String body = notificationNode.path("body").asText();
                    System.out.println("Timestamp: " + timestamp);
                    System.out.println("Phone number: " + phoneNumber);
                    System.out.println("Body: " + body);

                    // 특정 키워드가 포함된 경우 저장하지 않음
                    if (body.contains("해외에서 발송되었습니다")) {
                        return;
                    }

                    String amount = extractAmountFromBody(body);
                    String depositor = extractDepositorFromBody(body);

                    LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

                    WebSocketMessage webSocketMessage = new WebSocketMessage();
                    webSocketMessage.setTimestamp(dateTime);
                    webSocketMessage.setPhone(phoneNumber);
                    webSocketMessage.setDepositor(depositor);
                    webSocketMessage.setAmount(amount);
                    webSocketMessage.setMessage(body);

                    saveMessage(webSocketMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMessage(WebSocketMessage message) {
        try {
            webSocketMessageRepository.save(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractAmountFromBody(String body) {
        String amount = "";

        // 패턴 목록
        Pattern[] patterns = {
                Pattern.compile("입금\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?"), // 패턴: 입금 5,000,000원
                Pattern.compile("지급\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?"), // 패턴: 지급 280,000원
                Pattern.compile("전자금융입금\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?"), // 패턴: 전자금융입금 40,608원
                Pattern.compile("CMS입금\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?"), // 패턴: CMS입금 40,608원
                Pattern.compile("무통장입금\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?"), // 패턴: 무통장입금 40,608원
                Pattern.compile("창구입금\\s?(\\d{1,3}(,\\d{3})*(\\.\\d+)?)(원)?") // 패턴: 창구입금 40,608원
        };

        // 패턴 매칭
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                amount = matcher.group(1).replace(",", "").trim(); // 쉼표 제거 및 트림
                break;
            }
        }

        return amount;
    }

    private String extractDepositorFromBody(String body) {
        String depositor = "No depositor found";
        int bestMatchLength = 0;

        // 패턴 목록
        Pattern[] patterns = {
                Pattern.compile("\\[국제발신\\]\\n하나, \\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d+\\*{4}\\d+\\n(\\S+)\\n입금 \\d+,?\\d*원\\n잔액 \\d+[,\\d]*원"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n하나,\\d{2}\\/\\d{2},\\d{2}:\\d{2}\\n\\d+\\*{4}\\d+\\n입금\\d+,?\\d*원\\n(\\S+)\\n잔액\\d+,?\\d*원"),
                Pattern.compile("\\[Web발신\\]\\n\\d{4}\\/\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n입금 \\d+,?\\d*원\\n잔액 \\d+,?\\d*원\\n(\\S+)\\n\\d+\\*{3}\\d+"),
                Pattern.compile("\\[Web발신\\]\\n<새마을금고>\\d+\\*{2,4}\\d+\\n(\\S+)\\n입금\\d+,?\\d*원\\n잔액\\d+,?\\d*원\\n\\d{2}\\/\\d{2} \\d{2}:\\d{2}"),
                Pattern.compile("케이뱅크 안내\\n\\[Web발신\\]\\n\\[케이뱅크\\] \\n(\\S+?) \\(\\d+\\)\\n입금 \\d+,?\\d*원 \\n잔액 \\d+,?\\d*"),
                Pattern.compile("\\[Web발신\\]\\n농협 입금\\d+,?\\d*원\\n\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d+-\\*{4}-\\d+\\n(\\S+)\\n잔액 \\d+,?\\d*원"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n\\[KB\\]\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d{6}\\*\\*\\d{3}\\n(\\S+)\\n전자금융입금\\n\\d+,?\\d*\\n잔액\\d+,?\\d*"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n\\[KB\\]\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d{6}\\*\\*\\d{3}\\n(\\S+)\\nCMS입금\\n\\d+,?\\d*\\n잔액\\d+,?\\d*"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n\\[KB\\]\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d{6}\\*\\*\\d{3}\\n(\\S+)\\n무통장입금\\n\\d+,?\\d*\\n잔액\\d+,?\\d*"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n\\[KB\\]\\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\d{6}\\*\\*\\d{3}\\n(\\S+)\\n창구입금\\n\\d+,?\\d*\\n잔액\\d+,?\\d*"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n우리 \\d{2}\\/\\d{2} \\d{2}:\\d{2}\\n\\*\\d+\\n입금 \\d+,?\\d*원\\n(\\S+)"),
                Pattern.compile("\\[Web발신\\]\\n\\[신한은행\\] \\d{2}\\/\\d{2}\\n\\d{2}:\\d{2}:\\d{2}\\.\\d{2}\\n\\[\\d{3}-\\*{3}-\\d{6}\\]\\n지급 \\d+,?\\d*원\\n\\((\\S+)\\)"),
                Pattern.compile("\\[국제발신\\]\\n\\[Web발신\\]\\n우체국,\\d{2}:\\d{2}\\n\\d{3}\\*{8}\\d{3}\\n입금\\d+,?\\d*원\\n잔액\\d+,?\\d*원\\n(\\S+)")
        };

        // 패턴 매칭
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find() && matcher.group(0).length() > bestMatchLength) {
                bestMatchLength = matcher.group(0).length();
                depositor = matcher.group(matcher.groupCount()).trim();
            }
        }

        return depositor;
    }
}
