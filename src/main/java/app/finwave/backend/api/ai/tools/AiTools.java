package app.finwave.backend.api.ai.tools;

import app.finwave.backend.api.account.AccountApi;
import app.finwave.backend.api.account.folder.AccountFolderApi;
import app.finwave.backend.api.accumulation.AccumulationApi;
import app.finwave.backend.api.analytics.AnalyticsApi;
import app.finwave.backend.api.currency.CurrencyApi;
import app.finwave.backend.api.note.NoteApi;
import app.finwave.backend.api.notification.NotificationApi;
import app.finwave.backend.api.transaction.TransactionApi;
import app.finwave.backend.api.recurring.RecurringTransactionApi;
import app.finwave.backend.api.category.CategoryApi;
import app.finwave.backend.api.budget.CategoryBudgetApi;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.stefanbratanov.jvm.openai.Function;
import io.github.stefanbratanov.jvm.openai.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
public class AiTools {
    protected static final Logger log = LoggerFactory.getLogger(AiTools.class);

    protected List<Tool> tools = new ArrayList<>();
    protected HashMap<String, FunctionExecutor> functionExecutors = new HashMap<>();

    public static final GenericError NOT_FOUND = new GenericError("Function not found");

    protected TransactionApi transactionApi;
    protected CategoryApi categoryApi;
    protected RecurringTransactionApi recurringTransactionApi;
    protected AnalyticsApi analyticsApi;
    protected NoteApi noteApi;
    protected CurrencyApi currencyApi;
    protected AccumulationApi accumulationApi;
    protected AccountFolderApi accountFolderApi;
    protected AccountApi accountApi;
    protected NotificationApi notificationApi;
    protected CategoryBudgetApi categoryBudgetApi;

    @Inject
    public AiTools(TransactionApi transactionApi,
                   CategoryApi categoryApi,
                   RecurringTransactionApi recurringTransactionApi,
                   AnalyticsApi analyticsApi,
                   NoteApi noteApi,
                   CurrencyApi currencyApi,
                   AccumulationApi accumulationApi,
                   AccountFolderApi accountFolderApi,
                   AccountApi accountApi,
                   NotificationApi notificationApi,
                   CategoryBudgetApi categoryBudgetApi
    ) {
        this.transactionApi = transactionApi;
        this.categoryApi = categoryApi;
        this.recurringTransactionApi = recurringTransactionApi;
        this.analyticsApi = analyticsApi;
        this.noteApi = noteApi;
        this.currencyApi = currencyApi;
        this.accumulationApi = accumulationApi;
        this.accountFolderApi = accountFolderApi;
        this.accountApi = accountApi;
        this.notificationApi = notificationApi;
        this.categoryBudgetApi = categoryBudgetApi;

        buildTools();
    }

    public Object run(String name, UsersSessionsRecord session, Map<String, String> args) {
        FunctionExecutor executor = functionExecutors.get(name);

        if (executor == null)
            return NOT_FOUND;

        Object result;

        try {
            log.debug("AI run function: {} for user #{}", name, session.getUserId());

            result = executor.run(session, args);
        }catch (Exception e) {
            result = e.toString();
        }

        return result;
    }

