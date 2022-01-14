package space.rymiel.secret.config;

import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.NamingSchemes;

import java.io.File;

public record ConfigurationManager<T>(File file, TypeToken<T> type, T instance) {
  private static final ObjectMapper.Factory objectFactory = ObjectMapper
      .factoryBuilder()
      .defaultNamingScheme(NamingSchemes.LOWER_CASE_DASHED)
      .build();
  private static final HoconConfigurationLoader.Builder configBuilder = HoconConfigurationLoader.builder()
      .defaultOptions(opts ->
          opts.serializers(builder -> builder.registerAnnotatedObjects(objectFactory))
      );

  public T loadConfig() throws ConfigurateException {
    var loader = saveDefault();
    return objectFactory.get(type).load(loader.load());
  }

  public HoconConfigurationLoader loader() {
    return configBuilder.path(file.toPath()).build();
  }

  public ConfigurationLoader<CommentedConfigurationNode> saveDefault() throws ConfigurateException {
    var loader = loader();
    if (!file.exists()) {
      loader.save(loader.createNode().set(type, instance));
    }
    return loader;
  }
}
