/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.search;

import org.apache.http.HttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.Scroll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.elasticsearch.action.ValidateActions.addValidationError;
import static org.elasticsearch.search.Scroll.readScroll;

/**
 *
 */
public class SearchScrollRequest extends ActionRequest<SearchScrollRequest> {

    private String scrollId;
    private Scroll scroll;

    public SearchScrollRequest() {
    }

    public SearchScrollRequest(String scrollId) {
        this.scrollId = scrollId;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (scrollId == null) {
            validationException = addValidationError("scrollId is missing", validationException);
        }
        return validationException;
    }

    /**
     * The scroll id used to scroll the search.
     */
    public String scrollId() {
        return scrollId;
    }

    public SearchScrollRequest scrollId(String scrollId) {
        this.scrollId = scrollId;
        return this;
    }

    /**
     * If set, will enable scrolling of the search request.
     */
    public Scroll scroll() {
        return scroll;
    }

    /**
     * If set, will enable scrolling of the search request.
     */
    public SearchScrollRequest scroll(Scroll scroll) {
        this.scroll = scroll;
        return this;
    }

    /**
     * If set, will enable scrolling of the search request for the specified timeout.
     */
    public SearchScrollRequest scroll(TimeValue keepAlive) {
        return scroll(new Scroll(keepAlive));
    }

    /**
     * If set, will enable scrolling of the search request for the specified timeout.
     */
    public SearchScrollRequest scroll(String keepAlive) {
        return scroll(new Scroll(TimeValue.parseTimeValue(keepAlive, null)));
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        if (in.getVersion().before(Version.V_1_2_0)) {
            in.readByte(); // backward comp. for operation threading
        }
        scrollId = in.readString();
        if (in.readBoolean()) {
            scroll = readScroll(in);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        if (out.getVersion().before(Version.V_1_2_0)) {
            out.writeByte((byte) 2); // operation threading
        }
        out.writeString(scrollId);
        if (scroll == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            scroll.writeTo(out);
        }
    }

    @Override
    public String getEndPoint() {
        return "_search/scroll";
    }

    @Override
    public RestRequest.Method getMethod() {
        return RestRequest.Method.GET;
    }

    @Override
    public Map<String, String> getParams() {
        if (scroll != null) {
            return new MapBuilder<String, String>()
                    .put("scroll", this.scroll.keepAlive().toString()).map();
        }
        return super.getParams();
    }

    @Override
    public HttpEntity getEntity() throws IOException {
        return new NStringEntity(scrollId, StandardCharsets.UTF_8);
    }
}
