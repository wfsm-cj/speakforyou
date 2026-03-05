import { clearToken, getToken, request, setToken } from "./api.js";

export async function login(username, password) {
    const data = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
    }, false);
    setToken(data.token);
    return data;
}

export async function fetchMe() {
    return request("/auth/me");
}

export async function logout() {
    try {
        await request("/auth/logout", { method: "POST" });
    } finally {
        clearToken();
    }
}

export function requireAuth() {
    if (!getToken()) {
        window.location.href = "/index.html";
        return false;
    }
    return true;
}
