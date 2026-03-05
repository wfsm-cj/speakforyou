import { login } from "./auth.js";
import { normalizeError } from "./api.js";

const form = document.getElementById("login-form");
const errorBox = document.getElementById("login-error");

form?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(form);
    const username = String(formData.get("username") || "").trim();
    const password = String(formData.get("password") || "").trim();
    errorBox.classList.add("hidden");
    try {
        await login(username, password);
        window.location.href = "/app.html";
    } catch (error) {
        errorBox.textContent = normalizeError(error);
        errorBox.classList.remove("hidden");
    }
});
