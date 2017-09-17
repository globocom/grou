package com.globocom.grou.entities;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@Document
public class Loader implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private ObjectId id;

    private String url;

    public Loader() {
        this("UNDEF");
    }

    public Loader(String url) {
        this.url = url;
    }

    public ObjectId getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}
