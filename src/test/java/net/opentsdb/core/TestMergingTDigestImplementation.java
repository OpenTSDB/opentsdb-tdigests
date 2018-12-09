// This file is part of OpenTSDB.
// Copyright (C) 2017  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package net.opentsdb.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;

import net.opentsdb.utils.JSON;

public class TestMergingTDigestImplementation {
  
  private TDigest digest;
  
  @Before
  public void before() throws Exception {
    digest = MergingDigest.createDigest(100);
    digest.add(42.5);
    digest.add(1);
    digest.add(24.0);
  }

  @Test
  public void ctor() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    assertEquals(42, histo.getId());
    
    try {
      new MergingTDigestImplementation(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
    
    try {
      new MergingTDigestImplementation(256);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
  @Test
  public void histogram() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final ByteBuffer buf = ByteBuffer.allocate(digest.smallByteSize());
    digest.asSmallBytes(buf);
    
    assertArrayEquals(buf.array(), histo.histogram(false));
    byte[] with_id = histo.histogram(true);
    assertEquals(42, with_id[0]);
    byte[] raw = new byte[with_id.length - 1];
    System.arraycopy(with_id, 1, raw, 0, with_id.length - 1);
    assertArrayEquals(buf.array(), raw);
    
    histo.setDigest(null);
    try {
      histo.histogram(false);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) { }
  }

  @Test
  public void fromHistogram() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    final ByteBuffer buf = ByteBuffer.allocate(digest.smallByteSize());
    digest.asSmallBytes(buf);
    byte[] raw = buf.array();
    
    histo.fromHistogram(raw, false);
    assertEquals(42.5, histo.percentile(95.0), 0.001);
    
    byte[] with_id = new byte[raw.length + 1];
    with_id[0] = 42;
    System.arraycopy(raw, 0, with_id, 1, raw.length);
    
    histo.fromHistogram(with_id, true);
    assertEquals(42.5, histo.percentile(95.0), 0.001);
    
    try {
      histo.fromHistogram(new byte[] { 0, 0, 0, 2, 64, 89, 0, 0, 0, 0, 0, 0, 
          0, 0, 0, 3, 63, -128, 0, 0, /*65, -72, 0, 0, 65, -108, 0, 0, 1, 1, 1*/ }, 
          false);
      fail("Expected BufferUnderflowException");
    } catch (BufferUnderflowException e) { }
    
    try {
      histo.fromHistogram(null, false);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
    
    try {
      histo.fromHistogram(new byte[0], false);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
    
    try {
      histo.fromHistogram(new byte[1], true);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
  @Test
  public void percentile() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    assertEquals(42.5, histo.percentile(95.0), 0.001);
    assertEquals(24.0, histo.percentile(50.0), 0.001);
    assertEquals(1.0, histo.percentile(0.0), 0.001);
    
    try {
      histo.percentile(-1);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
    
    try {
      histo.percentile(101);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
  @Test
  public void percentiles() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final List<Double> percentiles = 
        histo.percentiles(Lists.<Double>newArrayList(0D, 50D, 95.0D));
    assertEquals(3, percentiles.size());
    assertEquals(1.0, percentiles.get(0), 0.001);
    assertEquals(24.0, percentiles.get(1), 0.001);
    assertEquals(42.5, percentiles.get(2), 0.001);
    
    try {
      histo.percentiles(Lists.<Double>newArrayList(0D, -1D, 95.0D));
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
//  @Test
//  public void mergingDigestSerDes() throws Exception {
//    final TDigest out = MergingDigest.createDigest(100);
//    out.add(42.5);
//    out.add(1);
//    out.add(24.0);
//    assertEquals(40.649, out.quantile(0.95), 0.001);
//    
//    final ByteBuffer output = ByteBuffer.allocate(out.smallByteSize());
//    out.asSmallBytes(output);
//    
//    ByteBuffer input = ByteBuffer.wrap(output.array());
//    try {
//      MergingDigest.fromBytes(input);
//    } catch (BufferUnderflowException e) {
//      System.out.println("WTF?");
//    }
//    
//    input = ByteBuffer.wrap(output.array());
//    final TDigest in = AVLTreeDigest.fromBytes(input);
//    assertEquals(40.649, in.quantile(0.95), 0.001);
//  }
  
  @Test (expected = UnsupportedOperationException.class)
  public void getHistogram() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    histo.getHistogram();
  }
  
  @Test
  public void getClone() throws Exception {
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final MergingTDigestImplementation copy = 
        (MergingTDigestImplementation) histo.clone();
    assertNotSame(histo, copy);
    assertNotSame(histo.getDigest(), copy.getDigest());
    assertEquals(42.5, histo.percentile(95.0), 0.001);
  }
  
  @Test
  public void aggregate() throws Exception {
    TDigest digest2 = MergingDigest.createDigest(100);
    digest2.add(12);
    digest2.add(89.3);
    digest2.add(15);
    
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final MergingTDigestImplementation histo2 = 
        new MergingTDigestImplementation(42);
    histo2.setDigest(digest2);
    
    histo.aggregate(histo2, HistogramAggregation.SUM);
    
    assertEquals(89.3, histo.percentile(95.0), 0.01);
    assertEquals(19.5, histo.percentile(50.0), 0.01);
    assertEquals(1.0, histo.percentile(0.0), 0.01);
    
    final SimpleHistogram simple = new SimpleHistogram(1);
    try {
      histo.aggregate(simple, HistogramAggregation.SUM);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
  @Test
  public void aggregateList() throws Exception {
    TDigest digest2 = MergingDigest.createDigest(100);
    digest2.add(12);
    digest2.add(89.3);
    digest2.add(15);
    
    TDigest digest3 = MergingDigest.createDigest(100);
    digest3.add(33);
    digest3.add(22.4);
    digest3.add(6.98);
    
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final MergingTDigestImplementation histo2 = 
        new MergingTDigestImplementation(42);
    histo2.setDigest(digest2);
    
    final MergingTDigestImplementation histo3 = 
        new MergingTDigestImplementation(42);
    histo3.setDigest(digest3);
    
    histo.aggregate(Lists.<Histogram>newArrayList(histo2, histo3), 
        HistogramAggregation.SUM);
    
    assertEquals(89.3, histo.percentile(95.0), 0.001);
    assertEquals(22.4, histo.percentile(50.0), 0.01);
    assertEquals(1.0, histo.percentile(0.0), 0.01);
    
    final SimpleHistogram simple = new SimpleHistogram(1);
    try {
      histo.aggregate(simple, HistogramAggregation.SUM);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }

  @Test
  public void foo() throws Exception {
    TDigest digest2 = MergingDigest.createDigest(100);
    digest2.add(12);
    digest2.add(89.3);
    digest2.add(15);
    
    TDigest digest3 = MergingDigest.createDigest(100);
    digest3.add(33);
    digest3.add(22.4);
    digest3.add(6.98);
    
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.setDigest(digest);
    
    final MergingTDigestImplementation histo2 = 
        new MergingTDigestImplementation(42);
    histo2.setDigest(digest2);
    
    final MergingTDigestImplementation histo3 = 
        new MergingTDigestImplementation(42);
    histo3.setDigest(digest3);
    
    List<HistogramPojo> histos = Lists.newArrayList();
    
    HistogramPojo pojo = new HistogramPojo();
    pojo.setMetric("sys.cpu.nice");
    pojo.setTags(Maps.<String, String>newHashMap(ImmutableMap.<String, String>builder()
        .put("host", "web01")
        .build()));
    pojo.setTimestamp(1346846400);
    pojo.setId(1);
    pojo.setValue(HistogramPojo.bytesToBase64String(histo.histogram(false)));
    histos.add(pojo);
   
    pojo = new HistogramPojo();
    pojo.setMetric("sys.cpu.nice");
    pojo.setTags(Maps.<String, String>newHashMap(ImmutableMap.<String, String>builder()
        .put("host", "web01")
        .build()));
    pojo.setTimestamp(1346846460);
    pojo.setId(1);
    pojo.setValue(HistogramPojo.bytesToBase64String(histo2.histogram(false)));
    histos.add(pojo);
    
    pojo = new HistogramPojo();
    pojo.setMetric("sys.cpu.nice");
    pojo.setTags(Maps.<String, String>newHashMap(ImmutableMap.<String, String>builder()
        .put("host", "web01")
        .build()));
    pojo.setTimestamp(1346846520);
    pojo.setId(1);
    pojo.setValue(HistogramPojo.bytesToBase64String(histo3.histogram(false)));
    histos.add(pojo);
    
    System.out.println(JSON.serializeToString(histos));
  }
}
