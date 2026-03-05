import { normalizeError, request } from "./api.js";

export function createSettingsModule(state) {
    const nicknameInput = document.getElementById("settings-nickname");
    const defaultPersonaSelect = document.getElementById("settings-default-persona");
    const usageBox = document.getElementById("settings-usage");
    const saveProfileBtn = document.getElementById("settings-save-profile");
    const apiKeyInput = document.getElementById("settings-api-key");
    const modelSelect = document.getElementById("settings-model-name");
    const saveApiBtn = document.getElementById("settings-save-api");
    const clearApiBtn = document.getElementById("settings-clear-api");
    const personaNameInput = document.getElementById("persona-name");
    const personaDescriptionInput = document.getElementById("persona-description");
    const personaToneInput = document.getElementById("persona-tone");
    const personaCreateBtn = document.getElementById("persona-create-btn");
    const personaListEl = document.getElementById("persona-list");

    function renderDefaultPersonas() {
        defaultPersonaSelect.innerHTML = state.personas
            .map(item => `<option value="${item.id}">${item.name}</option>`)
            .join("");
    }

    function renderUsage() {
        const usage = state.usage;
        if (!usage) {
            usageBox.textContent = "暂无用量信息";
            return;
        }
        usageBox.textContent = usage.unlimited
            ? `${usage.date} 今日用量：${usage.used}（已配置个人 API Key，不限次）`
            : `${usage.date} 今日用量：${usage.used}/${usage.limit}`;
    }

    function renderPersonaList() {
        personaListEl.innerHTML = state.personas.map(item => `
            <article class="rounded-xl border border-rose-200 bg-white p-3">
                <div class="flex items-center justify-between gap-3">
                    <div>
                        <p class="font-medium">${item.name} ${item.isSystem ? "<span class='text-xs text-rose-700'>(系统)</span>" : ""}</p>
                        <p class="text-xs text-rose-700">${item.description}</p>
                        <p class="mt-1 text-xs text-rose-600">语气：${item.tone}</p>
                    </div>
                    ${item.isSystem ? "" : `<div class="flex gap-2">
                        <button class="secondary-btn persona-edit-btn" data-id="${item.id}">编辑</button>
                        <button class="secondary-btn persona-delete-btn" data-id="${item.id}">删除</button>
                    </div>`}
                </div>
            </article>
        `).join("");

        personaListEl.querySelectorAll(".persona-delete-btn").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    await request(`/personas/${btn.dataset.id}`, { method: "DELETE" });
                    await refreshPersonas();
                } catch (error) {
                    alert(normalizeError(error));
                }
            });
        });

        personaListEl.querySelectorAll(".persona-edit-btn").forEach(btn => {
            btn.addEventListener("click", async () => {
                const id = Number(btn.dataset.id);
                const target = state.personas.find(item => item.id === id);
                if (!target) {
                    return;
                }
                const name = prompt("人格名称", target.name);
                if (name === null) return;
                const description = prompt("风格描述", target.description);
                if (description === null) return;
                const tone = prompt("语气特点", target.tone);
                if (tone === null) return;
                try {
                    await request(`/personas/${id}`, {
                        method: "PUT",
                        body: JSON.stringify({ name, description, tone })
                    });
                    await refreshPersonas();
                } catch (error) {
                    alert(normalizeError(error));
                }
            });
        });
    }

    async function refreshPersonas() {
        state.personas = await request("/personas");
        renderDefaultPersonas();
        renderPersonaList();
    }

    async function loadUsage() {
        state.usage = await request("/user/usage");
        renderUsage();
    }

    function fillProfile() {
        nicknameInput.value = state.user.nickname || "";
        defaultPersonaSelect.value = String(state.user.defaultPersonaId || "");
        modelSelect.value = state.user.modelName || "qwen-plus";
    }

    function bindEvents() {
        saveProfileBtn.addEventListener("click", async () => {
            try {
                await request("/user/profile", {
                    method: "PUT",
                    body: JSON.stringify({
                        nickname: nicknameInput.value.trim(),
                        defaultPersonaId: Number(defaultPersonaSelect.value)
                    })
                });
                alert("资料已保存");
            } catch (error) {
                alert(normalizeError(error));
            }
        });

        saveApiBtn.addEventListener("click", async () => {
            try {
                await request("/user/api-key", {
                    method: "PUT",
                    body: JSON.stringify({
                        apiKey: apiKeyInput.value.trim(),
                        modelName: modelSelect.value
                    })
                });
                await loadUsage();
                alert("API Key 已保存");
            } catch (error) {
                alert(normalizeError(error));
            }
        });

        clearApiBtn.addEventListener("click", async () => {
            try {
                await request("/user/api-key", { method: "DELETE" });
                apiKeyInput.value = "";
                await loadUsage();
                alert("API Key 已清除");
            } catch (error) {
                alert(normalizeError(error));
            }
        });

        personaCreateBtn.addEventListener("click", async () => {
            try {
                await request("/personas", {
                    method: "POST",
                    body: JSON.stringify({
                        name: personaNameInput.value.trim(),
                        description: personaDescriptionInput.value.trim(),
                        tone: personaToneInput.value.trim()
                    })
                });
                personaNameInput.value = "";
                personaDescriptionInput.value = "";
                personaToneInput.value = "";
                await refreshPersonas();
            } catch (error) {
                alert(normalizeError(error));
            }
        });
    }

    return {
        init: async () => {
            await refreshPersonas();
            await loadUsage();
            fillProfile();
            bindEvents();
        }
    };
}
