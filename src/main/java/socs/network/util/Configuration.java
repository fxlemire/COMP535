package socs.network.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.io.File;

public class Configuration {

  public final static String PROCESS_IP = "127.0.0.1";
  private static int SEQUENCE_NUMBER = Integer.MIN_VALUE + 1;

  private Config _config = null;
  private static Config _ports = ConfigFactory.parseFile(new File("conf/ports.conf"));

  public Configuration(String path) {
    _config = ConfigFactory.parseFile(new File(path));
  }

  public String getString(String key) {
    return _config.getString(key);
  }

  public Boolean getBoolean(String key) {
    return _config.getBoolean(key);
  }

  public int getInt(String key) {
    return _config.getInt(key);
  }

  public short getShort(String key) {
    return (short) _config.getInt(key);
  }

  public double getDouble(String key) {
    return _config.getDouble(key);
  }

  public static String getPortUser(short port) {
    return _ports.getString("socs.network.router.port." + port);
  }

  public void addEntry(String key, String value) {
    _config = _config.withValue(key, ConfigValueFactory.fromAnyRef(value));
  }

  public static int getSequenceNumber() {
    return ++SEQUENCE_NUMBER;
  }
}
