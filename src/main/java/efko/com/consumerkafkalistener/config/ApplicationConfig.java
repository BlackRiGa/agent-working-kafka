package efko.com.consumerkafkalistener.config;

import efko.com.consumerkafkalistener.sender.RequestSender;
import efko.com.consumerkafkalistener.utils.OperationResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.handler.annotation.Header;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class ApplicationConfig {
    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    public final String topicName;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServersConfig;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupID;
    @Value("${spring.kafka.consumer.pollTimeout}")
    private Integer pollTimeout;

    public ApplicationConfig(@Value("${spring.kafka.topic}") String topicName) {
        this.topicName = topicName;
    }

    @Bean
    public ConsumerFactory<String, String> productConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productConsumerFactory());
        factory.getContainerProperties().setPollTimeout(pollTimeout);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public KafkaClient stringValueConsumer(RequestSender request) {
        return new KafkaClient(request);
    }

    public static class KafkaClient {
        private final RequestSender requestSender;
        private static final int MAX_RETRIES = 3;
        private static final long RETRY_INTERVAL_MS = 5000;

        public KafkaClient(RequestSender requestSender) {
            this.requestSender = requestSender;
        }

        @KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
        public void consume(
                ConsumerRecord<String, String> record,
                Acknowledgment acknowledgment,
                @Header(value = "x_debug_tag", required = false) String xDebugTag
        ) throws InterruptedException {
            String key = record.key();
            String value = record.value();
            log.info(String.format("x_debug_tag -> " + xDebugTag + ", key -> " + key + ", value -> " + value.replace("\n", "")));

            int retry = 0;
            while (retry <= MAX_RETRIES) {
                try {
                    OperationResult result = requestSender.sendMessage(key, value, xDebugTag);

                    if (result.getMessage().contains("Exception") || result.getMessage().contains("Error")) {
                        acknowledgment.acknowledge();
                        log.error("Error processing package in consume from kafka and send to consumer: " + result.getMessage());
                    } else {
                        log.info("Package successfully processed: " + result.getMessage());
                        break;
                    }
                } catch (Exception e) {
                    log.error("Error processing the package: " + e.getMessage(), e);
                }
                retry++;
                Thread.sleep(RETRY_INTERVAL_MS);
            }

            if (retry == MAX_RETRIES) {
                log.error("Max retry count reached for processing package: " + key);
            }
        }
    }
}
