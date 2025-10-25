package g3pjt.service.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions =
            Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("Connected: {}", session.getId());
        broadcast("[입장]" + session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                     @NonNull TextMessage message) throws Exception{
        final String payload = message.getPayload();
        log.info("Message from {}: {}", session.getId(), payload);
        broadcast(session.getId() + ": " + payload);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session,
                                     @NonNull Throwable exception)throws Exception {
        log.warn("Transport error from {}: {}", session.getId(), exception.getMessage() );
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                      @NonNull CloseStatus status)throws Exception {
        sessions.remove(session);
        log.info("Disconnected: {} ({})", session.getId(), status);
        broadcast("[퇴장]" + session.getId());
    }

    private void broadcast(String msg){
        synchronized (sessions){
            for (WebSocketSession sess : sessions){
                if (sess.isOpen()){
                    try{
                        sess.sendMessage(new TextMessage(msg));
                    }catch (IOException e){
                        log.error("send error to {}: {}", sess.getId(), e.getMessage());
                    }
                }
            }
        }
    }
}
