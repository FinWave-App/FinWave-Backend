package app.finwave.backend.api.ai;

import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.AiMessagesRecord;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record1;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class AiDatabase extends AbstractDatabase {
    public AiDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newContext(int userId) {
        return context.insertInto(AI_CONTEXTS)
                .set(AI_CONTEXTS.OWNER_ID, userId)
                .set(AI_CONTEXTS.CREATED_AT, OffsetDateTime.now())
                .set(AI_CONTEXTS.COMPLETION_TOKENS_USED, 0)
                .set(AI_CONTEXTS.PROMPT_TOKENS_USED, 0)
                .returningResult(AI_CONTEXTS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public boolean userOwnContext(int userId, long contextId) {
        return context.select(AI_CONTEXTS.ID)
                .from(AI_CONTEXTS)
                .where(AI_CONTEXTS.OWNER_ID.eq(userId).and(AI_CONTEXTS.ID.eq(contextId)))
                .fetchOptional()
                .isPresent();
    }

    public void addUsedTokens(long contextId, int completionTokens, int promptTokens) {
        context.update(AI_CONTEXTS)
                .set(AI_CONTEXTS.COMPLETION_TOKENS_USED, AI_CONTEXTS.COMPLETION_TOKENS_USED.plus(completionTokens))
                .set(AI_CONTEXTS.PROMPT_TOKENS_USED, AI_CONTEXTS.PROMPT_TOKENS_USED.plus(promptTokens))
                .where(AI_CONTEXTS.ID.eq(contextId))
                .execute();
    }

    public Optional<AiMessagesRecord> pushMessage(long contextId, String role, JSON content) {
        return context.insertInto(AI_MESSAGES)
                .set(AI_MESSAGES.AI_CONTEXT, contextId)
                .set(AI_MESSAGES.ROLE, role)
                .set(AI_MESSAGES.CONTENT, content)
                .returningResult(AI_MESSAGES)
                .fetchOptional()
                .map(Record1::component1);
    }

    public List<AiMessagesRecord> getMessages(long contextId) {
        return context.selectFrom(AI_MESSAGES)
                .where(AI_MESSAGES.AI_CONTEXT.eq(contextId))
                .orderBy(AI_MESSAGES.ID)
                .fetch();
    }

    public void deleteContext(long contextId) {
        context.deleteFrom(AI_MESSAGES)
                .where(AI_MESSAGES.AI_CONTEXT.eq(contextId))
                .execute();

        context.deleteFrom(AI_CONTEXTS)
                .where(AI_CONTEXTS.ID.eq(contextId))
                .execute();
    }
}