const TOKEN_KEY = "speak_for_you_token";

export function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
}

export async function request(path, options = {}, auth = true) {
    const headers = new Headers(options.headers || {});
    headers.set("Content-Type", "application/json");
    if (auth) {
        const token = getToken();
        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }
    }
    const response = await fetch(`/api${path}`, { ...options, headers });
    const data = await response.json();
    if (data.code !== 0) {
        throw new Error(data.message || "请求失败");
    }
    return data.data;
}

export function normalizeError(error) {
    return error?.message || "系统错误，请稍后重试";
}