    public List<Tool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    protected void buildTools() {
        function("calculate", "Use this function to calculate math expressions",
                (FunctionExecutor) (s, args) -> {
                    try {
                        return new FunctionGenericResult(new Expression(args.get("expression"))
                                .evaluate()
                                .getNumberValue()
                                .toString()
                        );
                    } catch (EvaluationException | ParseException e) {
                        return new FunctionError("Invalid expression");
                    }
                },
                Parameter.of("expression", "string", "Expression to calculate", true)
        );

        function("get_transactions", "Get user's transactions. Sorted by date, new ones first", transactionApi::getTransactions,
                Parameter.of("count", "integer", "Count of transaction to fetch", true),
                Parameter.of("offset", "integer", "Offset. 0 - without it", false),
                Parameter.of("categoryIds", "array>integer", "Filter by categories ids", false),
                Parameter.of("accountIds", "array>integer", "Filter by accounts ids", false),
                Parameter.of("currenciesIds", "array>integer", "Filter by currencies ids", false),
                Parameter.of("fromTime", "string", "Filter by time: from what moment (example: 2022-12-03T10:15:30+01:00)", false),
                Parameter.of("toTime", "string", "Filter by time: until when (format like in fromTime)", false),
                Parameter.of("description", "string", "Filter by description", false)
        );

        function("get_transactions_count", "Get user's transactions count", transactionApi::getTransactionsCount,
                Parameter.of("categoryIds", "array>integer", "Filter by category ids", false),
                Parameter.of("accountIds", "array>integer", "Filter by accounts ids", false),
                Parameter.of("currenciesIds", "array>integer", "Filter by currencies ids", false),
                Parameter.of("fromTime", "string", "Filter by time: from what moment (example: 2022-12-03T10:15:30+01:00)", false),
                Parameter.of("toTime", "string", "Filter by time: until when (format like in fromTime)", false),
                Parameter.of("description", "string", "Filter by description", false)
        );

        function("new_transaction", "Create a new transaction.", transactionApi::newTransaction,
                Parameter.of("categoryId", "integer", "One of user's category id", true),
                Parameter.of("accountId", "integer", "One of user's accounts id", true),
                Parameter.of("delta", "number", "Transaction delta. Sign must be like category type", true),
                Parameter.of("description", "string", false)
        );

        function("new_transfer", "Create a new transfer transaction (transfer from one account to another account)", transactionApi::newInternalTransfer,
                Parameter.of("categoryId", "integer", "One of user's category id", true),
                Parameter.of("fromAccountId", "integer", "User's account id from which the amount should be debited", true),
                Parameter.of("toAccountId", "integer", "User's account id where the amount should be credited", true),
                Parameter.of("fromDelta", "number", "The delta that will be debited. Must be negative", true),
                Parameter.of("toDelta", "number", "Amount to be credited. Must be positive", true),
                Parameter.of("description", "string", false)
        );

        function("edit_transaction", "Edit exists user's transaction", transactionApi::editTransaction,
                Parameter.of("transactionId", "integer", "Exists transaction id", true),
                Parameter.of("categoryId", "integer", "One of user's category id", true),
                Parameter.of("accountId", "integer", "One of user's accounts id", true),
                Parameter.of("delta", "number", "Transaction delta. Sign must be like category type", true),
                Parameter.of("description", "string", false)
        );

        function("delete_transaction", "Delete user's transaction (make sure the user wants it, this is an irrevocable operation!)", transactionApi::deleteTransaction,
                Parameter.of("transactionId", "integer", "Exists transaction id", true)
        );

        function("new_category", "Add new category for transactions", categoryApi::newCategory,
                Parameter.of("type", "integer", "Expanse, mixed or income", List.of("-1", "0", "1"), true),
                Parameter.of("parentId", "integer", "Parent category id. Set it if the new category should be a child of parentId", false),
                Parameter.of("name", "string", true),
                Parameter.of("description", "string", false)
        );

        function("get_categories", "Get all user's transaction categories. Tip: type of category means expanse (-1), mix (0) or income (1)", categoryApi::getCategories);

        function("edit_category_type", "Edit category's type (expanse, mixed or income)", categoryApi::editCategoryType,
                Parameter.of("categoryId", "integer", "One of user's categories id", true),
                Parameter.of("type", "integer", "Expanse, mixed or income", List.of("-1", "0", "1"), true)
        );

        function("edit_category_parent", "Edit category's parent", categoryApi::editCategoryParent,
                Parameter.of("categoryId", "integer", "One of user's categories id", true),
                Parameter.of("parentId", "integer", "Parent category id. Required if setToRoot parameter not set", false),
                Parameter.of("setToRoot", "boolean", "Set true if category should not inherit anyone", false)
        );

        function("edit_category_name", "Edit category's name", categoryApi::getCategories,
                Parameter.of("categoryId", "integer", "One of user's categories (category) id", true),
                Parameter.of("name", "string", true)
        );

        function("edit_category_description", "Edit category's name", categoryApi::editCategoryDescription,
                Parameter.of("categoryId", "integer", "One of user's categories (category) id", true),
                Parameter.of("description", "string", true)
        );

        function("new_recurring_transaction", "Create a new recurring transaction rule", recurringTransactionApi::newRecurringTransaction,
                Parameter.of("categoryId", "integer", "One of user's categories id", true),
                Parameter.of("accountId", "integer", "One of user's accounts id", true),
                Parameter.of("nextRepeat", "string", "Next repeat time (example: 2022-12-03T10:15:30+01:00)", true),
                Parameter.of("repeatType", "integer", "Repeat type", true),
                Parameter.of("repeatArg", "integer", "Repeat argument", true),
                Parameter.of("notificationMode", "integer", "Notification mode", true),
                Parameter.of("delta", "number", "Transaction delta. Sign must be like category type", true),
                Parameter.of("description", "string", "Description of the transaction", false)
        );

        function("edit_recurring_transaction", "Edit existing recurring transaction rule", recurringTransactionApi::editRecurringTransaction,
                Parameter.of("recurringTransactionId", "integer", "Recurring transaction id", true),
                Parameter.of("categoryId", "integer", "One of user's categories id", true),
                Parameter.of("accountId", "integer", "One of user's accounts id", true),
                Parameter.of("nextRepeat", "string", "Next repeat time (example: 2022-12-03T10:15:30+01:00)", true),
                Parameter.of("repeatType", "integer", "Repeat type", true),
                Parameter.of("repeatArg", "integer", "Repeat argument", true),
                Parameter.of("notificationMode", "integer", "Notification mode", true),
                Parameter.of("delta", "number", "Transaction delta. Sign must be like category type", true),
                Parameter.of("description", "string", "Description of the transaction", false)
        );

        function("delete_recurring_transaction", "Delete recurring transaction rule", recurringTransactionApi::deleteRecurringTransaction,
                Parameter.of("recurringId", "integer", "Recurring transaction id", true)
        );

        function("get_recurring_transactions", "Get all user's recurring transactions rules", recurringTransactionApi::getList);

        function("get_analytics_by_months", "Get analytics summary by months (use it if you need to summarize transactions by month)", analyticsApi::getAnalyticsByMonths,
                Parameter.of("categoriesIds", "array>integer", "Filter by categories ids", false),
                Parameter.of("accountIds", "array>integer", "Filter by accounts ids", false),
                Parameter.of("currenciesIds", "array>integer", "Filter by currencies ids", false),
                Parameter.of("fromTime", "string", "Filter by time: from what moment (example: 2022-12-03T10:15:30+01:00)", false),
                Parameter.of("toTime", "string", "Filter by time: until when (format like in fromTime)", false),
                Parameter.of("description", "string", "Filter by description", false)
        );

        function("get_analytics_by_days", "Get analytics summary by days (use it if you need to summarize transactions by days)", analyticsApi::getAnalyticsByDays,
                Parameter.of("categoriesIds", "array>integer", "Filter by categories ids", false),
                Parameter.of("accountIds", "array>integer", "Filter by accounts ids", false),
                Parameter.of("currenciesIds", "array>integer", "Filter by currencies ids", false),
                Parameter.of("fromTime", "string", "Filter by time: from what moment (example: 2022-12-03T10:15:30+01:00)", false),
                Parameter.of("toTime", "string", "Filter by time: until when (format like in fromTime)", false),
                Parameter.of("description", "string", "Filter by description", false)
        );

        function("get_notification_points", "Get all user's notification points", notificationApi::getPoints);

        function("push_notification", "Send a push notification", notificationApi::pushNotification,
                Parameter.of("pointId", "integer", "Notification point id", true),
                Parameter.of("text", "string", "Notification text", true),
                Parameter.of("silent", "boolean", "Silent notification", true)
        );

        function("new_note", "Create a new note", noteApi::newNote,
                Parameter.of("notificationTime", "string", "Notification time (example: 2022-12-03T10:15:30+01:00)", false),
                Parameter.of("text", "string", "Text of the note", true)
        );

        function("edit_note", "Edit an existing note", noteApi::editNote,
                Parameter.of("noteId", "integer", "Note id", true),
                Parameter.of("text", "string", "New text of the note", true)
        );

        function("edit_note_time", "Edit note's notification time", noteApi::editNoteNotificationTime,
                Parameter.of("noteId", "integer", "Note id", true),
                Parameter.of("notificationTime", "string", "New notification time (example: 2022-12-03T10:15:30+01:00)", true)
        );

        function("get_notes", "Get all user's notes", noteApi::getNotesList);

        function("get_important_notes", "Get user's important notes", noteApi::getImportantNotes);

        function("delete_note", "Delete a note", noteApi::deleteNote,
                Parameter.of("noteId", "integer", "Note id", true)
        );

        function("new_currency", "Create a new currency", currencyApi::newCurrency,
                Parameter.of("code", "string", "Currency code", true),
                Parameter.of("symbol", "string", "Currency symbol", true),
                Parameter.of("decimals", "integer", "Number of decimals", true),
                Parameter.of("description", "string", "Description of the currency", true)
        );

        function("get_currencies", "Get all user's currencies", currencyApi::getCurrencies);

        function("edit_currency_code", "Edit currency code", currencyApi::editCurrencyCode,
                Parameter.of("currencyId", "integer", "Currency id", true),
                Parameter.of("code", "string", "New currency code", true)
        );

        function("edit_currency_symbol", "Edit currency symbol", currencyApi::editCurrencySymbol,
                Parameter.of("currencyId", "integer", "Currency id", true),
                Parameter.of("symbol", "string", "New currency symbol", true)
        );

        function("edit_currency_decimals", "Edit currency decimals", currencyApi::editCurrencyDecimals,
                Parameter.of("currencyId", "integer", "Currency id", true),
                Parameter.of("decimals", "integer", "New decimals value", true)
        );

        function("edit_currency_description", "Edit currency description", currencyApi::editCurrencyDescription,
                Parameter.of("currencyId", "integer", "Currency id", true),
                Parameter.of("description", "string", "New currency description", true)
        );

        function("remove_accumulation", "Remove accumulation configuration for an account", accumulationApi::removeAccumulation,
                Parameter.of("accountId", "integer", "Account id", true)
        );

        function("get_accumulations", "Get all user's accumulations", accumulationApi::getList);

        function("new_account_folder", "Add new folder for accounts", accountFolderApi::newFolder,
                Parameter.of("name", "string", "Name of the new folder", true),
                Parameter.of("description", "string", "Description of the new folder", false)
        );

        function("get_account_folders", "Get all user's account's folders", accountFolderApi::getFolders);

        function("edit_account_folder_name", "Edit account's folder's name", accountFolderApi::editFolderName,
                Parameter.of("folderId", "integer", "Account folder id", true),
                Parameter.of("name", "string", "New name of the account folder", true)
        );

        function("edit_account_folder_description", "Edit account's folder's description", accountFolderApi::editFolderDescription,
                Parameter.of("folderId", "integer", "Account folder id", true),
                Parameter.of("description", "string", "New description of the account folder", true)
        );

        function("delete_account_folder", "Delete account's folder", accountFolderApi::deleteFolder,
                Parameter.of("folderId", "integer", "Account folder id", true)
        );

        function("new_account", "Create a new account", accountApi::newAccount,
                Parameter.of("folderId", "integer", "One of user's account's folder id", true),
                Parameter.of("currencyId", "integer", "Currency id", true),
                Parameter.of("name", "string", "Name of the new account", true),
                Parameter.of("description", "string", "Description of the new account", false)
        );

        function("get_accounts", "Get all user's accounts", accountApi::getAccounts);

        function("hide_account", "Hide an account", accountApi::hideAccount,
                Parameter.of("accountId", "integer", "Account id", true)
        );

        function("show_account", "Show a hidden account", accountApi::showAccount,
                Parameter.of("accountId", "integer", "Account id", true)
        );

        function("edit_account_name", "Edit account's name", accountApi::editAccountName,
                Parameter.of("accountId", "integer", "Account id", true),
                Parameter.of("name", "string", "New name of the account", true)
        );

        function("edit_account_description", "Edit account's description", accountApi::editAccountDescription,
                Parameter.of("accountId", "integer", "Account id", true),
                Parameter.of("description", "string", "New description of the account", true)
        );

        function("edit_account_folder", "Edit account's folder", accountApi::editAccountFolder,
                Parameter.of("accountId", "integer", "Account id", true),
                Parameter.of("folderId", "integer", "New folder id", true)
        );

        function("add_category_budget", "Add new category budget", categoryBudgetApi::addBudget,
                Parameter.of("categoryId", "integer", "Transaction category id", true),
                Parameter.of("currencyId", "integer", "Currency id for budget", true),
                Parameter.of("dateType", "integer", "Date type (0 - per month, 1 - per quarter)", List.of("0", "1"), true),
                Parameter.of("amount", "number", "Expected income or expense (set negative sign for expenses)", true)
        );

        function("edit_category_budget", "Edit existing category's budget", categoryBudgetApi::editBudget,
                Parameter.of("budgetId", "integer", "Budget id", true),
                Parameter.of("categoryId", "integer", "New budget's category id", true),
                Parameter.of("currencyId", "integer", "New currency id for budget", true),
                Parameter.of("dateType", "integer", "Date type (0 - per month, 1 - per quarter)", List.of("0", "1"), true),
                Parameter.of("amount", "string", "New amount for budget", true)
        );

        function("get_budget_settings", "Get all categories budgets", categoryBudgetApi::getSettings);

        function("remove_category_budget", "Remove existing category's budget", categoryBudgetApi::remove,
                Parameter.of("budgetId", "integer", "Budget id", true)
        );
    }

