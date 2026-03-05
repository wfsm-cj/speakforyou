package com.speakforyou.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakforyou.common.BizException;
import com.speakforyou.model.entity.PersonaEntity;
import com.speakforyou.model.entity.SceneEntity;
import com.speakforyou.model.entity.UserEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class AiService {

    private final String baseUrl;
    private final String systemApiKey;
    private final String defaultModel;
    private final ObjectMapper objectMapper;
    private final RagService ragService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiService(
            @Value("${app.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${app.dashscope.system-api-key:}") String systemApiKey,
            @Value("${app.dashscope.default-model:qwen-plus}") String defaultModel,
            ObjectMapper objectMapper,
            RagService ragService
    ) {
        this.baseUrl = baseUrl;
        this.systemApiKey = systemApiKey;
        this.defaultModel = defaultModel;
        this.objectMapper = objectMapper;
        this.ragService = ragService;
    }

    public void streamStructuredQuickReply(UserEntity user, PersonaEntity persona, SceneEntity scene, String message, SseEmitter emitter) {
        executor.submit(() -> {
            try {
                List<Map<String, Object>> replies = generateQuickReplyList(user, persona, scene, message);
                for (Map<String, Object> reply : replies) {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of(
                            "type", "reply",
                            "data", reply
                    ))));
                }
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of(
                        "type", "complete",
                        "data", replies
                ))));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of(
                            "type", "error",
                            "data", e.getMessage() == null ? "AI 服务调用失败" : e.getMessage()
                    ))));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(new BizException(5001, "AI 服务调用失败: " + e.getMessage()));
            }
        });
    }

    private List<Map<String, Object>> generateQuickReplyList(UserEntity user, PersonaEntity persona, SceneEntity scene, String message) {
        String apiKey = user.getApiKey() != null && !user.getApiKey().isBlank() ? user.getApiKey() : systemApiKey;
        String modelName = user.getModelName() == null || user.getModelName().isBlank() ? defaultModel : user.getModelName();
        List<String> ragHints = ragService.retrieveForQuickReply(apiKey, message, persona.getName(), scene.getName(), 5);
        String ragBlock = ragHints.isEmpty()
                ? "（暂无匹配话术）"
                : IntStream.range(0, ragHints.size())
                .mapToObj(i -> (i + 1) + ". " + ragHints.get(i))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("（暂无匹配话术）");
        String prompt = """
                你是一位职场沟通助手。
                人格风格：%s
                风格描述：%s
                语气特点：%s
                场景：%s（%s）
                场景补充：%s

                对方消息：%s

                参考话术（RAG 检索结果）：
                %s

                请输出严格 JSON 数组，包含 3 条不同风格回复：
                [{"id":1,"reply":"...","style":"..."},{"id":2,"reply":"...","style":"..."},{"id":3,"reply":"...","style":"..."}]
                """.formatted(
                persona.getName(),
                persona.getDescription(),
                persona.getTone(),
                scene.getName(),
                scene.getDescription(),
                scene.getPromptHint() == null ? "" : scene.getPromptHint(),
                message,
                ragBlock
        );

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackQuickReplyList(persona, scene);
        }

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
        StringBuilder full = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();
        model.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                full.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                done.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errors.add(error);
                done.countDown();
            }
        });

        try {
            boolean finished = done.await(60, TimeUnit.SECONDS);
            if (!finished) {
                throw new BizException(5001, "AI 响应超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(5001, "AI 响应中断");
        }
        if (!errors.isEmpty()) {
            throw new BizException(5001, "AI 服务调用失败: " + errors.get(0).getMessage());
        }
        return parseQuickReplyList(full.toString(), persona, scene);
    }

    public void streamChatReply(UserEntity user, PersonaEntity persona, String message, ChatStreamingCallback callback) {
        String apiKey = user.getApiKey() != null && !user.getApiKey().isBlank() ? user.getApiKey() : systemApiKey;
        String modelName = user.getModelName() == null || user.getModelName().isBlank() ? defaultModel : user.getModelName();
        String prompt = """
                你是用户的职场沟通顾问。
                人格风格：%s
                风格描述：%s
                语气特点：%s

                仅输出一条可直接复制发送的回复，不要解释。
                对方消息：%s
                """.formatted(persona.getName(), persona.getDescription(), persona.getTone(), message);

        executor.submit(() -> {
            if (apiKey == null || apiKey.isBlank()) {
                String fallback = "收到，这件事我会尽快跟进，处理后第一时间同步你。";
                callback.onPartial(fallback);
                callback.onComplete(fallback);
                return;
            }
            try {
                OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build();
                StringBuilder full = new StringBuilder();
                model.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        full.append(partialResponse);
                        callback.onPartial(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        callback.onComplete(full.toString());
                    }

                    @Override
                    public void onError(Throwable error) {
                        callback.onError(error);
                    }
                });
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private List<Map<String, Object>> fallbackQuickReplyList(PersonaEntity persona, SceneEntity scene) {
        return List.of(
                Map.of("id", 1, "reply", "收到，你这边的诉求我理解了，我会尽快推进并及时给你反馈。", "style", persona.getName() + "-稳妥正式"),
                Map.of("id", 2, "reply", "明白了，我先把关键点处理掉，稍后把进展同步给你。", "style", persona.getName() + "-简洁高效"),
                Map.of("id", 3, "reply", "辛苦提醒，咱们按这个方向推进，我这边先落地执行，有进度马上回你。", "style", scene.getName() + "-高情商缓冲")
        );
    }

    private List<Map<String, Object>> parseQuickReplyList(String raw, PersonaEntity persona, SceneEntity scene) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
        }

        try {
            Object parsed = objectMapper.readValue(text, Object.class);
            if (parsed instanceof String parsedString) {
                parsed = objectMapper.readValue(parsedString, Object.class);
            }
            if (parsed instanceof List<?> list) {
                return objectMapper.convertValue(list, new TypeReference<List<Map<String, Object>>>() {});
            }
            if (parsed instanceof Map<?, ?> map && map.get("data") instanceof List<?> dataList) {
                return objectMapper.convertValue(dataList, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception ignored) {
        }

        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start >= 0 && end > start) {
            String candidate = text.substring(start, end + 1);
            try {
                return objectMapper.readValue(candidate, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }

        return fallbackQuickReplyList(persona, scene);
    }

    public interface ChatStreamingCallback {
        void onPartial(String text);

        void onComplete(String fullText);

        void onError(Throwable error);
    }
}
