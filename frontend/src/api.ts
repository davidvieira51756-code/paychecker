import type { ApiErrorResponse } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function apiRequest<T>(
    path: string,
    options: RequestInit = {},
    token?: string
): Promise<T> {
    const headers: Record<string, string> = {
        "Content-Type": "application/json",
    };

    if (options.headers) {
        Object.assign(headers, options.headers);
    }

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;

        try {
            const error = (await response.json()) as ApiErrorResponse;
            message = error.message ?? message;
        } catch {
            // Keep default message if response body is empty.
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}