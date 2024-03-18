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
package org.commonjava.service.promote.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.event.promote.PathsPromoteCompleteEvent;
import org.commonjava.service.promote.model.StoreKey;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletionStage;

import static java.lang.Thread.sleep;
import static org.commonjava.service.promote.client.kafka.KafkaEventDispatcher.CHANNEL_PROMOTE_COMPLETE;
import static org.commonjava.service.promote.model.StoreType.hosted;

@ApplicationScoped
public class MockPromoteEventConsumer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String POM_EXTENSION = ".pom";

    private static final String PACKAGE_TARBALL_EXTENSION = ".tgz";

    @Incoming(CHANNEL_PROMOTE_COMPLETE)
    public CompletionStage<Void> receive(Message<String> message ) throws Exception
    {
        String messagePayload = message.getPayload();
        PathsPromoteCompleteEvent event = new ObjectMapper().readValue( messagePayload, PathsPromoteCompleteEvent.class);
        StoreKey targetKey =  StoreKey.fromString( event.getTargetStore() );

        logger.info("Received message: {}", event);

        event.getCompletedPaths().forEach( path ->
        {
            if ( hosted != targetKey.getType() )
            {
                return;
            }

            if ( path.endsWith( POM_EXTENSION ) )
            {
                logger.info( "Pom file {}, will clean matched metadata file, store: {}", path, targetKey );

            }
            else if ( path.endsWith( PACKAGE_TARBALL_EXTENSION ) )
            {
                logger.info( "Tar file {}, will clean matched metadata file, store: {}", path, targetKey );
            }

            if ( event.getSourceStore().endsWith("build-evt")) {
                // wait 5s before return
                for (int i = 0; i < 5; i++) {
                    logger.info("Delete metadata-" + i);
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                logger.info("### this line should be printed before EventDispatcher saying 'Firing event done'");
            }
        } );

        return message.ack();
    }

}