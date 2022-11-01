/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
package org.commonjava.service.promote.client.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import org.commonjava.event.promote.PathsPromoteCompleteEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * This event dispatcher will dispatch Store Event through kafka
 */
@ApplicationScoped
@Startup
public class KafkaEventDispatcher
{
    public static final String CHANNEL_PROMOTE_COMPLETE = "promote-complete";

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Channel(CHANNEL_PROMOTE_COMPLETE)
    @OnOverflow( value = OnOverflow.Strategy.BUFFER )
    @Inject
    Emitter<String> emitter;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void fireEvent( PathsPromoteCompleteEvent event )
    {
        logger.debug( "Firing event to external: {}", event );
        try
        {
            emitter.send( objectMapper.writeValueAsString(event) );
        }
        catch ( Exception e )
        {
            logger.error( "Firing event failed", e );
        }
    }
}
