       ___                 _____ ____  ____  ____
      / _ \ _ __   ___ _ _|_   _/ ___||  _ \| __ )
     | | | | '_ \ / _ \ '_ \| | \___ \| | | |  _ \
     | |_| | |_) |  __/ | | | |  ___) | |_| | |_) |
      \___/| .__/ \___|_| |_|_| |____/|____/|____/
           |_|    The modern time series database.

[![Build Status](https://travis-ci.org/OpenTSDB/opentsdb-tdigests.svg?branch=master)](https://travis-ci.org/OpenTSDB/opentsdb-tdigests) [![Coverage Status](https://coveralls.io/repos/github/OpenTSDB/opentsdb-tdigests/badge.svg?branch=master)](https://coveralls.io/github/OpenTSDB/opentsdb-tdigests?branch=master)

# T-Digests

This plugin is used for storing and querying [Ted Dunning's T-Digests] (https://github.com/tdunning/t-digest) as histograms with [OpenTSDB] (http://opentsdb.net/). Ted's data structure efficiently stores measurements rank based statistics. Because of the structure, quantiles at the extreme edges (99.999%, etc) have less error than those toward the mean, making this structure useful for tracking the best or worst of a time series.

## Installation

1. Download the source code and run ``mvn package`` to create the jar in the ``target/`` directory. Copy this file to your OpenTSDB plugin directory as defined in the opentsdb config via ``tsd.core.plugin_path``.
1. Add the appropriate codec class to the ``tsd.core.histograms.config`` config in ``opentsdb.conf``. E.g. ``
{
  "MergingTDigestTDigestCodec":2
}``
1. Restart the TSD and make sure the plugin was loaded and associated with the proper ID. E.g. look in the logs for lines like:

```
2017-06-03 16:26:55,044 DEBUG [main] PluginLoader: Successfully added JAR to class loader: /Users/clarsen/Documents/opentsdb/plugins/opentsdb-tdigests-2.4.0-SNAPSHOT.jar
2017-06-03 16:26:55,550 INFO  [main] HistogramCodecManager: Successfully loaded decoder 'net.opentsdb.core.MergingTDigestTDigestCodec' with ID 1

```

## Usage
Currently sketches are implemented in Java so here's an example of how to create sketches and send them to OpenTSDB. First, import the latest ``sketches-core`` jar for your project, e.g.

```xml
<dependency>
  <groupId>com.tdunning</groupId>
  <artifactId>t-digest</artifactId>
  <version>3.1</version>
</dependency>
```

```java
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
    
    // Send the b64 encoded string to the TSD via HTTP or Telnet.
  }
}}
```

This is an extremely contrived example to start playing with digests. In reality what you'll want to do is create a new digest on a given interval, for example every 60 seconds, update that digest with all of the measurements from your application, then at the end of 60 seconds, flush the sketch to TSD and start a new one. 

**NOTE:** Do not continue updating the same digest and send it over and over again to TSD or your results will be incorrect when querying. Each digest is supposed to be a snapshot over a time period.

### HTTP

To send the digest over HTTP, create a JSON object like the following:

```javascript
{
  "metric": "webserver.request.latency.ms",
  "timestamp": 1346846400,
  "id":2,
  "value":"AgMIGoAAAAADAAAAAAAAAAAAAAAAAPA/AAAAAABARUAAAAAAAADwPwAAAAAAADhAAAAAAABARUA=",
  "tags": {
    "host": "web01"
  }
}
```

and ``POST`` it to ``<tsd>:4242/api/histogram``. The ``value`` is the base 64 encoded value from the example above.

### Telnet

Similar to HTTP, encode the digest as a base 64 string and call:

``histogram webserver.request.latency.ms 1346846400 2 AgMIGoAAAAADAAAAAAAAAAAAAAAAAPA/AAAAAABARUAAAAAAAADwPwAAAAAAADhAAAAAAABARUA= host=web01``