package ua.com.juja.microservices.keepers.slackbot.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * @author Konstantin Sergey
 * @author Oleksii Skachkov
 */
public class SlackParsedCommandTest {
    private List<UserDTO> usersInText;
    private UserDTO fromUser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        fromUser = new UserDTO("uuid0", "from-id");
        usersInText = new ArrayList<>();
    }

    @Test
    public void getFirstUserInText() {
        //given
        usersInText.add(new UserDTO("uuid1", "slack-id1"));
        String text = String.format("text text %s text", SlackParsedCommand.wrapSlackId("slack-id1"));
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        UserDTO result = slackParsedCommand.getFirstUserFromText();
        //then
        assertEquals("UserDTO(uuid=uuid1, slackId=slack-id1)", result.toString());
    }

    @Test
    public void getFirstUserInTextThrowExceptionIfNotUser() {
        //given
        String text = "text text text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(containsString("The text 'text text text' doesn't contain any slack ids"));
        //when
        slackParsedCommand.getFirstUserFromText();
    }

    @Test
    public void getAllUsers() {
        //given
        usersInText.add(new UserDTO("uuid1", "slack-id1"));
        usersInText.add(new UserDTO("uuid2", "slack-id2"));
        usersInText.add(new UserDTO("uuid3", "slack-id3"));
        String text = String.format("text %s text %s text %s", SlackParsedCommand.wrapSlackId("slack-id3"),
                SlackParsedCommand.wrapSlackId("slack-id2"), SlackParsedCommand.wrapSlackId("slack-id1"));
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        List<UserDTO> result = slackParsedCommand.getAllUsersFromText();
        //then
        assertEquals("[UserDTO(uuid=uuid1, slackId=slack-id1), UserDTO(uuid=uuid2, slackId=slack-id2)," +
                        " UserDTO(uuid=uuid3, slackId=slack-id3)]",
                result.toString());
    }

    @Test
    public void getText() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        String result = slackParsedCommand.getText();
        //then
        assertEquals("text", result);
    }

    @Test
    public void getFromUser() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        UserDTO result = slackParsedCommand.getFromUser();
        //then
        assertEquals("UserDTO(uuid=uuid0, slackId=from-id)", result.toString());
    }

    @Test
    public void getUserCount() {
        //given
        usersInText.add(new UserDTO("uuid1", "slack-id1"));
        String text = String.format("text %s text", SlackParsedCommand.wrapSlackId("slack-id1"));
        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        assertEquals(1, slackParsedCommand.getUserCountInText());
    }

    @Test
    public void getTextWithoutSlackIds() {
        //given
        String text = String.format("%s text %s text %s", SlackParsedCommand.wrapSlackId("slack-id"),
                SlackParsedCommand.wrapSlackId("slack-id"), SlackParsedCommand.wrapSlackId("slack-id"));
        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        assertEquals("text text", slackParsedCommand.getTextWithoutSlackIds());
    }
}