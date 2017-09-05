package com.globocom.grou.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
public class RootController {

    @Value("${build.project")
    private String buildProject;

    @Value("${build.version}")
    private String buildVersion;

    @Value("${build.timestamp}")
    private String buildTimestamp;

    @GetMapping("/version")
    public String get() {
        return String.format("{\"name\":\"%s\", \"version\":\"%s\", \"build\":\"%s\"}", buildProject, buildVersion, buildTimestamp);
    }

}
