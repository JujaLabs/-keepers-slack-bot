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
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand.*;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SlackIdHandlerServiceTest {

    @Inject
    private SlackIdHandlerService slackIdHandlerService;

    @MockBean
    private UserService userService;

    private UserDTO userFrom;
    private UserDTO user1;
    private UserDTO user2;

    @Before
    public void setup() {
        userFrom = new UserDTO("AAA000", "slack-idFrom");
        user1 = new UserDTO("AAA111", "slack-id1");
        user2 = new UserDTO("AAA222", "slack-id2");
    }

    @Test
    public void getSlackParsedCommandOneSlackInText() throws Exception {
        //given
        String text = "text " + SlackParsedCommand.wrapSlackId(user1.getSlackId()) + " TexT text.";
        List<String> requestToUserService = Arrays.asList(user1.getSlackId(),userFrom.getSlackId());
        List<UserDTO> responseFromUserService = Arrays.asList(userFrom, user1);
        when(userService.findUsersBySlackIds(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Collections.singletonList(user1));
        //when
        SlackParsedCommand actual = slackIdHandlerService.createSlackParsedCommand(userFrom.getSlackId(), text);
        //then
        assertEquals(expected, actual);
    }

    @Test
    public void getSlackParsedCommandTwoSlackInText() throws Exception {
        //given
        String text = "text " + SlackParsedCommand.wrapSlackId(user1.getSlackId()) + " TexT " + SlackParsedCommand.wrapSlackId(user2.getSlackId()) + " text.";
        List<String> requestToUserService = Arrays.asList(user1.getSlackId(), user2.getSlackId(), userFrom.getSlackId());
        List<UserDTO> responseFromUserService = Arrays.asList(userFrom, user1, user2);
        when(userService.findUsersBySlackIds(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Arrays.asList(user1, user2));
        //when
        SlackParsedCommand actual = slackIdHandlerService.createSlackParsedCommand(userFrom.getSlackId(), text);
        //then
        assertEquals(expected, actual);
    }

    @Test
    public void getSlackParsedCommandWithoutSlackInText() throws Exception {
        //given
        String text = "text without slack id TexT text.";
        List<String> requestToUserService = Collections.singletonList(userFrom.getSlackId());
        List<UserDTO> responseFromUserService = Collections.singletonList(userFrom);
        when(userService.findUsersBySlackIds(requestToUserService)).thenReturn(responseFromUserService);
        SlackParsedCommand expected = new SlackParsedCommand(userFrom, text, Collections.singletonList(userFrom));
        //when
        SlackParsedCommand actual = slackIdHandlerService.createSlackParsedCommand(userFrom.getSlackId(), text);
        //then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldAddATToFromUserIfFromUserWithoutAT() {
        //given
        String text = "SomeText " + SlackParsedCommand.wrapSlackId(user1.getSlackId());
        List<String> requestToUserService = Arrays.asList(user1.getSlackId(), userFrom.getSlackId());
        List<UserDTO> responseFromUserService = Arrays.asList(userFrom, user1);
        when(userService.findUsersBySlackIds(requestToUserService)).thenReturn(responseFromUserService);
        //when
        String expected = "slack-idFrom";
        SlackParsedCommand actual = slackIdHandlerService.createSlackParsedCommand("slack-idFrom", text);
        //then
        assertEquals(expected, actual.getFromUser().getSlackId());
        verify(userService).findUsersBySlackIds(requestToUserService);
        verifyNoMoreInteractions(userService);
    }
}