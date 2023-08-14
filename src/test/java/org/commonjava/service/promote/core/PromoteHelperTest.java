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
package org.commonjava.service.promote.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PromoteHelperTest
{
    private final String json = "{\n" +
            "  \"type\" : \"remote\",\n" +
            "  \"key\" : \"generic-http:remote:r-raw-githubusercontent-com-build-A2SSYZN2UEQAA\",\n" +
            "  \"description\" : \"HTTProx proxy based on: https://raw.githubusercontent.com:443/project-ncl/empty-testing-repo/tc36/brown-fox.txt\",\n" +
            "  \"metadata\" : {\n" +
            "    \"changelog\" : \"Creating HTTProx proxy for: https://raw.githubusercontent.com:443/project-ncl/empty-testing-repo/tc36/brown-fox.txt\",\n" +
            "    \"origin\" : \"httprox\",\n" +
            "    \"trackingId\" : \"build-A2SSYZN2UEQAA\"\n" +
            "  },\n" +
            "  \"disabled\" : false,\n" +
            "  \"host\" : \"raw.githubusercontent.com\",\n" +
            "  \"port\" : 443,\n" +
            "  \"user\" : \"raw.githubusercontent.com\",\n" +
            "  \"password\" : \"443\",\n" +
            "  \"name\" : \"r-raw-githubusercontent-com-build-A2SSYZN2UEQAA\",\n" +
            "  \"type\" : \"remote\",\n" +
            "  \"packageType\" : \"generic-http\",\n" +
            "  \"disable_timeout\" : 0,\n" +
            "  \"path_style\" : \"hashed\",\n" +
            "  \"authoritative_index\" : false,\n" +
            "  \"create_time\" : \"2023-08-14 08:10:48 +0000\",\n" +
            "  \"allow_snapshots\" : false,\n" +
            "  \"allow_releases\" : true,\n" +
            "  \"url\" : \"https://raw.githubusercontent.com/\",\n" +
            "  \"timeout_seconds\" : 300,\n" +
            "  \"max_connections\" : 30,\n" +
            "  \"ignore_hostname_verification\" : true,\n" +
            "  \"nfc_timeout_seconds\" : 0,\n" +
            "  \"is_passthrough\" : false,\n" +
            "  \"cache_timeout_seconds\" : 0,\n" +
            "  \"metadata_timeout_seconds\" : 0,\n" +
            "  \"proxy_port\" : 0,\n" +
            "  \"prefetch_priority\" : 0,\n" +
            "  \"prefetch_rescan\" : false,\n" +
            "  \"prefetch_listing_type\" : \"html\"\n" +
            "}";

    /**
     * This test is to verify getPathStyleFromJson can handle duplicate property ("type") and get the right path_style.
     * The duplicate property is a legacy issue in repo definitions, and we have to workaround it.
     */
    @Test
    public void testParseRepoJson() throws JsonProcessingException
    {
        assertThat( new PromotionHelper().getPathStyleFromJson(json), equalTo( "hashed" ) );
    }
}
