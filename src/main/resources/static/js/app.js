import { logout, fetchMe, requireAuth } from "./auth.js";
import { normalizeError, request } from "./api.js";
import { createQuickReplyModule } from "./quick-reply.js";
import { createChatModule } from "./chat.js";
import { createSettingsModule } from "./settings.js";

if (!requireAuth()) {
    throw new Error("未登录");
}

const state = {
    user: null,
    personas: [],
    scenes: [],
    sessions: [],
    usage: null
};

const tabButtons = Array.from(document.querySelectorAll(".tab-btn"));
const tabPanels = Array.from(document.querySelectorAll(".tab-panel"));
const nicknameDisplay = document.getElementById("nickname-display");
const logoutBtn = document.getElementById("logout-btn");

function switchTab(tabName) {
    tabButtons.forEach(btn => {
        const active = btn.dataset.tab === tabName;
        btn.classList.toggle("tab-active", active);
    });
    tabPanels.forEach(panel => {
        const active = panel.id === `tab-${tabName}`;
        panel.classList.toggle("hidden", !active);
    });
}

async function bootstrap() {
    try {
        state.user = await fetchMe();
        nicknameDisplay.textContent = `你好，${state.user.nickname || state.user.username}`;
        state.personas = await request("/personas");
        state.scenes = await request("/scenes");

        const quickReply = createQuickReplyModule(state);
        const chat = createChatModule(state);
        const settings = createSettingsModule(state);

        await quickReply.init();
        await chat.init();
        await settings.init();

        tabButtons.forEach(btn => {
            btn.addEventListener("click", () => switchTab(btn.dataset.tab));
        });
        logoutBtn.addEventListener("click", async () => {
            await logout();
            window.location.href = "/index.html";
        });
    } catch (error) {
        alert(normalizeError(error));
        window.location.href = "/index.html";
    }
}

bootstrap();
