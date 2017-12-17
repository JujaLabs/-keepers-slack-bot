package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SlackUserHandlerServiceTest {

    @Inject
    private SlackUserHandlerService slackUserHandlerService;

    @MockBean
    private UserService userService;

    private UserDTO userFrom;
    private UserDTO user1;
    private UserDTO user2;

    @Before
    public void setup() {
        userFrom = new UserDTO("uuid-from", "slack-from");
        user1 = new UserDTO("uuid1", "slack1");
        user2 = new UserDTO("uuid2", "slack2");
    }

    @Test
    public void getSlackParsedCommandOneSlackInText() throws Exception {
        //given
        String text = String.format("text %s TexT text.", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        List<String> requestToUserService = Arrays.asList(user1.getSlackUser(), userFrom.getSlackUser());
        List<UserDTO> responseFromUserService = Arrays.asList(userFrom, user1);
        when(userService.findUsersBySlackUsers(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Collections.singletonList(user1));

        //when
        SlackParsedCommand actual = slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), text);

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void getSlackParsedCommandTwoSlackInText() throws Exception {
        //given
        String text =String.format("text %s TexT %s text.",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user2.getSlackUser()) );
        List<String> requestToUserService = Arrays.asList(user1.getSlackUser(), user2.getSlackUser(), userFrom.getSlackUser());
        List<UserDTO> responseFromUserService = Arrays.asList(userFrom, user1, user2);
        when(userService.findUsersBySlackUsers(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Arrays.asList(user1, user2));

        //when
        SlackParsedCommand actual = slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), text);

        //then
        assertEquals(expected, actual);
    }

    @Test
    public void getSlackParsedCommandWithoutSlackInText() throws Exception {
        //given
        String text = "text without slack id TexT text.";
        List<String> requestToUserService = Collections.singletonList(userFrom.getSlackUser());
        List<UserDTO> responseFromUserService = Collections.singletonList(userFrom);
        when(userService.findUsersBySlackUsers(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Collections.singletonList(userFrom));

        //when
        SlackParsedCommand actual = slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), text);

        //then
        assertEquals(expected, actual);
    }
}