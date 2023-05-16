/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.service.promote.tracking.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.commonjava.service.promote.model.PromoteQueryByPath;

import java.util.Objects;

import static org.commonjava.service.promote.tracking.cassandra.SchemaUtils.TABLE_QUERY_BY_PATH;

@Table( name = TABLE_QUERY_BY_PATH, readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxPromoteQueryByPath implements PromoteQueryByPath
{
    @PartitionKey(0)
    private String target;

    @PartitionKey(1)
    private String path;

    @Column
    private String trackingId;

    @Column
    private String source;

    @Override
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    @Override
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DtxPromoteQueryByPath that = (DtxPromoteQueryByPath) o;
        return target.equals(that.target) && path.equals(that.path)
                && trackingId.equals(that.trackingId) && source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, path, trackingId, source);
    }

    @Override
    public String toString() {
        return "DtxPromoteQueryByPath{" +
                "target='" + target + '\'' +
                ", path='" + path + '\'' +
                ", trackingId='" + trackingId + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
