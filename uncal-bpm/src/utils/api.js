const BASE_URL = "http://localhost:8080/api";

/**
 * Custom fetcher untuk menangani otorisasi (Bearer Token).
 */
export const authorizedFetch = async (endpoint, options = {}, token) => {
    
    if (!token) {
        throw new Error("Missing authentication token for authorized request.");
    }
    
    const url = `${BASE_URL}${endpoint}`;

    const defaultHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
    };

    try {
        const res = await fetch(url, config);

        // Handle 401/403 - Authentication errors
        if (res.status === 401 || res.status === 403) {
            const errorMsg = await res.text();
            const error = new Error(errorMsg || `Authentication failed with status ${res.status}.`);
            error.status = res.status;
            throw error;
        }

        // Handle 404 - Not Found
        if (res.status === 404) {
            // Untuk kasus project tidak ditemukan, return empty array daripada throw error
            if (endpoint.includes('/projects/') && endpoint.includes('/files/')) {
                console.warn(`⚠️ 404 - Endpoint not found: ${endpoint}, returning empty array`);
                return [];
            }
            
            const errorMsg = await res.text();
            const error = new Error(errorMsg || `Resource not found: ${endpoint}`);
            error.status = res.status;
            throw error;
        }

        if (!res.ok) {
            // Tangani error umum (400, 500, dll)
            let errorMsg = `HTTP error! Status: ${res.status}`;
            try {
                const errorData = await res.json();
                errorMsg = errorData.message || JSON.stringify(errorData);
            } catch (e) {
                errorMsg = await res.text();
            }
            const error = new Error(errorMsg);
            error.status = res.status;
            throw error;
        }

        // Cek apakah ada konten, hindari JSON parse error untuk 204 No Content
        if (res.status === 204 || res.headers.get('content-length') === '0') {
            return null; 
        }

        return res.json();
    } catch (error) {
        console.error(`❌ API Error for ${endpoint}:`, error);
        throw error;
    }
};

/**
 * Export objek API agar mudah diakses di komponen lain
 */
const api = {
    get: (endpoint, token) => authorizedFetch(endpoint, { method: 'GET' }, token),
    post: (endpoint, body, token) => authorizedFetch(endpoint, { method: 'POST', body: JSON.stringify(body) }, token),
    put: (endpoint, body, token) => authorizedFetch(endpoint, { method: 'PUT', body: JSON.stringify(body) }, token),
    patch: (endpoint, body, token) => authorizedFetch(endpoint, { method: 'PATCH', body: JSON.stringify(body) }, token),
    delete: (endpoint, token) => authorizedFetch(endpoint, { method: 'DELETE' }, token),
};

export default api;