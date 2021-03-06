package com.spotify.heroic.aggregation;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.heroic.common.DateRange;
import com.spotify.heroic.common.Series;
import com.spotify.heroic.metric.MetricType;
import com.spotify.heroic.metric.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Test;
import org.mockito.Mockito;

public class BucketAggregationTest {
    public final class IterableBuilder {
        final ArrayList<Point> datapoints = new ArrayList<Point>();

        public IterableBuilder add(long timestamp, double value) {
            datapoints.add(new Point(timestamp, value));
            return this;
        }

        public List<Point> result() {
            return datapoints;
        }
    }

    public IterableBuilder build() {
        return new IterableBuilder();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestBucket extends AbstractBucket {
        private final long timestamp;
        private double sum;

        public void updatePoint(Map<String, String> key, Point d) {
            sum += d.getValue();
        }

        @Override
        public long timestamp() {
            return timestamp;
        }
    }

    public BucketAggregationInstance<TestBucket> setup(long sampling, long extent) {
        return new BucketAggregationInstance<TestBucket>(sampling, extent,
            ImmutableSet.of(MetricType.POINT), MetricType.POINT) {
            @Override
            protected TestBucket buildBucket(long timestamp) {
                return new TestBucket(timestamp);
            }

            @Override
            protected Point build(TestBucket bucket) {
                return new Point(bucket.timestamp, bucket.sum);
            }

            @Override
            public AggregationInstance distributed() {
                return this;
            }

            @Override
            public AggregationInstance reducer() {
                return Mockito.mock(AggregationInstance.class);
            }
        };
    }

    final Map<String, String> group = ImmutableMap.of();
    final Set<Series> series = ImmutableSet.of();

    @Test
    public void testSameSampling() {
        final BucketAggregationInstance<TestBucket> a = setup(10, 10);
        final AggregationSession session = a.session(new DateRange(10, 30));

        session.updatePoints(group, series, build()
            .add(10, 1000.0)
            .add(11, 3.0)
            .add(12, 4.0)
            .add(20, 7.0)
            .add(21, 3.0)
            .add(22, 4.0)
            .add(31, 1000.0)
            .result());

        final AggregationResult result = session.result();

        assertEquals(build().add(20, 14.0).add(30, 7.0).result(),
            result.getResult().get(0).getMetrics().getData());
    }

    @Test
    public void testLongerExtent() {
        final BucketAggregationInstance<TestBucket> a = setup(10, 20);
        final AggregationSession session = a.session(new DateRange(10, 30));

        session.updatePoints(group, series, build()
            .add(0, 1000.0)
            .add(1, 3.0)
            .add(2, 4.0)
            .add(11, 3.0)
            .add(12, 4.0)
            .add(31, 1000.0)
            .result());

        final AggregationResult result = session.result();

        assertEquals(build().add(20, 14.0).add(30, 7.0).result(),
            result.getResult().get(0).getMetrics().getData());
    }

    @Test
    public void testShorterExtent() {
        final BucketAggregationInstance<TestBucket> a = setup(10, 5);
        final AggregationSession session = a.session(new DateRange(10, 30));

        session.updatePoints(group, series, build()
            .add(15, 1000.0)
            .add(16, 3.0)
            .add(17, 4.0)
            .add(21, 1000.0)
            .add(25, 1000.0)
            .add(26, 3.0)
            .add(27, 4.0)
            .add(31, 1000.0)
            .result());

        final AggregationResult result = session.result();

        assertEquals(build().add(20, 7.0).add(30, 7.0).result(),
            result.getResult().get(0).getMetrics().getData());
    }

    private void checkBucketAggregation(
        List<Point> input, List<Point> expected, final long extent
    ) {
        final BucketAggregationInstance<TestBucket> a = setup(1000, extent);
        final AggregationSession session = a.session(new DateRange(1000, 3000));
        session.updatePoints(group, series, input);

        final AggregationResult result = session.result();

        assertEquals(expected, result.getResult().get(0).getMetrics().getData());
    }

    @Test
    public void testUnevenSampling() {
        final BucketAggregationInstance<TestBucket> a = setup(10, 15);
        final AggregationSession session = a.session(new DateRange(10, 40));

        session.updatePoints(group, series, build()
            .add(5, 1000.0)
            .add(6, 3.0)
            .add(7, 4.0)
            .add(16, 3.0)
            .add(17, 4.0)
            .add(40, 1.0)
            .add(41, 1000.0)
            .result());

        final AggregationResult result = session.result();

        assertEquals(build().add(20, 14.0).add(30, 7.0).add(40, 1.0).result(),
            result.getResult().get(0).getMetrics().getData());
    }

    @Test
    public void testMapTimestamps() {
        final BucketAggregationInstance<TestBucket> a = setup(10, 10);
        final BucketAggregationInstance.Session session =
            (BucketAggregationInstance.Session) a.session(new DateRange(10, 30));

        final Map<Long, BucketAggregationInstance.StartEnd> fromTo = new HashMap<>();

        // underflow
        for (long ts = 0L; ts <= 10L; ts++) {
            fromTo.put(ts, new BucketAggregationInstance.StartEnd(0, 0));
        }

        // first bucket
        for (long ts = 11L; ts <= 20L; ts++) {
            fromTo.put(ts, new BucketAggregationInstance.StartEnd(0, 1));
        }

        // second bucket
        for (long ts = 21L; ts <= 30L; ts++) {
            fromTo.put(ts, new BucketAggregationInstance.StartEnd(1, 2));
        }

        // overflow
        for (long ts = 31L; ts <= 40L; ts++) {
            fromTo.put(ts, new BucketAggregationInstance.StartEnd(2, 2));
        }

        for (final Map.Entry<Long, BucketAggregationInstance.StartEnd> e : fromTo.entrySet()) {
            assertEquals("Expected same mapping for timestamp " + e.getKey(), e.getValue(),
                session.mapTimestamp(e.getKey()));
        }
    }
}
