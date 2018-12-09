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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;

public class TestMergingTDigestCodec {

private TDigest digest;
  
  @Before
  public void before() throws Exception {
    digest = MergingDigest.createDigest(100);
    digest.add(42.5);
    digest.add(1);
    digest.add(24.0);
  }
  
  @Test
  public void decode() throws Exception {
    final MergingTDigestTDigestCodec codec = new MergingTDigestTDigestCodec();
    codec.setId(42);
    
    final ByteBuffer buf = ByteBuffer.allocate(digest.smallByteSize());
    digest.asSmallBytes(buf);
    
    final byte[] raw = buf.array();
    Histogram histo = codec.decode(raw, false);
    assertArrayEquals(raw, histo.histogram(false));
    assertEquals(42.5, histo.percentile(95.0), 0.001);
    
    byte[] with_id = new byte[raw.length + 1];
    with_id[0] = 42;
    System.arraycopy(raw, 0, with_id, 1, raw.length);
    histo = codec.decode(raw, false);
    assertArrayEquals(raw, histo.histogram(false));
    assertArrayEquals(with_id, histo.histogram(true));
    assertEquals(42.5, histo.percentile(95.0), 0.001);
    
    try {
      codec.decode(null, false);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
    
    try {
      codec.decode(new byte[0], false);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
  
  @Test
  public void encode() throws Exception {
    final MergingTDigestTDigestCodec codec = new MergingTDigestTDigestCodec();
    codec.setId(42);
    
    final ByteBuffer buf = ByteBuffer.allocate(digest.smallByteSize());
    digest.asSmallBytes(buf);
    
    final byte[] raw = buf.array();
    final MergingTDigestImplementation histo = 
        new MergingTDigestImplementation(42);
    histo.fromHistogram(raw, false);
    
    assertArrayEquals(raw, codec.encode(histo, false));
    
    byte[] with_id = codec.encode(histo, true);
    assertEquals(42, with_id[0]);
    byte[] raw_component = new byte[raw.length];
    System.arraycopy(with_id, 1, raw_component, 0, raw.length);
    assertArrayEquals(raw, raw_component);
    
    try {
      codec.encode(null, false);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) { }
  }
}
