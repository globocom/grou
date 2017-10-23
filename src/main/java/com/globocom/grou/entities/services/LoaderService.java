package com.globocom.grou.entities.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.entities.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LoaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public LoaderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<Loader> loaders() {
        return redisTemplate.keys("grou:loader:*").stream().map(k -> {
            try {
                String loaderStr = redisTemplate.opsForValue().get(k);
                final Loader loader = mapper.readValue(loaderStr, Loader.class);
                if (!loader.getStatusDetailed().isEmpty() && loader.getStatus() == Loader.Status.RUNNING) {
                    String statusDetailed = "Running test " + loader.getStatusDetailed();
                    loader.setStatusDetailed(statusDetailed);
                }
                return loader;
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

}