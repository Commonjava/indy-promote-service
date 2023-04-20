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
import org.commonjava.service.promote.core.IndyObjectMapper;
import org.commonjava.service.promote.model.PathsPromoteResult;
import org.commonjava.service.promote.model.PromoteTrackingRecords;
import org.commonjava.service.promote.tracking.cassandra.CassandraClient;
import org.commonjava.service.promote.tracking.cassandra.CassandraConfiguration;
import org.commonjava.service.promote.tracking.cassandra.DtxPromoteRecord;
import org.commonjava.service.promote.tracking.cassandra.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

import static org.commonjava.service.promote.tracking.cassandra.SchemaUtils.TABLE_TRACKING;

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

    private PreparedStatement preparedTrackingRecordRollback;

    private Mapper<DtxPromoteRecord> promoteRecordMapper;

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

        MappingManager mappingManager = new MappingManager(session);
        promoteRecordMapper = mappingManager.mapper(DtxPromoteRecord.class, keySpace);

        preparedTrackingRecordQuery = session.prepare("SELECT * FROM " + keySpace + "." + TABLE_TRACKING
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

        logger.debug("addTrackingRecord, trackingId: {}", trackingId);
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

    public void rollbackTrackingRecord(String trackingId, String promotionId)
    {
        BoundStatement bound = preparedTrackingRecordRollback.bind( trackingId, promotionId );
        session.execute( bound );
    }
}
