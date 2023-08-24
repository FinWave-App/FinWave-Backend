package su.knst.finwave.api.transaction.tag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.postgres.extensions.types.Ltree;
import su.knst.finwave.database.Database;
import su.knst.finwave.jooq.tables.records.TransactionsTagsRecord;

import java.util.List;
import java.util.Optional;

import static su.knst.finwave.jooq.Tables.TRANSACTIONS_TAGS;

@Singleton
public class TransactionTagDatabase {

    protected DSLContext context;

    @Inject
    public TransactionTagDatabase(Database database) {
        this.context = database.context();
    }

    public Optional<Long> newTag(int userId, short type, Long parentId, String name, String description) {
        TagTree tree;

        if (parentId != null){
            TransactionsTagsRecord parent = context.selectFrom(TRANSACTIONS_TAGS)
                    .where(TRANSACTIONS_TAGS.ID.eq(parentId))
                    .fetchOptional()
                    .orElseThrow();

            tree = TagTree.of(parent.getParentsTree());
            tree.append(parentId);
        }else {
            tree = TagTree.empty();
        }

        return context.insertInto(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.OWNER_ID, userId)
                .set(TRANSACTIONS_TAGS.TYPE, type)
                .set(TRANSACTIONS_TAGS.PARENTS_TREE, tree.toLtree())
                .set(TRANSACTIONS_TAGS.NAME, name)
                .set(TRANSACTIONS_TAGS.DESCRIPTION, description)
                .returningResult(TRANSACTIONS_TAGS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public List<TransactionsTagsRecord> getTags(int userId) {
        return context.selectFrom(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId))
                .orderBy(TRANSACTIONS_TAGS.ID)
                .fetch();
    }

    public boolean userOwnTag(int userId, long tagId) {
        return context.select(TRANSACTIONS_TAGS.ID)
                .from(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId).and(TRANSACTIONS_TAGS.ID.eq(tagId)))
                .fetchOptional()
                .isPresent();
    }

    public int getTagsCount(int userId) {
        return context.selectCount()
                .from(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public void editTagType(long tagId, short type) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.TYPE, type)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public boolean newParentIsSafe(long tagId, long parentId) {
        TagTree tree = context.select(TRANSACTIONS_TAGS.PARENTS_TREE)
                .from(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.ID.eq(parentId))
                .fetchOptional()
                .map(Record1::component1)
                .map(TagTree::of)
                .orElseThrow();

        if (tree.contains(tagId))
            return false;

        int tagChilds = context.fetchCount(TRANSACTIONS_TAGS,
                TRANSACTIONS_TAGS.PARENTS_TREE.contains(Ltree.ltree(String.valueOf(tagId))));

        return tagChilds == 0;
    }

    public void editTagParentId(long tagId, long parentId) {
        TransactionsTagsRecord parent = context.selectFrom(TRANSACTIONS_TAGS)
                .where(TRANSACTIONS_TAGS.ID.eq(parentId))
                .fetchOptional()
                .orElseThrow();

        TagTree tree = TagTree.of(parent.getParentsTree()).append(parentId);

        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.PARENTS_TREE, tree.toLtree())
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void setParentToRoot(long tagId) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.PARENTS_TREE, TagTree.empty().toLtree())
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagName(long tagId, String name) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.NAME, name)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }

    public void editTagDescription(long tagId, String description) {
        context.update(TRANSACTIONS_TAGS)
                .set(TRANSACTIONS_TAGS.DESCRIPTION, description)
                .where(TRANSACTIONS_TAGS.ID.eq(tagId))
                .execute();
    }
}
