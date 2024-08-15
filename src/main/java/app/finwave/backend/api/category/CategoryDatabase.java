package app.finwave.backend.api.category;

import app.finwave.backend.jooq.tables.records.CategoriesRecord;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.postgres.extensions.types.Ltree;
import app.finwave.backend.database.AbstractDatabase;

import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.CATEGORIES;

public class CategoryDatabase extends AbstractDatabase {

    public CategoryDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newCategory(int userId, short type, Long parentId, String name, String description) {
        BudgetTree tree;

        if (parentId != null){
            CategoriesRecord parent = context.selectFrom(CATEGORIES)
                    .where(CATEGORIES.ID.eq(parentId))
                    .fetchOptional()
                    .orElseThrow();

            tree = BudgetTree.of(parent.getParentsTree());
            tree.append(parentId);
        }else {
            tree = BudgetTree.empty();
        }

        return context.insertInto(CATEGORIES)
                .set(CATEGORIES.OWNER_ID, userId)
                .set(CATEGORIES.TYPE, type)
                .set(CATEGORIES.PARENTS_TREE, tree.toLtree())
                .set(CATEGORIES.NAME, name)
                .set(CATEGORIES.DESCRIPTION, description)
                .returningResult(CATEGORIES.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public List<CategoriesRecord> getCategories(int userId) {
        return context.selectFrom(CATEGORIES)
                .where(CATEGORIES.OWNER_ID.eq(userId))
                .orderBy(CATEGORIES.ID)
                .fetch();
    }

    public boolean userOwnCategory(int userId, long categoryId) {
        return context.select(CATEGORIES.ID)
                .from(CATEGORIES)
                .where(CATEGORIES.OWNER_ID.eq(userId).and(CATEGORIES.ID.eq(categoryId)))
                .fetchOptional()
                .isPresent();
    }

    public Optional<CategoriesRecord> getCategory(long categoryId) {
        return context.selectFrom(CATEGORIES)
                .where(CATEGORIES.ID.eq(categoryId))
                .fetchOptional();
    }

    public int getCategoriesCount(int userId) {
        return context.selectCount()
                .from(CATEGORIES)
                .where(CATEGORIES.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public void editCategoryType(long categoryId, short type) {
        context.update(CATEGORIES)
                .set(CATEGORIES.TYPE, type)
                .where(CATEGORIES.ID.eq(categoryId))
                .execute();
    }

    public boolean newParentIsSafe(long categoryId, long parentId) {
        BudgetTree tree = context.select(CATEGORIES.PARENTS_TREE)
                .from(CATEGORIES)
                .where(CATEGORIES.ID.eq(parentId))
                .fetchOptional()
                .map(Record1::component1)
                .map(BudgetTree::of)
                .orElseThrow();

        if (tree.contains(categoryId))
            return false;

        int categoryChildren = context.fetchCount(CATEGORIES,
                CATEGORIES.PARENTS_TREE.contains(Ltree.ltree(String.valueOf(categoryId))));

        return categoryChildren == 0;
    }

    public void editCategoryParentId(long categoryId, long parentId) {
        CategoriesRecord parent = context.selectFrom(CATEGORIES)
                .where(CATEGORIES.ID.eq(parentId))
                .fetchOptional()
                .orElseThrow();

        BudgetTree tree = BudgetTree.of(parent.getParentsTree()).append(parentId);

        context.update(CATEGORIES)
                .set(CATEGORIES.PARENTS_TREE, tree.toLtree())
                .where(CATEGORIES.ID.eq(categoryId))
                .execute();
    }

    public void setParentToRoot(long categoryId) {
        context.update(CATEGORIES)
                .set(CATEGORIES.PARENTS_TREE, BudgetTree.empty().toLtree())
                .where(CATEGORIES.ID.eq(categoryId))
                .execute();
    }

    public void editCategoryName(long categoryId, String name) {
        context.update(CATEGORIES)
                .set(CATEGORIES.NAME, name)
                .where(CATEGORIES.ID.eq(categoryId))
                .execute();
    }

    public void editCategoryDescription(long categoryId, String description) {
        context.update(CATEGORIES)
                .set(CATEGORIES.DESCRIPTION, description)
                .where(CATEGORIES.ID.eq(categoryId))
                .execute();
    }
}
