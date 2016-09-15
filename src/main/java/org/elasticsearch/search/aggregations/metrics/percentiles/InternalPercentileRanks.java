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
package org.elasticsearch.search.aggregations.metrics.percentiles;

import com.google.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentObject;
import org.elasticsearch.search.aggregations.AggregationStreams;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.JsonField;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.TDigestState;

import java.io.IOException;
import java.util.Iterator;

/**
*
*/
public class InternalPercentileRanks extends AbstractInternalPercentiles implements PercentileRanks {

    public final static Type TYPE = new Type("percentile_ranks");

    public final static AggregationStreams.Stream STREAM = new AggregationStreams.Stream() {
        @Override
        public InternalPercentileRanks readResult(StreamInput in) throws IOException {
            InternalPercentileRanks result = new InternalPercentileRanks();
            result.readFrom(in);
            return result;
        }

        @Override
        public InternalAggregation readResult(XContentObject in) throws IOException {
            InternalPercentileRanks result = new InternalPercentileRanks();
            result.readFrom(in);
            return result;
        }
    };

    public static void registerStreams() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
    }
    
    InternalPercentileRanks() {} // for serialization

    public InternalPercentileRanks(String name, double[] cdfValues, TDigestState state, boolean keyed) {
        super(name, cdfValues, state, keyed);
    }

    @Override
    public Iterator<Percentile> iterator() {
        if (values != null) {
            return new PercentileIterator(values.entrySet().iterator());
        }
        return new Iter(keys, state);
    }

    @Override
    public double percent(double value) {
        if (values != null) {
            return values.get(String.valueOf(value));
        }
        return percentileRank(state, value);
    }

    @Override
    public double value(double key) {
        return percent(key);
    }

    protected AbstractInternalPercentiles createReduced(String name, double[] keys, TDigestState merged, boolean keyed) {
        return new InternalPercentileRanks(name, keys, merged, keyed);
    }

    @Override
    public Type type() {
        return TYPE;
    }
    
    static double percentileRank(TDigestState state, double value) {
        double percentileRank = state.cdf(value);
        if (percentileRank < 0) {
            percentileRank = 0;
        }
        else if (percentileRank > 1) {
            percentileRank = 1;
        }
        return percentileRank * 100;
    }

    public static class Iter extends UnmodifiableIterator<Percentile> {

        private final double[] values;
        private final TDigestState state;
        private int i;

        public Iter(double[] values, TDigestState state) {
            this.values = values;
            this.state = state;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < values.length;
        }

        @Override
        public Percentile next() {
            final Percentile next = new InternalPercentile(percentileRank(state, values[i]), values[i]);
            ++i;
            return next;
        }
    }
}
