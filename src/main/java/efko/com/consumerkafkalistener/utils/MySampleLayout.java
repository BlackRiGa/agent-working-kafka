package efko.com.consumerkafkalistener.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class MySampleLayout extends LayoutBase<ILoggingEvent> {
    public static String TYPE_SERVER_NAME = "CONSUMERKAFKALISTENER";

    public String doLayout(ILoggingEvent event) {
        LogData logData = new LogData();

        logData.setIndex(MySampleLayout.TYPE_SERVER_NAME);
        logData.setCategory(event.getLevel().toString());
        logData.setContext(event.getLoggerName());

        String level = event.getLevel().toString();
        int levelValue = switch (level) {
            case "OFF" -> 0;
            case "ERROR" -> 1;
            case "WARN" -> 2;
            case "INFO" -> 3;
            case "DEBUG" -> 4;
            case "TRACE" -> 5;
            case "ALL" -> 6;
            default -> throw new IllegalStateException("Unexpected value: " + level);
        };

        logData.setLevel(levelValue);

        logData.setLevel_name(event.getLevel().toString());
        logData.setAction(event.getThreadName());
        logData.setAction_type("web");


        LocalDateTime dateTimeFormat = LocalDateTime.now();
        String formattedDateTime = dateTimeFormat.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")).concat("+03:00");
        logData.setDatetime(formattedDateTime);

        String timeStampDate = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")).concat("+03:00");
        logData.setTimestamp(timeStampDate);

        logData.setUserId(event.getLoggerContextVO().getName());
        logData.setIp("ip");
        logData.setReal_ip("real_ip");
        logData.setX_debug_tag("x_debug_tag");
        logData.setMessage(event.getFormattedMessage());

        if (Objects.equals(event.getLevel().toString(), "WARN") || Objects.equals(event.getLevel().toString(), "ERROR")) {
            ExceptionInfo exceptionInfo = new ExceptionInfo();
            exceptionInfo.setFile(event.getLoggerName());
            exceptionInfo.setLine("null");
            exceptionInfo.setCode(event.getLevel().levelInt);
            exceptionInfo.getTrace().add(event.getFormattedMessage());
            logData.setException(exceptionInfo);
        } else {
            logData.setException(null);
        }

        logData.setExtras(null);


        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(logData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString + "\n";
    }
}

