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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.globocom.grou.SystemEnv;
import com.globocom.grou.report.ReportService;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "FieldCanBeLocal", "Annotator"})
@Document
@CompoundIndexes({
    @CompoundIndex(name = "test_project", unique = true, def = "{'name': 1, 'project': 1}")
})
public class Test implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DURATION_TIME_MILLIS_PROP = "durationTimeMillis";

    public enum Status {
        SCHEDULED,
        ENQUEUED,
        RUNNING,
        OK,
        ERROR,
        ABORTED
    }

    @Id
    private String id;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private Date createdDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @LastModifiedDate
    private Date lastModifiedDate;

    @Indexed
    private String name;

    @Indexed
    private String project;

    private Set<Loader> loaders = new HashSet<>();

    private Map<String, Object> properties = new HashMap<>();

    @Indexed
    private Set<String> tags = new HashSet<>();

    @Indexed
    private Status status = Status.SCHEDULED;

    @JsonSerialize(contentUsing = ReportService.DoubleSerializer.class)
    private HashMap<String, Double> result = null;

    private Set<String> notify = new HashSet<>();

    @Transient
    private String dashboard;

    private int durationTimeMillis;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getProject() {
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<Loader> getLoaders() {
        return loaders;
    }

    public void setLoaders(Set<Loader> loaders) {
        if (loaders != null) {
            this.loaders = loaders;
        }
    }

    public HashMap<String, Double> getResult() {
        return result;
    }

    public void setResult(HashMap<String, Double> result) {
        this.result = result;
    }

    public Set<String> getNotify() {
        return notify;
    }

    public void setNotify(Set<String> notify) {
        if (notify != null) this.notify = new HashSet<>(notify);
    }

    public String getDashboard() {
        String link = "%s?refresh=5s&orgId=1&var-project=%s&var-alltags=%s&from=now-2m&to=now";
        String tagsCollection = sanitize(tags.stream().sorted().collect(Collectors.joining()), "");
        String allTags = "".equals(tagsCollection) ? "UNDEF" : tagsCollection;
        return String.format(link, SystemEnv.DASHBOARD_URL.getValue(), sanitize(project, "_"), allTags);
    }

    /**
     * @deprecated Replaced by properties.durationTimeMillis
     */
    @Deprecated
    public int getDurationTimeMillis() {
        return durationTimeMillis;
    }

    /**
     * @deprecated Replaced by properties.durationTimeMillis
     */
    @Deprecated
    public void setDurationTimeMillis(int durationTimeMillis) {
        this.durationTimeMillis = durationTimeMillis;
    }

    private String sanitize(String key, String to) {
        return key.replaceAll("[@.:/\\s\\t/\\\\]", to).toLowerCase();
    }
}
