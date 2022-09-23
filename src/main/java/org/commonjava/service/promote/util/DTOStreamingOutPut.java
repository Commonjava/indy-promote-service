package org.commonjava.service.promote.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

public class DTOStreamingOutPut
                implements StreamingOutput
{

    private static final double NANOS_PER_SEC = 1000000000.0;

    private final ObjectMapper mapper;

    private final Object dto;

    //TODO: will think about metrics later
    //    private final DefaultMetricsManager metricsManager;

    //    private final IndyMetricsConfig metricsConfig;

    //    public DTOStreamingOutput( final ObjectMapper mapper, final Object dto, final DefaultMetricsManager metricsManager )
    //    {
    //        this.mapper = mapper;
    //        this.dto = dto;
    //        this.metricsManager = metricsManager;
    //        //        this.metricsConfig = metricsConfig;
    //    }
    public DTOStreamingOutPut( final ObjectMapper mapper, final Object dto )
    {
        this.mapper = mapper;
        this.dto = dto;
    }

    @Override
    public String toString()
    {
        try
        {
            return mapper.writeValueAsString( dto );
        }
        catch ( JsonProcessingException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Could not render toString() for DTO: " + dto, e );
            return String.valueOf( dto );
        }
    }

    @Override
    public void write( final OutputStream outputStream )
                    throws IOException, WebApplicationException
    {
        CountingOutputStream cout = new CountingOutputStream( outputStream );

        try
        {
            mapper.writeValue( cout, dto );
        }
        finally
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Wrote: {} bytes", cout.getByteCount() );

        }
        // TODO: This is metrics wrapped way, will rollback to this when enable metrics
        //        AtomicReference<IOException> ioe = new AtomicReference<>();
        //        metricsManager.wrapWithStandardMetrics( () -> {
        //            CountingOutputStream cout = new CountingOutputStream( outputStream );
        //            //            long start = System.nanoTime();
        //            try
        //            {
        //                mapper.writeValue( cout, dto );
        //            }
        //            catch ( IOException e )
        //            {
        //                ioe.set( e );
        //            }
        //            finally
        //            {
        //                Logger logger = LoggerFactory.getLogger( getClass() );
        //                logger.trace( "Wrote: {} bytes", cout.getByteCount() );
        //
        //                //                String name = getName( metricsConfig.getNodePrefix(), TRANSFER_METRIC_NAME,
        //                //                                       getDefaultName( dto.getClass(), "write" ), METER );
        //                //
        //                //                long end = System.nanoTime();
        //                //                double elapsed = ( end - start ) / NANOS_PER_SEC;
        //                //
        //                //                Meter meter = metricsManager.getMeter( name );
        //                //                meter.mark( Math.round( cout.getByteCount() / elapsed ) );
        //            }
        //
        //            return null;
        //
        //        }, () -> null );

        //        if ( ioe.get() != null )
        //        {
        //            throw ioe.get();
        //        }
    }
}
