-- Выполнять вручную после шагов 1, 3, 4 в requests.http.
-- На чистой таблице: ACTIVE; затем USED + ACTIVE; затем REVOKED + ACTIVE.
SELECT id, username, device_id, status, access_token_expiry, refresh_token_expiry
FROM user_sessions
ORDER BY refresh_token_expiry, id;

SELECT count(*) AS session_count FROM user_sessions;
