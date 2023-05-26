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
package org.commonjava.service.promote.model;

import java.util.Map;
import java.util.Objects;

public class PromoteTrackingRecords
{
    private String trackingId; // user specified tracking id

    private Map<String, PathsPromoteResult> resultMap; // promotion uuid -> result

    public String getTrackingId()
    {
        return trackingId;
    }

    public void setTrackingId(String trackingId)
    {
        this.trackingId = trackingId;
    }

    public Map<String, PathsPromoteResult> getResultMap()
    {
        return resultMap;
    }

    public void setResultMap(Map<String, PathsPromoteResult> resultMap)
    {
        this.resultMap = resultMap;
    }

    public PromoteTrackingRecords()
    {
    }

    public PromoteTrackingRecords(String trackingId, Map<String, PathsPromoteResult> resultMap)
    {
        this.trackingId = trackingId;
        this.resultMap = resultMap;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromoteTrackingRecords that = (PromoteTrackingRecords) o;
        return trackingId.equals(that.trackingId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(trackingId);
    }

    @Override
    public String toString() {
        return "PromoteTrackingRecords{" +
                "trackingId='" + trackingId + '\'' +
                ", resultMap=" + resultMap +
                '}';
    }
}
