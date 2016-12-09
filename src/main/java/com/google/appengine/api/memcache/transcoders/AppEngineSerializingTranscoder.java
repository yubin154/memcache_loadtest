package com.google.appengine.api.memcache.transcoders;

import com.google.appengine.api.memcache.transcoders.AppEngineSerialization.Flag;
import com.google.appengine.api.memcache.transcoders.AppEngineSerialization.ValueAndFlags;
import java.io.IOException;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.BaseSerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

/**
 * 
 */
public class AppEngineSerializingTranscoder extends BaseSerializingTranscoder implements
    Transcoder<Object> {

  public AppEngineSerializingTranscoder() {
    super(CachedData.MAX_SIZE);
  }

  @Override
  public boolean asyncDecode(CachedData d) {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.Transcoder#decode(net.spy.memcached.CachedData)
   */
  public Object decode(CachedData d) {
    try {
      return AppEngineSerialization.deserialize(d.getData(), d.getFlags());
    } catch (Exception e) {
      throw new TranscoderException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see net.spy.memcached.Transcoder#encode(java.lang.Object)
   */
  public CachedData encode(Object o) {
    try {
      ValueAndFlags vaf = AppEngineSerialization.serialize(o);
      return new CachedData(vaf.flags.ordinal(), vaf.value, getMaxSize());
    } catch (IOException e) {
      throw new TranscoderException(e);
    }
  }
}
