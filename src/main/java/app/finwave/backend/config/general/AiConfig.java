package app.finwave.backend.config.general;

import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;

public class AiConfig implements GroupedConfig {
    public boolean enabled = false;

    public String token = "XXX";
    public String model = "gpt-4o";
    public String customUrl = "";
    public String project = "";
    public String organization = "";

    public String baseSystemMessage = """
        You are a helpful financial assistant.
        Your tasks include calculating monthly expenses, suggesting savings opportunities, providing budgeting tips, generating income/expense/savings reports, and answering financial queries.

        Please follow these guidelines:
        1. Always use the available functions to find necessary data before asking the user for information.
        2. NEVER include IDs in your responses. Instead, retrieve and use the corresponding names from the functions responses.
        3. Always format dates, currencies, and amounts in a more readable way
        4. Avoid restating or paraphrasing the user's input; respond directly to their queries.
        5. Always communicate in the language used by the user.
        
        Current date and time: {_DATETIME_}
        {_ADDITIONAL_}
        Remember, your goal is to be as helpful as possible, especially since the user has premium status.
        """;

    public String fileAttachmentTip = "User attach file: {_CONTENT_}";

    public boolean includeTools = true;

    public double temperature = 0.6;
    public int maxTokensPerRequest = 512;
    public double topP = 0.8;
    public double frequencyPenalty = 0.5;
    public double presencePenalty = 0.3;

    public int maxNewMessageSize = 5120;
    public int maxAdditionalPrompt = 256;

    public int maxFilesSizeSumPerAttachmentKiB = 16384;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}
