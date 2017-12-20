package ua.com.juja.microservices.utils;

/**
 * @author Nikolay Horushko
 */
public class SlackUrlUtils {
    public static String getUrlTemplate(String endpoint) {
        return endpoint + "?" +
                "token={slashCommandToken}&" +
                "team_id={team_id}&" +
                "team_domain={team_domain}&" +
                "channel_id={channel_id}&" +
                "channel_name={channel_name}&" +
                "user_id={user_id}&" +
                "user_name={user_name}&" +
                "command={command}&" +
                "text={text}&" +
                "response_url={response_url}&";
    }

    public static Object[] getUriVars(String slackToken, String command, String description) {
        return new Object[]{slackToken,
                "any_team_id",
                "any_domain",
                "UHASHB8JB",
                "test-channel",
                "slack-from",
                "from-name",
                command,
                description,
                "http://example.com"};
    }

    public static Object[] getUriVars(String fromSlackUser, String slackToken, String command, String description) {
        return new Object[]{slackToken,
                "any_team_id",
                "any_domain",
                "UHASHB8JB",
                "test-channel",
                fromSlackUser,
                "from-name",
                command,
                description,
                "http://example.com"};
    }
}
