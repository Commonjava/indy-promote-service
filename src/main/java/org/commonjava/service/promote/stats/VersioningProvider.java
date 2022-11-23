package org.commonjava.service.promote.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Producer class that reads a properties file off the classpath containing version info for the APP, and assembles an instance of {@link Versioning},
 * which this component then provides for injecting into other components.
 */
@Singleton
public class VersioningProvider
{

    private static final String APP_VERSIONING_PROPERTIES = "version.properties";

    private final Versioning versioning;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public VersioningProvider()
    {
        ClassLoader cl = VersioningProvider.class.getClassLoader();

        // Load app-version
        final Properties props = new Properties();
        try (InputStream is = cl.getResourceAsStream( APP_VERSIONING_PROPERTIES ))
        {
            if ( is != null )
            {
                props.load( is );
            }
            else
            {
                logger.warn( "Resource not found, file: {}, loader: {}", APP_VERSIONING_PROPERTIES, cl );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read App versioning information from classpath resource: "
                                          + APP_VERSIONING_PROPERTIES, e );
        }

        versioning = new Versioning( props.getProperty( "version", "unknown" ),
                                     props.getProperty( "builder", "unknown" ),
                                     props.getProperty( "commit.id", "unknown" ),
                                     props.getProperty( "timestamp", "unknown" ),
                                     props.getProperty( "api-version", "unknown" ) );

    }

    @Produces
    @Default
    public Versioning getVersioningInstance()
    {
        return versioning;
    }

}
