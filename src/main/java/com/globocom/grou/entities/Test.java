/*
 * Copyright (c) 2017-2017 Globo.com
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globocom.grou.entities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@RequiredArgsConstructor
@Document
public class Test implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        SCHEDULED,
        ENQUEUED,
        RUNNING,
        OK,
        ERROR
    }

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String name;

    @DBRef
    @NonNull
    private Project project;

    @NonNull
    private Map<String, Object> properties = new HashMap<>();

    private Set<String> tags = new HashSet<>();

    private Status status = Status.SCHEDULED;

    private String status_detailed = "";

    public ObjectId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Project getProject() {
        return project;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        if (tags != null) {
            this.tags = tags;
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getStatus_detailed() {
        return status_detailed;
    }
}