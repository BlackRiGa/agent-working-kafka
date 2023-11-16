package efko.com.consumerkafkalistener.sender;

import efko.com.consumerkafkalistener.utils.OperationResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
public class RequestSender {
    private static final Logger log = LoggerFactory.getLogger(RequestSender.class);
    @Value("${spring.application.url}")
    private String urlService;

    public OperationResult sendMessage(String key, String value, String xDebugTag) {
        if (urlService == null || urlService.isEmpty()) {
            log.error("Invalid URL in properties");
            return new OperationResult(false, "Invalid URL");
        }
        String requestBody = addKeyInJSON(key, value);
        return makeHttpRequest(xDebugTag, requestBody);
    }

    private OperationResult makeHttpRequest(String xDebugTag, String requestBody) {
        try {
            RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("x_debug_tag", xDebugTag);
            Charset customCharset = StandardCharsets.UTF_8;
            headers.setAcceptCharset(Collections.singletonList(customCharset));

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(urlService, HttpMethod.POST, request, String.class);

            log.info("Response Code: " + response.getStatusCodeValue());
            return new OperationResult(true, "Success message: " + response.toString().replace("\n", ""));
        } catch (HttpClientErrorException e) {
            log.error("HTTP client error: " + e.getStatusCode() + ", " + e.getStatusText());
            return new OperationResult(false, "HTTP client error: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("Rest client error: " + e.getMessage().replace("\n", ""));
            return new OperationResult(false, "Exception occurred");
        }
    }

    private String addKeyInJSON(String key, String value) {
        try {
            JSONArray jsonArray = new JSONArray(value);
            JSONObject firstObject = jsonArray.getJSONObject(0);
            firstObject.put("key", key);
            log.info("Added key values to JSON in method addKeyInJSON: " + jsonArray);
            return jsonArray.toString();
        } catch (Exception e) {
            log.error("Problem adding the key to JSON in addKeyInJSON, key = " + key, e);
            return value;
        }
    }
}

