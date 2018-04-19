package com.globocom.grou.entities;

import java.util.ArrayList;
import java.util.List;

public class GroupLoader {
    private String name = "default";
    private List<Loader> loaders = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Loader> getLoaders() {
        return loaders;
    }

    public void setLoaders(List<Loader> loaders) {
        this.loaders = loaders;
    }
}
