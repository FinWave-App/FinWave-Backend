package su.knst.finwave.config;

import com.google.inject.Singleton;
import su.knst.scw.ConfigNode;
import su.knst.scw.RootConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class Configs {
    protected final Path confsPath = Path.of("./configs/");

    protected final Map<ConfigGroup, RootConfig> loadedConfigs = new HashMap<>();

    public Configs() {

    }

    protected Optional<RootConfig> getRoot(ConfigGroup group) {
        if (loadedConfigs.containsKey(group))
            return Optional.of(loadedConfigs.get(group));

        RootConfig result = null;

        try {
            result = new RootConfig(confsPath.resolve(group.toString() + ".conf").toFile(), true);
            result.load();

            loadedConfigs.put(group, result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.ofNullable(result);
    }

    public <T extends GroupedConfig> T getState(T defaultState) {
        Optional<ConfigNode> node = getRoot(defaultState.group())
                .map((r) -> r.subNode(defaultState.name()));

        if (node.isEmpty())
            return defaultState;

        Optional<T> state = node.get().getAs((Class<T>) defaultState.getClass());

        if (state.isEmpty())
            node.get().setAs(defaultState);

        return state.orElse(defaultState);
    }
}
