package org.commonjava.service.promote.stats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

@Alternative
@Named
public class Versioning
{

    private String version;

    private String builder;

    @JsonProperty( "commit-id" )
    private String commitId;

    private String timestamp;

    private String apiVersion;

    public Versioning()
    {
    }

    @JsonCreator
    public Versioning( @JsonProperty( value = "version" ) final String version,
                       @JsonProperty( "builder" ) final String builder,
                       @JsonProperty( "commit-id" ) final String commitId,
                       @JsonProperty( "timestamp" ) final String timestamp,
                       @JsonProperty( "api-version" ) final String apiVersion )
    {
        this.version = version;
        this.builder = builder;
        this.commitId = commitId;
        this.timestamp = timestamp;
        this.apiVersion = apiVersion;
    }

    public String getVersion()
    {
        return version;
    }

    public String getBuilder()
    {
        return builder;
    }

    public String getCommitId()
    {
        return commitId;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public String getApiVersion()
    {
        return apiVersion;
    }

}
