package su.knst.finwave.api.transaction.tag;

import org.jooq.postgres.extensions.types.Ltree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class TagTree {
    protected ArrayList<Long> parents;

    public static TagTree empty() {
        return new TagTree("");
    }

    public static TagTree of(Ltree ltree) {
        return new TagTree(ltree.data());
    }

    protected TagTree(String data) {
        if (data.isEmpty()) {
            this.parents = new ArrayList<>();
            return;
        }

        this.parents = new ArrayList<>(
                Arrays.stream(data.split("\\."))
                .map(Long::parseLong)
                .toList()
        );
    }

    public TagTree append(Long parent) {
        parents.add(parent);

        return this;
    }

    public boolean contains(Long id) {
        return parents.contains(id);
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        Iterator<Long> iterator = parents.iterator();

        while (iterator.hasNext()) {
            Long value = iterator.next();

            builder.append(value);

            if (iterator.hasNext())
                builder.append('.');
        }

        return builder.toString();
    }

    public Ltree toLtree() {
        return Ltree.ltree(build());
    }
}
