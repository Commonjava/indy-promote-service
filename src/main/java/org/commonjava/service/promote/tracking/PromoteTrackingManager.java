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
package org.commonjava.service.promote.tracking;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.model.*;
import org.commonjava.service.promote.tracking.cassandra.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.service.promote.tracking.cassandra.SchemaUtils.TABLE_TRACKING;
import static org.commonjava.service.promote.util.PathUtils.ROOT;

@Startup
@ApplicationScoped
public class PromoteTrackingManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    CassandraClient client;

    @Inject
    CassandraConfiguration config;

    @Inject
    IndyObjectMapper objectMapper;

    private boolean trackingEnabled; // if tracking is enabled

    private Session session;

    private PreparedStatement preparedTrackingRecordQuery;

    private PreparedStatement preparedTrackingRecordDelete;

    private PreparedStatement preparedTrackingRecordRollback;

    private Mapper<DtxPromoteRecord> promoteRecordMapper;

    private Mapper<DtxPromoteQueryByPath> promoteQueryByPathMapper;

    public PromoteTrackingManager() {
    }

    public PromoteTrackingManager(CassandraClient client, CassandraConfiguration config )
    {
        this.client = client;
        this.config = config;
        this.objectMapper = new IndyObjectMapper();
        this.objectMapper.init();
        init();
    }

    @PostConstruct
    public void init()
    {
        if (!config.isEnabled())
        {
            logger.info("Cassandra not enabled, skip.");
            return;
        }

        logger.info("Init Cassandra promote tracking manager.");
        String keySpace = config.getKeyspace();

        session = client.getSession(keySpace);
        if ( session == null )
        {
            logger.info("Failed to get Cassandra session");
            return;
        }

        session.execute(SchemaUtils.getSchemaCreateKeyspace(keySpace, config.getKeyspaceReplicas()));
        session.execute(SchemaUtils.getSchemaCreateTableTracking(keySpace));
        session.execute(SchemaUtils.getSchemaCreateTableQueryByPath(keySpace));

        MappingManager mappingManager = new MappingManager(session);
        promoteRecordMapper = mappingManager.mapper(DtxPromoteRecord.class, keySpace);
        promoteQueryByPathMapper = mappingManager.mapper(DtxPromoteQueryByPath.class, keySpace);

        preparedTrackingRecordQuery = session.prepare("SELECT * FROM " + keySpace + "." + TABLE_TRACKING
                + " WHERE trackingId=?");

        preparedTrackingRecordDelete = session.prepare("DELETE FROM " + keySpace + "." + TABLE_TRACKING
                + " WHERE trackingId=?");

        preparedTrackingRecordRollback = session.prepare("UPDATE " + keySpace + "." + TABLE_TRACKING
                + " SET rollback=True WHERE trackingId=? AND promotionId=?");

        trackingEnabled = true;
    }

    public Optional<PromoteTrackingRecords> getTrackingRecords( String trackingId )
    {
        if (!trackingEnabled)
        {
            logger.debug("Tracking not enabled, skip getTrackingRecords");
            return Optional.empty();
        }

        BoundStatement bound = preparedTrackingRecordQuery.bind( trackingId );
        ResultSet resultSet = session.execute( bound );
        Result<DtxPromoteRecord> records = promoteRecordMapper.map(resultSet);

        Map<String, PathsPromoteResult> resultMap = new HashMap<>();
        records.forEach( record -> {
            if ( record.isRollback() )
            {
                logger.debug("Skip rollback record, trackingId: {}, promotionId: {}", trackingId, record.getPromotionId());
            }
            else
            {
                PathsPromoteResult ret = toPathsPromoteResult( record.getResult() );
                if ( ret != null )
                {
                    resultMap.put( ret.getRequest().getPromotionId(), ret );
                }
            }
        } );

        if ( resultMap.isEmpty() )
        {
            return Optional.empty();
        }
        return Optional.of( new PromoteTrackingRecords( trackingId, resultMap ));
    }

    public void addTrackingRecord( String trackingId, PathsPromoteResult result ) throws Exception
    {
        if (!trackingEnabled)
        {
            logger.debug("Tracking not enabled, skip addTrackingRecord");
            return;
        }

        DtxPromoteRecord dtxPromoteRecord = new DtxPromoteRecord();
        dtxPromoteRecord.setTrackingId(trackingId);
        dtxPromoteRecord.setPromotionId(result.getRequest().getPromotionId());
        dtxPromoteRecord.setResult(objectMapper.writeValueAsString( result ));
        promoteRecordMapper.save(dtxPromoteRecord);

        // Also update query-by-path table
        updateQueryByPath(trackingId, result.getRequest(), result.getCompletedPaths(), false);

        logger.debug("Add tracking record done, trackingId: {}", trackingId);
    }

    public void deleteTrackingRecords( String trackingId )
    {
        if (!trackingEnabled)
        {
            logger.debug("Tracking not enabled, skip deleteTrackingRecords");
            return;
        }

        // Get record(s)
        Optional<PromoteTrackingRecords> recordsOptional = getTrackingRecords( trackingId );
        if (recordsOptional.isEmpty())
        {
            logger.debug("Tracking not found, trackingId: {}", trackingId);
            return;
        }

        //  Delete from query-by-path table
        final PromoteTrackingRecords records = recordsOptional.get();
        final AtomicInteger count = new AtomicInteger();
        records.getResultMap().values().forEach( ret -> {
            StoreKey target = ret.getRequest().getTarget();
            Set<String> paths = ret.getCompletedPaths();
            if ( paths != null )
            {
                paths.forEach( p -> promoteQueryByPathMapper.delete( target.toString(), normalizeTrackedPath(p) ) );
                count.incrementAndGet();
            }
        });
        logger.debug("Delete from query-by-path done, trackingId: {}, count: {}", trackingId, count.get());

        // Delete record(s) by tracking id
        BoundStatement bound = preparedTrackingRecordDelete.bind( trackingId );
        session.execute( bound );

        logger.info("Delete tracking record done, trackingId: {}", trackingId);
    }

    public Optional<PromoteQueryByPath> queryByRepoAndPath( String repo, String path )
    {
        // If the value is null, ofNullable returns an empty instance of the Optional class
        return Optional.ofNullable(promoteQueryByPathMapper.get(repo, path));
    }

    private void updateQueryByPath(String trackingId, PathsPromoteRequest request, Set<String> completedPaths,
                                   boolean rollback)
    {
        if ( completedPaths != null )
        {
            String target = request.getTarget().toString();
            String source = request.getSource().toString();
            completedPaths.forEach( path -> {
                DtxPromoteQueryByPath et = new DtxPromoteQueryByPath();
                et.setTarget(target);
                et.setPath(normalizeTrackedPath(path));
                et.setRollback(rollback);
                et.setTrackingId(trackingId);
                et.setSource(source);
                promoteQueryByPathMapper.saveAsync( et );
            });
            logger.debug("Update query-by-path, rollback: {}, size: {}", rollback, completedPaths.size());
        }
    }

    /**
     * Promoted paths were sent by client. Usually they begin with '/'. For querying purpose, we prepend '/' if otherwise.
     */
    public static String normalizeTrackedPath(String path)
    {
        if (path.startsWith(ROOT))
        {
            return path;
        }
        return ROOT + path;
    }

    private PathsPromoteResult toPathsPromoteResult(String result)
    {
        try
        {
            return objectMapper.readValue( result, PathsPromoteResult.class );
        }
        catch (JsonProcessingException e)
        {
            logger.error( "Failed to readValue, result: " + result,  e);
        }
        return null;
    }

    public void rollbackTrackingRecord(String trackingId, PathsPromoteRequest request, Set<String> completedPaths)
    {
        BoundStatement bound = preparedTrackingRecordRollback.bind( trackingId, request.getPromotionId() );
        session.execute( bound );

        // Update query-by-path table to set the rollback flag
        updateQueryByPath(trackingId, request, completedPaths, true);
    }
}
