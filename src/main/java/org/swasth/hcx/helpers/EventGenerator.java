package org.swasth.hcx.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.swasth.hcx.dto.Request;
import org.swasth.hcx.utils.JSONUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.swasth.hcx.utils.Constants.*;

@Component
public class EventGenerator {

    @Autowired
    Environment env;
    public String generatePayloadEvent(String mid, Request request) throws JsonProcessingException {
        Map<String,Object> event = new HashMap<>();
        event.put(MID, mid);
        event.put(PAYLOAD, request.getPayload());
        return JSONUtils.serialize(event);
    }

    public String generateMetadataEvent(String mid, String apiAction, Request request) throws Exception {
        Map<String,Object> event = new HashMap<>();
        List<String> protocolHeaders = null;
        if (request.getPayload().containsKey(PAYLOAD)) {
            protocolHeaders = env.getProperty(PROTOCOL_HEADERS_MANDATORY, List.class);
            protocolHeaders.addAll(env.getProperty(PROTOCOL_HEADERS_OPTIONAL, List.class));
            List<String> joseHeaders = env.getProperty(JOSE_HEADERS, List.class);
            Map<String,Object> protectedHeaders = request.getHcxHeaders();
            Map<String,Object> filterJoseHeaders = new HashMap<>();
            Map<String,Object> filterProtocolHeaders = new HashMap<>();
            joseHeaders.forEach(key -> {
                if (protectedHeaders.containsKey(key))
                    filterJoseHeaders.put(key, protectedHeaders.get(key));
            });
            protocolHeaders.forEach(key -> {
                if (protectedHeaders.containsKey(key))
                    filterProtocolHeaders.put(key, protectedHeaders.get(key));
            });
            event.put(MID, mid);
            event.put(ETS, System.currentTimeMillis());
            event.put(ACTION, apiAction);
            event.put(HEADERS, new HashMap<>(){{
                put(JOSE, filterJoseHeaders);
                put(PROTOCOL, filterProtocolHeaders);
            }});
            event.put("status", "request.queued");
        } else {
            if(REDIRECT_STATUS.equalsIgnoreCase(request.getStatus())) {
                protocolHeaders = env.getProperty(REDIRECT_HEADERS_MANDATORY, List.class);
                protocolHeaders.addAll(env.getProperty(REDIRECT_HEADERS_OPTIONAL, List.class));
            }else {
                protocolHeaders = env.getProperty(ERROR_HEADERS_MANDATORY, List.class);
                protocolHeaders.addAll(env.getProperty(ERROR_HEADERS_OPTIONAL, List.class));
            }
            Map<String, Object> protectedHeaders = request.getHcxHeaders();
            Map<String, Object> filterProtocolHeaders = new HashMap<>();
            protocolHeaders.forEach(key -> {
                if (protectedHeaders.containsKey(key))
                    filterProtocolHeaders.put(key, protectedHeaders.get(key));
            });
            event.put(HEADERS, new HashMap<>() {{
                put(PROTOCOL, filterProtocolHeaders);
            }});
            event.put(MID, mid);
            event.put(ETS, System.currentTimeMillis());
            event.put(ACTION, apiAction);
            event.put("status", "request.queued");
        }
        return JSONUtils.serialize(event);
    }
}
