package com.globocom.grou.entities.services;

import com.globocom.grou.entities.Loader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoaderService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public LoaderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<Loader> loaders() {
        return redisTemplate.keys("grou:loader:*").stream().map(k -> {
            String loaderName = k.split(":")[2];
            String loaderStatusStr = redisTemplate.opsForValue().get(k);
            int idx;
            String statusDetailed = "";
            if ((idx = loaderStatusStr.indexOf(":")) > -1) {
                statusDetailed = "Running test " + loaderStatusStr.substring(idx + 1);
                loaderStatusStr = loaderStatusStr.substring(0, idx);
            }
            Loader loader = new Loader();
            loader.setName(loaderName);
            loader.setStatus(Enum.valueOf(Loader.Status.class, loaderStatusStr));
            loader.setStatusDetailed(statusDetailed);
            return loader;
        }).collect(Collectors.toList());
    }

}