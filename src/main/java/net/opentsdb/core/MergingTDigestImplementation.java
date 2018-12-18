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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;

import net.opentsdb.core.Histogram;
import net.opentsdb.core.HistogramAggregation;

/**
 * TODO
 *  
 * @since 2.4
 */
public class MergingTDigestImplementation implements Histogram {

  /** The ID of this histogram in the TSD. */
  private final int id;
  
  /** The digest for this data point. */
  private TDigest digest;
  
  /**
   * Default ctor.
   * @param id The ID within the TSD.
   * @throws IllegalArgumentException if the ID was not a value from 0 to 255.
   */
  public MergingTDigestImplementation(final int id) {
    if (id < 0 || id > 255) {
      throw new IllegalArgumentException("ID must be between 0 and 255");
    }
    this.id = id;
  }
  
  public byte[] histogram(final boolean include_id) {
    if (digest == null) {
      throw new IllegalStateException("The sketch has not been set yet.");
    }
    final ByteBuffer buf = ByteBuffer.allocate(digest.smallByteSize());
    digest.asSmallBytes(buf);
    final byte[] encoded = buf.array();
    if (include_id) {
      final byte[] with_id = new byte[encoded.length + 1];
      with_id[0] = (byte) id;
      System.arraycopy(encoded, 0, with_id, 1, encoded.length);
      return with_id;
    }
    return encoded;
  }

  public void fromHistogram(final byte[] raw, final boolean includes_id) {
    if (raw == null || raw.length < 8) {
      throw new IllegalArgumentException("Raw data cannot be null or less "
          + "than 8 bytes.");
    }
    if (includes_id && raw.length < 9) {
      throw new IllegalArgumentException("Must have more than 1 bytes.");
    }
    final byte[] encoded;
    if (includes_id) {
      encoded = new byte[raw.length - 1];
      System.arraycopy(raw, 1, encoded, 0, raw.length - 1);
    } else {
      encoded = raw;
    }
    final ByteBuffer buf = ByteBuffer.wrap(encoded);
    digest = MergingDigest.fromBytes(buf);
  }

  public double percentile(double p) {
    return digest.quantile(p / 100);
  }

  public List<Double> percentiles(List<Double> p) {
    final List<Double> percentiles = Lists.newArrayListWithCapacity(p.size());
    
    for (int i = 0; i < p.size(); i++) {
      percentiles.add(digest.quantile(p.get(i) / 100));
    }
    return percentiles;
  }

  public Map getHistogram() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public Histogram clone() {
    final MergingTDigestImplementation clone = 
        new MergingTDigestImplementation(id);
    clone.fromHistogram(histogram(false), false);
    return clone;
  }

  public int getId() {
    return id;
  }

  public void aggregate(final Histogram histo, final HistogramAggregation func) {
    if (func != HistogramAggregation.SUM) {
      throw new UnsupportedOperationException("Function " + func 
          + " is not supported yet."); 
    }
    if (!(histo instanceof MergingTDigestImplementation)) {
      throw new IllegalArgumentException("Incoming histogram was not of the "
          + "same type: " + histo.getClass());
    }
    digest.add(((MergingTDigestImplementation) histo).digest);
  }

  public void aggregate(final List<Histogram> histos, 
                        final HistogramAggregation func) {
    if (func != HistogramAggregation.SUM) {
      throw new UnsupportedOperationException("Function " + func 
          + " is not supported yet."); 
    }
    for (final Histogram histogram : histos) {
      if (!(histogram instanceof MergingTDigestImplementation)) {
        throw new IllegalArgumentException("Incoming histogram was not of the "
            + "same type: " + histogram.getClass());
      }
      digest.add(((MergingTDigestImplementation) histogram).digest);
    }
  }

  /**
   * @param digest The digest to set for this histogram.
   */
  public void setDigest(final TDigest digest) {
    this.digest = digest;
  }
  
  /** @return The digest associated with this histogram. */
  public TDigest getDigest() {
    return digest;
  }
}
