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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

@SuppressWarnings("unused")
@Document
public class Loader implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        RUNNING,
        OK,
        IDLE,
        ERROR
    }

    @Indexed
    private String name = "UNDEF";

    private Status status = Status.IDLE;

    private String statusDetailed = "";

    private String groupName = "";

    private String version = "";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = StdDateFormat.DATE_FORMAT_STR_ISO8601)
    private Date lastExecAt = Date.from(Instant.now());

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusDetailed() {
        return statusDetailed;
    }

    public void setStatusDetailed(String statusDetailed) {
        this.statusDetailed = statusDetailed;
    }

    public String getGroupName() { return groupName; }

    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastExecAt() {
        return lastExecAt;
    }

    public void setLastExecAt(Date lastExecAt) {
        this.lastExecAt = lastExecAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Loader loader = (Loader) o;
        return name.equals(loader.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
