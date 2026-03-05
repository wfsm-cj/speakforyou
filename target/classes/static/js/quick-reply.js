import { getToken, normalizeError, request } from "./api.js";

function renderOptions(selectEl, items) {
    selectEl.innerHTML = items.map(item => `<option value="${item.id}">${item.name}</option>`).join("");
}

function extractJsonArray(raw) {
    const start = raw.indexOf("[");
    const end = raw.lastIndexOf("]");
    if (start < 0 || end <= start) {
        throw new Error("AI 输出格式错误，未解析到 JSON 数组");
    }
    return JSON.parse(raw.slice(start, end + 1));
}

function decodeSseChunk(chunk) {
    return chunk
        .split("\n")
        .filter(line => line.startsWith("data:"))
        .map(line => line.slice(5).trimStart())
        .join("");
}

export function createQuickReplyModule(state) {
    const personaSelect = document.getElementById("quick-persona");
    const sceneSelect = document.getElementById("quick-scene");
    const messageInput = document.getElementById("quick-message");
    const generateBtn = document.getElementById("quick-generate-btn");
    const statusText = document.getElementById("quick-status");
    const streamBox = document.getElementById("quick-stream");
    const repliesBox = document.getElementById("quick-replies");

    async function loadBaseData() {
        const [personas, scenes] = await Promise.all([
            request("/personas"),
            request("/scenes")
        ]);
        state.personas = personas;
        state.scenes = scenes;
        renderOptions(personaSelect, personas);
        renderOptions(sceneSelect, scenes);
    }

    function renderReplies(replies) {
        repliesBox.innerHTML = replies.map(item => `
            <article class="card space-y-2">
                <p class="text-xs text-rose-700">${item.style || "建议"}</p>
                <p class="text-sm leading-6">${item.reply || ""}</p>
                <button class="secondary-btn copy-reply-btn">复制</button>
            </article>
        `).join("");
        repliesBox.querySelectorAll(".copy-reply-btn").forEach((btn, index) => {
            btn.addEventListener("click", async () => {
                const content = replies[index]?.reply || "";
                await navigator.clipboard.writeText(content);
                btn.textContent = "已复制";
                setTimeout(() => {
                    btn.textContent = "复制";
                }, 1200);
            });
        });
    }

    async function generate() {
        const message = messageInput.value.trim();
        if (!message) {
            statusText.textContent = "请先输入对方消息";
            return;
        }
        streamBox.textContent = "";
        repliesBox.innerHTML = "";
        statusText.textContent = "生成中...";
        generateBtn.disabled = true;
        try {
            const response = await fetch("/api/quick-reply/stream", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "text/event-stream",
                    "Authorization": `Bearer ${getToken()}`
                },
                body: JSON.stringify({
                    message,
                    personaId: Number(personaSelect.value),
                    sceneId: Number(sceneSelect.value)
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
                if (done) {
                    break;
                }
                pending += decoder.decode(value, { stream: true });
                const events = pending.split("\n\n");
                pending = events.pop() || "";
                for (const event of events) {
                    const token = decodeSseChunk(event);
                    if (token) {
                        fullText += token;
                        streamBox.textContent = fullText;
                    }
                }
            }
            if (pending) {
                const token = decodeSseChunk(pending);
                if (token) {
                    fullText += token;
                    streamBox.textContent = fullText;
                }
            }
            const replies = extractJsonArray(fullText);
            renderReplies(replies);
            statusText.textContent = "生成完成";
        } catch (error) {
            statusText.textContent = normalizeError(error);
        } finally {
            generateBtn.disabled = false;
        }
    }

    function bindEvents() {
        generateBtn.addEventListener("click", generate);
    }

    return {
        init: async () => {
            await loadBaseData();
            bindEvents();
        }
    };
}
