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
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {KeeperSlackBotApplication.class})
@AutoConfigureMockMvc
public class KeeperSlackBotIntegrationTest {

    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private static final String EXAMPLE_URL = "http://example.com";
    private static final String TOKEN_WRONG = "wrongSlackToken";

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
    @Value("${users.endpoint.usersBySlackUsers}")
    private String urlGetUsers;

    @Value("${keepers.slackBot.rest.api.version}")
    private String slackBotVersion;

    private UserDTO userFrom = new UserDTO("uuid-from", "slack-from");
    private UserDTO user1 = new UserDTO("uuid1", "slack1");
    private UserDTO user2 = new UserDTO("uuid2", "slack2");

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void onReceiveSlashCommandKeeperAddSendOkRichMessage() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String expectedResponseFromKeepers = "[\"1000\"]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Thanks, we added a new Keeper: %s in direction: teams\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.POST, expectedRequestToKeepers,
                expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperAddIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper-add", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandConsistsTwoOrMoreSlackUsers() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s %s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user2.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack users in your command '%s'. " +
                "You can not perform actions with several slack users.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", keeperAddCommandText);

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandConsistTwoOrMoreDirections() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s teams else",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command 'teams else'. " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperAddCommandWithNoConsistDirections() throws Exception {
        //Given
        final String keeperAddCommandText = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command '%s'. " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperAddWhenUserServiceIsFail() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperAddWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.POST, expectedRequestToKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void keeperAddWhenSlackAnsweredFail() throws Exception {
        //Given
        final String keeperAddCommandText = String.format("%s %s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user2.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack users in your command '%s'. " +
                "You can not perform actions with several slack users.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", keeperAddCommandText);

        mockSuccessUsersService(usersInCommand);
        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperDeactivateReturnOkRichMessage() throws Exception {
        //Given
        final String keeperDeactivateCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String expectedResponseFromKeepers = "[\"1000\"]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Keeper: %s in direction: teams deactivated\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.PUT, expectedRequestToKeepers,
                expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperDeactivateIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper-deactivate", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandConsistTwoOrMoreSlackUsers() throws Exception {
        //Given
        final String keeperDeactivateCommandText = String.format("%s %s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user2.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack users in your command '%s'. " +
                "You can not perform actions with several slack users.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", keeperDeactivateCommandText);

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandConsistTwoOrMoreDirections() throws Exception {
        //Given
        final String keeperDeactivateCommandText = String.format("%s teams else",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command 'teams else'. " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnErrorMessageIfKeeperDeactivateCommandWithNoConsistDirections() throws Exception {
        //Given
        final String keeperDeactivateCommandText = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command '%s'. " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperDeactivateWhenUserServiceIsFail() throws Exception {
        //Given
        final String keeperDeactivateCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperDeactivateWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String keeperDismissCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"teams\"" +
                "}";
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers, HttpMethod.PUT, expectedRequestToKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDismissCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void keeperDeactivateWhenSlackAnsweredFail() throws Exception {
        //Given
        final String keeperDeactivateCommandText = String.format("%s %s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user2.getSlackUser()));
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack users in your command '%s'. " +
                "You can not perform actions with several slack users.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", keeperDeactivateCommandText);

        mockSuccessUsersService(usersInCommand);
        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsWhenFromUserNotInTextReturnOkRichMessage() throws Exception {
        //Given
        final String getDirectionsCommand = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedResponseFromKeepers = "[\"direction1, direction2\"]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper %s has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(), HttpMethod.GET,
                expectedRequestToKeepers, expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getDirectionsCommand))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsWhenFromUserInTextReturnErrorRichMessage() throws Exception {
        //Given
        final String getDirectionsCommand = SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(userFrom, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Your own slack in command. To get your own directions use another command\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getDirectionsCommand))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsWhenFromUserNotInTextReturnEmptyRichMessage() throws Exception {
        //Given
        final String getDirectionsCommand = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedResponseFromKeepers = "[]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper %s has no active directions.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(), HttpMethod.GET,
                expectedRequestToKeepers, expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getDirectionsCommand))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetDirectionsIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/keeper", "AnyText"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperGetDirectionsWhenUserServiceIsFail() throws Exception {
        //Given
        final String getDirectionsCommand = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getDirectionsCommand))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperGetDirectionsWhenKeepersServiceIsFail() throws Exception {
        //Given
        final String getDirectionsCommand = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid1\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + user1.getUuid(),
                HttpMethod.GET, expectedRequestToKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getDirectionsCommand))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }


    @Test
    public void onReceiveSlashCommandKeeperGetMyDirectionsReturnOkRichMessage() throws Exception {
        //Given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid-from\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedResponseFromKeepers = "[\"direction1, direction2\"]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper %s has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + userFrom.getUuid(),
                HttpMethod.GET, expectedRequestToKeepers, expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(slackBotVersion + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/my-directions", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetMyDirectionsWhenFromUserNotAKeeperReturnEmptyRichMessage() throws
            Exception {
        //Given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid-from\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedResponseFromKeepers = "[]";
        final String expectedRequestToSlack = String.format("{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper %s has no active directions.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}", SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser()));

        mockSuccessUsersService(usersInCommand);
        mockSuccessKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + userFrom.getUuid(),
                HttpMethod.GET, expectedRequestToKeepers, expectedResponseFromKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(slackBotVersion + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/my-directions", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void onReceiveSlashCommandKeeperGetMyDirectionsIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(slackBotVersion
                        + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/my-directions", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperGetMyDirectionsWhenUserServiceIsFail() throws Exception {
        //Given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockFailUsersService(usersInCommand);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(slackBotVersion + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/my-directions", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    @Test
    public void returnClientErrorMessageForKeeperGetMyDirectionsWhenKeepersServiceIsFail() throws Exception {
        //Given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        final String expectedRequestToKeepers = "{" +
                "\"from\":\"uuid-from\"," +
                "\"uuid\":\"uuid-from\"," +
                "\"direction\":\"\"" +
                "}";
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Oops something went wrong :(\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        mockSuccessUsersService(usersInCommand);
        mockFailKeepersService(urlBaseKeepers + keepersVersion + urlKeepers + "/" + userFrom.getUuid(),
                HttpMethod.GET, expectedRequestToKeepers);
        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //When
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(slackBotVersion + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/my-directions", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));

        //Then
        mockServer.verify();
    }

    private void mockFailUsersService(List<UserDTO> users) throws JsonProcessingException {
        List<String> slackUsers = new ArrayList<>();
        for (UserDTO user : users) {
            slackUsers.add(user.getSlackUser());
        }
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(urlBaseUsers + usersVersion + urlGetUsers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(String.format("{\"slackIds\":%s}", mapper.writeValueAsString(slackUsers))))
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
        List<String> slackUsers = new ArrayList<>();
        for (UserDTO user : users) {
            slackUsers.add(user.getSlackUser());
        }
        ObjectMapper mapper = new ObjectMapper();
        mockServer.expect(requestTo(urlBaseUsers + usersVersion + urlGetUsers))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().string(String.format("{\"slackIds\":%s}", mapper.writeValueAsString
                        (slackUsers))))
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

