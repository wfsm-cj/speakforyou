import { getToken, normalizeError, request } from "./api.js";

function escapeHtml(text) {
    return text
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}

export function createChatModule(state) {
    const createPersonaSelect = document.getElementById("chat-create-persona");
    const titleInput = document.getElementById("chat-title");
    const createBtn = document.getElementById("chat-create-btn");
    const sessionsEl = document.getElementById("chat-sessions");
    const sessionTitleEl = document.getElementById("chat-session-title");
    const messagesEl = document.getElementById("chat-messages");
    const inputEl = document.getElementById("chat-input");
    const sendBtn = document.getElementById("chat-send-btn");
    const connectionStateEl = document.getElementById("chat-connection-state");

    let client = null;
    let subscription = null;
    let activeSessionId = null;
    let streamingAiBubble = null;

    function renderPersonaOptions() {
        createPersonaSelect.innerHTML = state.personas
            .map(item => `<option value="${item.id}">${item.name}</option>`)
            .join("");
    }

    function renderSessions() {
        sessionsEl.innerHTML = state.sessions.map(item => `
            <div class="session-item ${item.id === activeSessionId ? "active" : ""}" data-id="${item.id}">
                <p class="font-medium text-sm">${escapeHtml(item.title || "新会话")}</p>
                <div class="mt-1 flex items-center justify-between">
                    <span class="text-xs text-rose-700">ID: ${item.id}</span>
                    <button class="text-xs text-red-700 hover:text-red-800 delete-session-btn" data-id="${item.id}">删除</button>
                </div>
            </div>
        `).join("");
        sessionsEl.querySelectorAll(".session-item").forEach(el => {
            el.addEventListener("click", () => openSession(Number(el.dataset.id)));
        });
        sessionsEl.querySelectorAll(".delete-session-btn").forEach(btn => {
            btn.addEventListener("click", async (event) => {
                event.stopPropagation();
                await deleteSession(Number(btn.dataset.id));
            });
        });
    }

    function appendMessage(role, content) {
        const div = document.createElement("div");
        div.className = role === "USER" ? "bubble-user" : "bubble-ai";
        div.textContent = content;
        if (role !== "USER") {
            div.title = "可长按复制";
        }
        messagesEl.appendChild(div);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return div;
    }

    async function connectStomp() {
        if (client?.connected) {
            return;
        }
        const socketUrl = `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws/chat`;
        client = new window.StompJs.Client({
            brokerURL: socketUrl,
            connectHeaders: {
                Authorization: `Bearer ${getToken()}`
            },
            reconnectDelay: 2000,
            onConnect: () => {
                connectionStateEl.textContent = "已连接";
                if (activeSessionId) {
                    subscribeSession(activeSessionId);
                }
            },
            onStompError: (frame) => {
                connectionStateEl.textContent = `连接异常: ${frame.headers.message || "unknown"}`;
            },
            onWebSocketClose: () => {
                connectionStateEl.textContent = "连接中断，重连中...";
            }
        });
        connectionStateEl.textContent = "连接中...";
        client.activate();
    }

    function subscribeSession(sessionId) {
        if (!client?.connected) {
            return;
        }
        if (subscription) {
            subscription.unsubscribe();
            subscription = null;
        }
        const destination = `/user/queue/chat/${sessionId}`;
        subscription = client.subscribe(destination, (message) => {
            const payload = JSON.parse(message.body);
            if (payload.type === "token") {
                if (!streamingAiBubble) {
                    streamingAiBubble = appendMessage("ASSISTANT", "");
                }
                streamingAiBubble.textContent += payload.data || "";
                messagesEl.scrollTop = messagesEl.scrollHeight;
            } else if (payload.type === "complete") {
                if (!streamingAiBubble) {
                    appendMessage("ASSISTANT", payload.data || "");
                } else {
                    streamingAiBubble.textContent = payload.data || streamingAiBubble.textContent;
                    streamingAiBubble = null;
                }
            } else if (payload.type === "error") {
                appendMessage("ASSISTANT", `错误：${payload.data || "未知错误"}`);
                streamingAiBubble = null;
            }
        });
    }

    async function loadSessions() {
        state.sessions = await request("/chat/sessions");
        renderSessions();
    }

    async function loadMessages(sessionId) {
        const messages = await request(`/chat/sessions/${sessionId}/messages`);
        messagesEl.innerHTML = "";
        messages.forEach(item => appendMessage(item.role, item.content));
    }

    async function openSession(sessionId) {
        activeSessionId = sessionId;
        const item = state.sessions.find(s => s.id === sessionId);
        sessionTitleEl.textContent = item?.title || "会话";
        await loadMessages(sessionId);
        renderSessions();
        subscribeSession(sessionId);
    }

    async function createSession() {
        const personaId = Number(createPersonaSelect.value);
        const title = titleInput.value.trim();
        await request("/chat/sessions", {
            method: "POST",
            body: JSON.stringify({ personaId, title })
        });
        titleInput.value = "";
        await loadSessions();
    }

    async function deleteSession(sessionId) {
        await request(`/chat/sessions/${sessionId}`, { method: "DELETE" });
        if (sessionId === activeSessionId) {
            activeSessionId = null;
            messagesEl.innerHTML = "";
            sessionTitleEl.textContent = "请选择会话";
        }
        await loadSessions();
    }

    function sendMessage() {
        if (!client?.connected) {
            throw new Error("WebSocket 未连接");
        }
        if (!activeSessionId) {
            throw new Error("请先选择会话");
        }
        const text = inputEl.value.trim();
        if (!text) {
            return;
        }
        appendMessage("USER", text);
        inputEl.value = "";
        streamingAiBubble = null;
        client.publish({
            destination: "/app/chat.send",
            body: JSON.stringify({
                sessionId: activeSessionId,
                message: text
            })
        });
    }

    function bindEvents() {
        createBtn.addEventListener("click", async () => {
            try {
                await createSession();
            } catch (error) {
                alert(normalizeError(error));
            }
        });
        sendBtn.addEventListener("click", () => {
            try {
                sendMessage();
            } catch (error) {
                alert(normalizeError(error));
            }
        });
        inputEl.addEventListener("keydown", (event) => {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                sendBtn.click();
            }
        });
    }

    return {
        init: async () => {
            renderPersonaOptions();
            await loadSessions();
            await connectStomp();
            bindEvents();
        }
    };
}
