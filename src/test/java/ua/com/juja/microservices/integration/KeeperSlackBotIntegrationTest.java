package ua.com.juja.microservices.integration;

import feign.FeignException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.KeeperSlackBotApplication;
import ua.com.juja.microservices.keepers.slackbot.dao.feign.KeepersClient;
import ua.com.juja.microservices.keepers.slackbot.dao.feign.UsersClient;
import ua.com.juja.microservices.keepers.slackbot.model.dto.SlackNameRequest;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {KeeperSlackBotApplication.class})
@AutoConfigureMockMvc
public class KeeperSlackBotIntegrationTest {

    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private static final String EXAMPLE_URL = "http://example.com";
    private static final String FROM_USER_SLACK_NAME_WITHOUT_AT = "from-user";

    @Value("${slack.slashCommandToken}")
    private String tokenCorrect;

    @Inject
    private RestTemplate restTemplate;

    @Inject
    private MockMvc mvc;

    private MockRestServiceServer mockServer;

    private UserDTO userFrom = new UserDTO("f2034f11-561a-4e01-bfcf-ec615c1ba61a", "@from-user");
    private UserDTO user1 = new UserDTO("f2034f22-562b-4e02-bfcf-ec615c1ba62b", "@slack1");
    private UserDTO user2 = new UserDTO("f2034f33-563c-4e03-bfcf-ec615c1ba63c", "@slack2");

    @MockBean
    private KeepersClient keepersClient;
    @MockBean
    private UsersClient usersClient;

    private String keepersSlackbotAddKeeperUrl = "/v1/commands/keeper/add";
    private String keepersSlackbotDeactivateKeeperUrl = "/v1/commands/keeper/deactivate";
    private String keepersSlackbotGetKeeperDirectionsUrl = "/v1/commands/keeper";
    private String keepersSlackbotGetMyDirectionsUrl = "/v1/commands/keeper/myDirections";

