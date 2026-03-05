import { logout, fetchMe, requireAuth } from "./auth.js";
import { getToken, normalizeError, request } from "./api.js";
import { Client } from "https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.2.0/+esm";

const { createApp, ref, reactive, computed, onMounted, watch } = Vue;

if (!requireAuth()) {
    throw new Error("未登录");
}

function decodeSseChunk(chunk) {
    return chunk
        .split("\n")
        .filter(line => line.startsWith("data:"))
        .map(line => line.slice(5).trimStart())
        .join("");
}

createApp({
    setup() {
        const tab = ref("quick");
        const user = reactive({});
        const personas = ref([]);
        const scenes = ref([]);
        const usage = ref(null);

        const quick = reactive({
            personaId: null,
            sceneId: null,
            message: "",
            streamText: "",
            replies: [],
            status: ""
        });

        const chat = reactive({
            createPersonaId: null,
            createTitle: "",
            sessions: [],
            activeSessionId: null,
            messages: [],
            input: "",
            connectionState: "未连接"
        });

        const settings = reactive({
            nickname: "",
            defaultPersonaId: null,
            apiKey: "",
            modelName: "qwen-plus",
            newPersona: { name: "", description: "", tone: "" }
        });

        const usageText = computed(() => {
            if (!usage.value) return "暂无用量信息";
            if (usage.value.unlimited) {
                return `${usage.value.date} 今日用量：${usage.value.used}（已配置个人 API Key，不限次）`;
            }
            return `${usage.value.date} 今日用量：${usage.value.used}/${usage.value.limit}`;
        });

        const activeSessionTitle = computed(() => {
            const found = chat.sessions.find(s => s.id === chat.activeSessionId);
            return found?.title || "请选择会话";
        });

        let stompClient = null;
        let subscription = null;
        let streamingAiIndex = -1;

        async function loadBootstrapData() {
            const me = await fetchMe();
            Object.assign(user, me);
            settings.nickname = me.nickname || "";
            settings.defaultPersonaId = me.defaultPersonaId || null;
            settings.modelName = me.modelName || "qwen-plus";

            personas.value = await request("/personas");
            scenes.value = await request("/scenes");
            usage.value = await request("/user/usage");
            chat.sessions = await request("/chat/sessions");

            if (personas.value.length > 0) {
                quick.personaId = quick.personaId ?? personas.value[0].id;
                chat.createPersonaId = chat.createPersonaId ?? personas.value[0].id;
                settings.defaultPersonaId = settings.defaultPersonaId ?? personas.value[0].id;
            }
            if (scenes.value.length > 0) {
                quick.sceneId = quick.sceneId ?? scenes.value[0].id;
            }
        }

        async function generateQuickReplies() {
            if (!quick.message.trim()) {
                quick.status = "请先输入对方消息";
                return;
            }
            quick.streamText = "";
            quick.replies = [];
            quick.status = "生成中...";
            try {
                const response = await fetch("/api/quick-reply/stream", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "text/event-stream",
                        "Authorization": `Bearer ${getToken()}`
                    },
                    body: JSON.stringify({
                        message: quick.message.trim(),
                        personaId: Number(quick.personaId),
                        sceneId: Number(quick.sceneId)
                    })
                });
                if (!response.ok || !response.body) {
                    throw new Error("快速回复流式请求失败");
                }
                const reader = response.body.getReader();
                const decoder = new TextDecoder("utf-8");
                let pending = "";
                let fullText = "";
                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;
                    pending += decoder.decode(value, { stream: true });
                    const events = pending.split("\n\n");
                    pending = events.pop() || "";
                    for (const event of events) {
                        const token = decodeSseChunk(event);
                        if (token) {
                            fullText += token;
                            quick.streamText = fullText;
                        }
                    }
                }
                const start = fullText.indexOf("[");
                const end = fullText.lastIndexOf("]");
                if (start < 0 || end <= start) {
                    throw new Error("AI 输出格式错误");
                }
                quick.replies = JSON.parse(fullText.slice(start, end + 1));
                quick.status = "生成完成";
            } catch (error) {
                quick.status = normalizeError(error);
            }
        }

        async function copyText(text) {
            if (!text) return;
            await navigator.clipboard.writeText(text);
        }

        async function createSession() {
            await request("/chat/sessions", {
                method: "POST",
                body: JSON.stringify({
                    personaId: Number(chat.createPersonaId),
                    title: chat.createTitle.trim()
                })
            });
            chat.createTitle = "";
            chat.sessions = await request("/chat/sessions");
        }

        async function openSession(sessionId) {
            chat.activeSessionId = sessionId;
            chat.messages = await request(`/chat/sessions/${sessionId}/messages`);
            subscribeSession();
        }

        async function deleteSession(sessionId) {
            await request(`/chat/sessions/${sessionId}`, { method: "DELETE" });
            if (chat.activeSessionId === sessionId) {
                chat.activeSessionId = null;
                chat.messages = [];
            }
            chat.sessions = await request("/chat/sessions");
        }

        function subscribeSession() {
            if (!stompClient?.connected || !chat.activeSessionId) return;
            if (subscription) subscription.unsubscribe();
            subscription = stompClient.subscribe(`/user/queue/chat/${chat.activeSessionId}`, message => {
                const payload = JSON.parse(message.body);
                if (payload.type === "token") {
                    if (streamingAiIndex === -1) {
                        chat.messages.push({ role: "ASSISTANT", content: "" });
                        streamingAiIndex = chat.messages.length - 1;
                    }
                    chat.messages[streamingAiIndex].content += payload.data || "";
                } else if (payload.type === "complete") {
                    if (streamingAiIndex >= 0) {
                        chat.messages[streamingAiIndex].content = payload.data || chat.messages[streamingAiIndex].content;
                    } else {
                        chat.messages.push({ role: "ASSISTANT", content: payload.data || "" });
                    }
                    streamingAiIndex = -1;
                } else if (payload.type === "error") {
                    chat.messages.push({ role: "ASSISTANT", content: `错误：${payload.data || "未知错误"}` });
                    streamingAiIndex = -1;
                }
            });
        }

        function connectStomp() {
            const socketUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws/chat`;
            stompClient = new Client({
                brokerURL: socketUrl,
                connectHeaders: { Authorization: `Bearer ${getToken()}` },
                reconnectDelay: 2000,
                onConnect: () => {
                    chat.connectionState = "已连接";
                    subscribeSession();
                },
                onWebSocketClose: () => {
                    chat.connectionState = "连接中断，重连中...";
                },
                onStompError: (frame) => {
                    chat.connectionState = `连接异常: ${frame.headers.message || "unknown"}`;
                }
            });
            chat.connectionState = "连接中...";
            stompClient.activate();
        }

        function sendChat() {
            if (!stompClient?.connected) {
                throw new Error("WebSocket 未连接");
            }
            if (!chat.activeSessionId) {
                throw new Error("请先选择会话");
            }
            const text = chat.input.trim();
            if (!text) return;
            chat.messages.push({ role: "USER", content: text });
            chat.input = "";
            streamingAiIndex = -1;
            stompClient.publish({
                destination: "/app/chat.send",
                body: JSON.stringify({ sessionId: chat.activeSessionId, message: text })
            });
        }

        async function saveProfile() {
            await request("/user/profile", {
                method: "PUT",
                body: JSON.stringify({
                    nickname: settings.nickname.trim(),
                    defaultPersonaId: Number(settings.defaultPersonaId)
                })
            });
            user.nickname = settings.nickname.trim();
            alert("资料已保存");
        }

        async function saveApiKey() {
            await request("/user/api-key", {
                method: "PUT",
                body: JSON.stringify({
                    apiKey: settings.apiKey.trim(),
                    modelName: settings.modelName
                })
            });
            usage.value = await request("/user/usage");
            alert("API Key 已保存");
        }

        async function clearApiKey() {
            await request("/user/api-key", { method: "DELETE" });
            settings.apiKey = "";
            usage.value = await request("/user/usage");
            alert("API Key 已清除");
        }

        async function createPersona() {
            await request("/personas", {
                method: "POST",
                body: JSON.stringify(settings.newPersona)
            });
            settings.newPersona = { name: "", description: "", tone: "" };
            personas.value = await request("/personas");
        }

        async function editPersona(persona) {
            const name = window.prompt("人格名称", persona.name);
            if (name === null) return;
            const description = window.prompt("风格描述", persona.description);
            if (description === null) return;
            const tone = window.prompt("语气特点", persona.tone);
            if (tone === null) return;
            await request(`/personas/${persona.id}`, {
                method: "PUT",
                body: JSON.stringify({ name, description, tone })
            });
            personas.value = await request("/personas");
        }

        async function deletePersona(id) {
            await request(`/personas/${id}`, { method: "DELETE" });
            personas.value = await request("/personas");
        }

        async function handleLogout() {
            await logout();
            window.location.href = "/index.html";
        }

        watch(() => chat.activeSessionId, () => {
            subscribeSession();
        });

        onMounted(async () => {
            try {
                await loadBootstrapData();
                connectStomp();
            } catch (error) {
                alert(normalizeError(error));
                window.location.href = "/index.html";
            }
        });

        return {
            tab,
            user,
            personas,
            scenes,
            usageText,
            quick,
            chat,
            settings,
            activeSessionTitle,
            generateQuickReplies,
            copyText,
            createSession,
            openSession,
            deleteSession,
            sendChat,
            saveProfile,
            saveApiKey,
            clearApiKey,
            createPersona,
            editPersona,
            deletePersona,
            handleLogout
        };
    }
}).mount("#app");
