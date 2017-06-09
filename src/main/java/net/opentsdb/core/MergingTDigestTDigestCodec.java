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

import com.tdunning.math.stats.TDigest;

import net.opentsdb.core.Histogram;
import net.opentsdb.core.HistogramDataPointCodec;

/**
 * Plugin that handles encoding/decoding {@link TDigest} objects. 
 * 
 * @since 2.4
 */
public class MergingTDigestTDigestCodec extends HistogramDataPointCodec {

  @Override
  public Histogram decode(final byte[] raw_data, final boolean includes_id) {
    if (raw_data == null || raw_data.length < 8) {
      throw new IllegalArgumentException("Raw data cannot be null or less "
          + "than 8 bytes.");
    }
    final MergingTDigestImplementation histogram = 
        new MergingTDigestImplementation(id);
    histogram.fromHistogram(raw_data, includes_id);
    return histogram;
  }

  @Override
  public byte[] encode(final Histogram data_point, final boolean include_id) {
    if (data_point == null) {
      throw new IllegalArgumentException("Histogram cannot be null.");
    }
    return data_point.histogram(include_id);
  }

}
