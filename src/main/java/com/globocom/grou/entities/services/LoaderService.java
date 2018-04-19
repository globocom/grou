package com.globocom.grou.entities.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globocom.grou.entities.GroupLoader;
import com.globocom.grou.entities.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class LoaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public LoaderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<GroupLoader> loaders() {

        final Map<String, GroupLoader> groupLoaderMap = new HashMap<>();
        redisTemplate.keys("grou:loader:*").forEach(k -> {
            try {
                String loaderStr = redisTemplate.opsForValue().get(k);
                final Loader loader = mapper.readValue(loaderStr, Loader.class);
                if (!loader.getStatusDetailed().isEmpty() && loader.getStatus() == Loader.Status.RUNNING) {
                    String statusDetailed = "Running test " + loader.getStatusDetailed();
                    loader.setStatusDetailed(statusDetailed);
                }
                String groupName = loader.getGroupName();
                groupName = groupName == null ? "default" : groupName;
                GroupLoader groupLoader = groupLoaderMap.computeIfAbsent(groupName, GroupLoader::new);
                groupLoader.getLoaders().add(loader);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        return new ArrayList<>(groupLoaderMap.values());
    }

}