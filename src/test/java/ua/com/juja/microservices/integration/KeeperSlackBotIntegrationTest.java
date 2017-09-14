package ua.com.juja.microservices.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.KeeperSlackBotApplication;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {KeeperSlackBotApplication.class})
@AutoConfigureMockMvc
public class KeeperSlackBotIntegrationTest {

    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private static final String EXAMPLE_URL = "http://example.com";
    private static final String TOKEN_WRONG = "wrongSlackToken";
    private static final String FROM_USER_SLACK_NAME_WITHOUT_AT = "from-user";

    @Value("${keepers.slackBot.slack.slashCommandToken}")
    private String tokenCorrect;

    @Inject
    private RestTemplate restTemplate;

    @Inject
    private MockMvc mvc;

    private MockRestServiceServer mockServer;

    @Value("${keepers.baseURL}")
    private String urlBaseKeepers;
    @Value("${keepers.rest.api.version}")
    private String keepersVersion;
    @Value("${keepers.endpoint.keepers}")
    private String urlKeepers;

    @Value("${users.baseURL}")
    private String urlBaseUsers;
    @Value("${users.rest.api.version}")
    private String usersVersion;
    @Value("${users.endpoint.usersBySlackNames}")
    private String urlGetUsers;

    @Value("${keepers.slackBot.rest.api.version}")
    private String slackBotVersion;

    private UserDTO userFrom = new UserDTO("f2034f11-561a-4e01-bfcf-ec615c1ba61a", "@from-user");
    private UserDTO user1 = new UserDTO("f2034f22-562b-4e02-bfcf-ec615c1ba62b", "@slack1");
    private UserDTO user2 = new UserDTO("f2034f33-563c-4e03-bfcf-ec615c1ba63c", "@slack2");

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void onReceiveSlashCommandKeeperAddSendOkRichMessage() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String EXPECTED_RESPONSE_FROM_KEEPERS = "[\"1000\"]";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Thanks, we added a new Keeper: @slack1 in direction: teams\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.POST, EXPECTED_REQUEST_TO_KEEPERS,
                EXPECTED_RESPONSE_FROM_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperAddWhenFromUserSlackNameWithoutATSendOkRichMessage() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String EXPECTED_RESPONSE_FROM_KEEPERS = "[\"1000\"]";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Thanks, we added a new Keeper: @slack1 in direction: teams\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.POST,
                EXPECTED_REQUEST_TO_KEEPERS, EXPECTED_RESPONSE_FROM_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(FROM_USER_SLACK_NAME_WITHOUT_AT, tokenCorrect, "/keeper-add",
                        KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }


    @Test
    public void onReceiveSlashCommandKeeperAddIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper-add", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandConsistTwoOrMoreSlackNames() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandConsistTwoOrMoreDirections() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams else";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command: 'teams else' " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandWithNoConsistDirections() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command: '@slack1' " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnClientErrorMessageForKeeperAddWhenUserServiceIsFail() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnClientErrorMessageForKeeperAddWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.POST, EXPECTED_REQUEST_TO_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void keeperAddWhenSlackAnsweredFail() throws Exception {
        //Given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperDeactivateReturnOkRichMessage() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String EXPECTED_RESPONSE_FROM_KEEPERS = "[\"1000\"]";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Keeper: @slack1 in direction: teams deactivated\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.PUT, EXPECTED_REQUEST_TO_KEEPERS,
                EXPECTED_RESPONSE_FROM_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperDeactivateIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper-deactivate", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandConsistTwoOrMoreSlackNames() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandConsistTwoOrMoreDirections() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 teams else";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command: 'teams else' " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandWithNoConsistDirections() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command: '@slack1' " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnClientErrorMessageForKeeperDeactivateWhenUserServiceIsFail() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnClientErrorMessageForKeeperDeactivateWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String KEEPER_DISMISS_COMMAND_TEXT = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.PUT, EXPECTED_REQUEST_TO_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DISMISS_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void keeperDeactivateWhenSlackAnsweredFail() throws Exception {
        //Given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsReturnOkRichMessage() throws Exception {
        //Given
        final String GET_DIRECTIONS_COMMAND = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"\"" +
                "}";
        final String EXPECTED_RESPONSE_FROM_KEEPERS = "[\"direction1, direction2\"]";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @slack1 has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(), HttpMethod.GET,
                EXPECTED_REQUEST_TO_KEEPERS, EXPECTED_RESPONSE_FROM_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", GET_DIRECTIONS_COMMAND))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsReturnEmptyRichMessage() throws Exception {
        //Given
        final String GET_DIRECTIONS_COMMAND = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"\"" +
                "}";
        final String EXPECTED_RESPONSE_FROM_KEEPERS = "[]";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @slack1 has no active directions.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(), HttpMethod.GET,
                EXPECTED_REQUEST_TO_KEEPERS, EXPECTED_RESPONSE_FROM_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", GET_DIRECTIONS_COMMAND))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));
    }

    @Test
    public void returnClientErrorMessageForKeeperGetDirectionsWhenUserServiceIsFail() throws Exception {
        //Given
        final String GET_DIRECTIONS_COMMAND = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", GET_DIRECTIONS_COMMAND))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    @Test
    public void returnClientErrorMessageForKeeperGetDirectionsWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String GET_DIRECTIONS_COMMAND = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String EXPECTED_REQUEST_TO_KEEPERS = "{" +
                "\"from\":\"f2034f11-561a-4e01-bfcf-ec615c1ba61a\"," +
                "\"uuid\":\"f2034f22-562b-4e02-bfcf-ec615c1ba62b\"," +
                "\"direction\":\"\"" +
                "}";
        final String EXPECTED_REQUEST_TO_SLACK = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        //When
        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(),
                HttpMethod.GET, EXPECTED_REQUEST_TO_KEEPERS);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, EXPECTED_REQUEST_TO_SLACK);

        //Then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", GET_DIRECTIONS_COMMAND))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
    }

    private void mockFailUsersService(List<UserDTO> users) throws JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (UserDTO user : users) {
            slackNames.add(user.getSlack());
        }
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(urlBaseUsers + usersVersion + urlGetUsers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));
    }

    private void mockFailKeepersService(String expectedURI, HttpMethod method, String expectedRequestBody) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withBadRequest().body("{\"httpStatus\":400,\"internalErrorCode\":1," +
                        "\"clientMessage\":\"Oops something went wrong :(\"," +
                        "\"developerMessage\":\"General exception for this service\"," +
                        "\"exceptionMessage\":\"very big and scare error\",\"detailErrors\":[]}"));
    }

    private void mockFailSlack(String expectedURI, HttpMethod method, String expectedRequestBody) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withBadRequest());
    }

    private void mockSuccessUsersService(List<UserDTO> users) throws JsonProcessingException {
        List<String> slackNames = new ArrayList<>();
        for (UserDTO user : users) {
            slackNames.add(user.getSlack());
        }
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(urlBaseUsers + usersVersion + urlGetUsers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(String.format("{\"slackNames\":%s}", mapper.writeValueAsString(slackNames))))
                .andRespond(withSuccess(mapper.writeValueAsString(users), MediaType.APPLICATION_JSON_UTF8));
    }

    private void mockSuccessKeepersService(String expectedURI, HttpMethod method, String expectedRequestBody, String response) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private void mockSuccessSlack(String expectedURI, HttpMethod method, String expectedRequestBody) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withSuccess().body("OK"));
    }
}

