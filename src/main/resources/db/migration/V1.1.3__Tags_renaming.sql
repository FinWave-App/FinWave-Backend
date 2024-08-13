alter table accounts                        rename column tag_id to folder_id;

alter table accumulation_settings           rename column tag_id to category_id;
alter table recurring_transactions          rename column tag_id to category_id;
alter table transactions                    rename column tag_id to category_id;
alter table transactions_tags_management    rename column tag_id to category_id;


alter table transactions_tags rename to categories;

alter table transactions_tags_management rename to categories_budgets;

alter table accounts_tags rename to accounts_folders;