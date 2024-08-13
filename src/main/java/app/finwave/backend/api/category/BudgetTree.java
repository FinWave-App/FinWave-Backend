package app.finwave.backend.api.category;

import org.jooq.postgres.extensions.types.Ltree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class BudgetTree {
    protected ArrayList<Long> parents;

    public static BudgetTree empty() {
        return new BudgetTree("");
    }

    public static BudgetTree of(Ltree ltree) {
        return new BudgetTree(ltree.data());
    }

    protected BudgetTree(String data) {
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

    public BudgetTree append(Long parent) {
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