    @Before
    public void setup() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    public void onReceiveAllSlashCommandsWhenTokenIsIncorrectShouldReturnErrorMessage() throws Exception {
        String commandText = user1.getSlack();
        String responseUrl = "http:/example.com";
        List<String> urls = Arrays.asList(
                keepersSlackbotAddKeeperUrl,
                keepersSlackbotDeactivateKeeperUrl,
                keepersSlackbotGetKeeperDirectionsUrl,
                keepersSlackbotGetMyDirectionsUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(url),
                        SlackUrlUtils.getUriVars("wrongToken", "/command", commandText, responseUrl))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(MockMvcResultMatchers.content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenAllCorrectSendOkRichMessage() throws Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Thanks, we added a new Keeper: @slack1 in direction: teams\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] addedKeeperIDs = {"101"};
        when(keepersClient.addKeeper(captorKeeperRequest.capture())).thenReturn(addedKeeperIDs);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).addKeeper(captorKeeperRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperFromUserSlackNameWithoutATSendOkRichMessage() throws Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Thanks, we added a new Keeper: @slack1 in direction: teams\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] addedKeeperIDs = {"101"};
        when(keepersClient.addKeeper(captorKeeperRequest.capture())).thenReturn(addedKeeperIDs);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(FROM_USER_SLACK_NAME_WITHOUT_AT, tokenCorrect, "/keeper-add",
                        addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).addKeeper(captorKeeperRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenCommandConsistsOfTwoOrMoreSlackNamesSendErrorRichMessage() throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenCommandConsistsOfTwoOrMoreDirectionsSendErrorRichMessage() throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1 direction1 direction2 ";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command: 'direction1 direction2' " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenCommandContainZeroDirectionsSendErrorRichMessage() throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command: '@slack1' " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenUserServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something wrong on User server\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading UsersClient#findUsersBySlackNames(); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenUserServiceReturnErrorWithIncorrectContentSendErrorRichMessage()
            throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something went wrong\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading UsersClient#findUsersBySlackNames(); content: \n";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenUserServiceReturnErrorWithoutContentSendErrorRichMessage()
            throws Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something went wrong\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService = "Something went wrong";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenKeepersServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String addKeeperCommandText = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromKeepersService =
                "status 400 reading KeepersClient#addKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromKeepersService);
        when(keepersClient.addKeeper(captorKeeperRequest.capture())).thenThrow(feignException);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).addKeeper(captorKeeperRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandAddKeeperWhenSlackAnsweredFail() throws Exception {
        //given
        final String addKeeperCommandText = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", addKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenAllCorrectSendOkRichMessage() throws Exception {
        //given
        final String deacivateKeeperCommandText = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Keeper: @slack1 in direction: teams deactivated\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] deactivatedKeeperIDs = {"101"};
        when(keepersClient.deactivateKeeper(captorKeeperRequest.capture())).thenReturn(deactivatedKeeperIDs);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deacivate", deacivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).deactivateKeeper(captorKeeperRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenCommandConsistsOfTwoOrMoreSlackNamesSendErrorRichMessage()
            throws Exception {
        //given
        final String deacivateKeeperCommandText = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack(), user2.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deacivate", deacivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);

    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenCommandConsistsOfTwoOrMoreDirectionsSendErrorRichMessage()
            throws Exception {
        //given
        final String deacivateKeeperCommandText = "@slack1 direction1 direction2";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found several directions in your command: 'direction1 direction2' " +
                "You can perform the action with keepers on one direction only.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deacivate", deacivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenCommandContainsZeroDirectionsSendErrorRichMessage()
            throws Exception {
        //given
        final String deacivateKeeperCommandText = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We didn't find direction in your command: '@slack1' " +
                "You must write the direction to perform the action with keepers.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deacivate", deacivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenUserServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String deactivateKeeperCommandText = "@slack1 teams";
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something wrong on User server\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading UsersClient#findUsersBySlackNames(); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", deactivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenKeepersServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String deactivateKeeperCommandText = "@slack1 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromKeepersService =
                "status 400 reading KeepersClient#addKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromKeepersService);
        when(keepersClient.deactivateKeeper(captorKeeperRequest.capture())).thenThrow(feignException);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", deactivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).deactivateKeeper(captorKeeperRequest.capture());
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandDeactivateKeeperWhenSlackAnsweredFail() throws Exception {
        //given
        final String deactivateKeeperCommandText = "@slack1 @slack2 teams";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, user2, userFrom);
        final List<String> slackNames = Arrays.asList(user1.getSlack(), user2.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"We found 2 slack names in your command: '@slack1 @slack2 teams' " +
                "You can not perform actions with several slack names.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockFailSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", deactivateKeeperCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(usersClient);
    }

    @Test
    public void onReceiveSlashCommandGetKeepersDirectionsWhenFromUserNotInTextSendOkRichMessage() throws Exception {
        //given
        final String getKeepersDirectionsCommandText = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @slack1 has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] directions = {"direction1", "direction2"};
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid()))).thenReturn
                (directions);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getKeepersDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetKeepersDirectionsWhenFromUserInTextSendErrorRichMessage() throws Exception {
        //given
        final String getKeepersDirectionsCommandText = "@from-user";
        final List<UserDTO> usersInCommand = Arrays.asList(userFrom, userFrom);
        final List<String> slackNames = Arrays.asList(userFrom.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Your own slackname in command. To get your own directions use another command\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getKeepersDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetKeepersDirectionsWhenFromUserNotInTextSendEmptyRichMessage() throws Exception {
        //given
        final String getKeepersDirectionsCommandText = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @slack1 has no active directions.\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] directions = {};
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid()))).thenReturn
                (directions);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getKeepersDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetKeeperDirectionsWhenUserServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String getKeepersDirectionsCommandText = "@slack1 teams";
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something wrong on User server\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading UsersClient#findUsersBySlackNames(); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getKeepersDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetKeeperDirectionsWhenKeepersServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final String getKeepersDirectionsCommandText = "@slack1";
        final List<UserDTO> usersInCommand = Arrays.asList(user1, userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "");
        final List<String> slackNames = Arrays.asList(user1.getSlack(), userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromKeepersService =
                "status 400 reading KeepersClient#addKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromKeepersService);
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid())))
                .thenThrow(feignException);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", getKeepersDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(user1.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyDirectionsWhenAllCorrectSendOkRichMessage() throws Exception {
        //given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), userFrom.getUuid(), "");
        final List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @from-user has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] directions = {"direction1", "direction2"};
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()))).thenReturn
                (directions);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyDirectionsWhenFromUserWithoutAtSendOkRichMessage() throws Exception {
        //given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), userFrom.getUuid(), "");
        final List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"The keeper @from-user has active directions: [direction1, direction2]\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";

        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        String[] directions = {"direction1", "direction2"};
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()))).thenReturn
                (directions);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars("from-user", tokenCorrect, "/keeper", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });
        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyDirectionsWhenUserServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"Something wrong on User server\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromUserService =
                "status 400 reading UsersClient#findUsersBySlackNames(); content:" +
                        "{\n" +
                        "  \"httpStatus\": 400,\n" +
                        "  \"internalErrorCode\": \"TMF-F1-D3\",\n" +
                        "  \"clientMessage\": \"Sorry, User server return an error\",\n" +
                        "  \"developerMessage\": \"Exception - UserExchangeException\",\n" +
                        "  \"exceptionMessage\": \"Something wrong on User server\",\n" +
                        "  \"detailErrors\": []\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenThrow(feignException);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromUserService);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        Assertions.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verifyNoMoreInteractions(usersClient);
        verifyZeroInteractions(keepersClient);
    }

    @Test
    public void onReceiveSlashCommandGetMyDirectionsWhenKeepersServiceReturnErrorSendErrorRichMessage() throws
            Exception {
        //given
        final List<UserDTO> usersInCommand = Collections.singletonList(userFrom);
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), userFrom.getUuid(), "");
        final List<String> slackNames = Collections.singletonList(userFrom.getSlack());
        final String expectedRequestToSlack = "{" +
                "\"username\":null," +
                "\"channel\":null," +
                "\"text\":\"very big and scare error\"," +
                "\"attachments\":null," +
                "\"icon_emoji\":null," +
                "\"response_type\":null" +
                "}";
        String expectedJsonResponseBodyFromKeepersService =
                "status 400 reading KeepersClient#addKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        ArgumentCaptor<SlackNameRequest> captorSlackNameRequest = ArgumentCaptor.forClass(SlackNameRequest.class);
        when(usersClient.findUsersBySlackNames(captorSlackNameRequest.capture())).thenReturn(usersInCommand);

        ArgumentCaptor<KeeperRequest> captorKeeperRequest = ArgumentCaptor.forClass(KeeperRequest.class);
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBodyFromKeepersService);
        when(keepersClient.getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()))).thenThrow
                (feignException);

        mockSuccessSlack(EXAMPLE_URL, HttpMethod.POST, expectedRequestToSlack);

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(IN_PROGRESS));
        //then
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(captorSlackNameRequest.getValue().getSlackNames())
                    .as("'captorSlackNamesRequest' slacknames not contains 'slackNames'")
                    .containsExactlyInAnyOrder(slackNames.toArray(new String[slackNames.size()]));
            soft.assertThat(captorKeeperRequest.getValue())
                    .as("'captorKeeperRequest' is not 'keeperRequest'")
                    .isEqualTo(keeperRequest);
        });

        verify(usersClient).findUsersBySlackNames(captorSlackNameRequest.capture());
        verify(keepersClient).getKeeperDirections(captorKeeperRequest.capture(), eq(userFrom.getUuid()));
        verifyNoMoreInteractions(usersClient, keepersClient);
    }

    private void mockFailSlack(String expectedURI, HttpMethod method, String expectedRequestBody) {
        mockServer.expect(requestTo(expectedURI))
                .andExpect(method(method))
                .andExpect(request -> assertThat(request.getHeaders().getContentType().toString(),
                        containsString("application/json")))
                .andExpect(request -> assertThat(request.getBody().toString(), equalTo(expectedRequestBody)))
                .andRespond(withBadRequest());
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

