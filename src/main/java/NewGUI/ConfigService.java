package NewGUI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class ConfigService {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  public static void save(AppConfig config, File file) throws IOException {
    MAPPER.writeValue(file, config);
  }

  public static AppConfig load(File file) throws IOException {
    return MAPPER.readValue(file, AppConfig.class);
  }
}