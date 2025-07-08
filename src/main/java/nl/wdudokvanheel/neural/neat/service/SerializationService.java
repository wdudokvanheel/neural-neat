package nl.wdudokvanheel.neural.neat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationService {
    private final Logger logger = LoggerFactory.getLogger(SerializationService.class);

    private final ObjectMapper objectMapper;

    public SerializationService() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        objectMapper.enableDefaultTyping();
    }

    public String serialize(Genome genome) {
        try {
            return objectMapper.writeValueAsString(genome);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return "";
        }
    }

    public Genome deserialize(String json) {
        try {
            return objectMapper.readValue(json, Genome.class);
        } catch (JsonProcessingException e) {
            logger.info("Failed to deserialize: {}", e);
            return null;
        }
    }
}
