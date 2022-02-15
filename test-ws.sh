curl    --include \
        --no-buffer \
        --header "Connection: Upgrade" \
        --header "Upgrade: websocket" \
        --header "Host: localhost:3000" \
        --cookie "ring-session=863adeba-aa54-4f9a-8f5d-b8570f0067b6;JSESSIONID=ebDZS9PdLlXjKENhvQUC0lzmctJka4FuvjB32eQi" \
        http://localhost:3000/ws/1