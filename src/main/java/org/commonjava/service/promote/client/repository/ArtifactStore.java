package org.commonjava.service.promote.client.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.commonjava.service.promote.model.StoreKey;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactStore
{
    public StoreKey key;

    public Boolean readonly;
}
