package org.commonjava.service.promote.fixture;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.commonjava.service.promote.client.kafka.KafkaEventDispatcher.CHANNEL_PROMOTE_COMPLETE;

/**
 * This is to mock some HTTP resources. It is just another way to mock other services like storage.
 */
public class TestResources
                implements QuarkusTestResourceLifecycleManager
{
    public WireMockServer wireMockServer;

    @Override
    public Map<String, String> start()
    {
        wireMockServer = new WireMockServer(
                        new WireMockConfiguration().port( 9090 ).extensions( ReturnSameBodyTransformer.class ) );
        wireMockServer.start();
        return emptyMap();
/*
 * After adding MockPromoteEventConsumer, this is not needed.
 *
        // for Kafka tests
        Map<String, String> env = new HashMap<>();
        Map<String, String> props1 = InMemoryConnector.switchOutgoingChannelsToInMemory( CHANNEL_PROMOTE_COMPLETE );
        env.putAll(props1);
        return env;
*/
    }

    /**
     * Allow this test resource to provide custom injection of fields of the test class.
     */
    @Override
    public void inject( Object testInstance )
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );
        //logger.debug( "Start injection" );
        Class<?> c = testInstance.getClass();
        while ( c != Object.class )
        {
            //logger.debug( "Check class: {}", c );
            for ( Field f : c.getDeclaredFields() )
            {
                //logger.debug( "Check field: {}", f );
                if ( f.getAnnotation( WireServerInject.class ) != null )
                {
                    //logger.debug( "Get annotated field: {}", f );
                    f.setAccessible( true );
                    try
                    {
                        logger.debug( "Set mock server, {}", wireMockServer );
                        f.set( testInstance, wireMockServer );
                        return;
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    @Override
    public void stop()
    {
        if ( wireMockServer != null )
        {
            wireMockServer.stop();
        }
        InMemoryConnector.clear();
    }
}