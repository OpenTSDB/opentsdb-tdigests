package net.opentsdb.core;

import java.nio.ByteBuffer;
import javax.xml.bind.DatatypeConverter;

import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;

public class DigestExample {

  private TDigest latency = MergingDigest.createDigest(100);
  
  public void updateAndEncode() {
    latency.add(42.5);
    latency.add(1);
    latency.add(24.0);
    
    final ByteBuffer buf = ByteBuffer.allocate(latency.smallByteSize());
    latency.asSmallBytes(buf);
    final String b64 = DatatypeConverter.printBase64Binary(buf.array());
  }
}
