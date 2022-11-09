package org.commonjava.service.promote.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndyObjectMapper extends ObjectMapper
{
    @PostConstruct
    public void init()
    {
        setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
        configure( JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true );
        configure( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        enable( SerializationFeature.INDENT_OUTPUT, SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID );
        disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
    }
}