    protected void function(String name, String description, FunctionExecutor executor, Parameter... parameters) {
        functionExecutors.put(name, executor);

        tools.add(Tool.functionTool(Function.newBuilder()
                .name(name)
                .description(description)
                .parameters(wrapParameters(parameters))
                .build())
        );
    }

    protected void function(String name, String description, FunctionApiExecutor executor, Parameter... parameters) {
        function(name, description, (FunctionExecutor) executor, parameters);
    }

    protected static Map<String, Object> wrapParameters(Parameter... parameters) {
        HashMap<String, Object> properties = new HashMap<>();
        ArrayList<String> required = new ArrayList<>();

        for (Parameter parameter : parameters) {
            properties.put(parameter.name, parameter.toSchema());

            if (parameter.required)
                required.add(parameter.name);
        }

        HashMap<String, Object> result = new HashMap<>(Map.of(
                "type", "object", // always accepting object as arguments for function
                "properties", properties
        ));

        if (!required.isEmpty())
            result.put("required", required);

        return result;
    }

    public static class Parameter {
        public String name;
        public String type;
        public String description;
        public List<String> enumVariants;
        public boolean required;

        public Parameter(String name, String type, String description, List<String> enumVariants, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.enumVariants = enumVariants;
            this.required = required;
        }

        public Object toSchema() {
            HashMap<String, Object> schema = new HashMap<>();

            if (type.startsWith("array>")) {
                String[] types = type.split(">");

                schema.put("type", types[0]);
                schema.put("items", Map.of("type", types[1]));
            }else
                schema.put("type", type);

            if (description != null)
                schema.put("description", description);

            if (enumVariants != null)
                schema.put("enum", enumVariants);

            return schema;
        }

        public static Parameter of(String name, String type, String description, List<String> enumVariants, boolean required) {
            return new Parameter(name, type, description, enumVariants, required);
        }

        public static Parameter of(String name, String type, String description, boolean required) {
            return of(name, type, description, null, required);
        }

        public static Parameter of(String name, String type, boolean required) {
            return of(name, type, null, null, required);
        }

        public static Parameter of(String name, List<String> enumVariants, boolean required) {
            return of(name, "string", null, enumVariants, required);
        }
    }
}
