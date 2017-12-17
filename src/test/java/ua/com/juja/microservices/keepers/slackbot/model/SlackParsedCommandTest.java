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
import static org.junit.Assert.assertEquals;

/**
 * @author Konstantin Sergey
 * @author Oleksii Skachkov
 */
public class SlackParsedCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private List<UserDTO> usersInText;
    private UserDTO userFrom;
    private UserDTO user1;

    @Before
    public void setup() {
        userFrom = new UserDTO("uuid-from", "slack-from");
        user1 = new UserDTO("uuid1", "slack1");
        usersInText = new ArrayList<>();
    }

    @Test
    public void getFirstUserInText() {
        //given
        usersInText.add(user1);
        String text = String.format("text text %s text", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //when
        UserDTO actual = slackParsedCommand.getFirstUserFromText();

        //then
        assertEquals(user1, actual);
    }

    @Test
    public void getFirstUserInTextThrowExceptionIfNotUser() {
        //given
        String text = "text text text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //then
        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(containsString("The text 'text text text' doesn't contain any slack users"));

        //when
        slackParsedCommand.getFirstUserFromText();
    }

    @Test
    public void getAllUsers() {
        //given
        usersInText.add(user1);
        usersInText.add(new UserDTO("uuid2", "slack2"));
        usersInText.add(new UserDTO("uuid3", "slack3"));
        String text = String.format("text %s text %s text %s",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern("slack2"),
                SlackParsedCommand.wrapSlackUserInFullPattern("slack3"));
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //when
        List<UserDTO> actual = slackParsedCommand.getAllUsersFromText();

        //then
        assertEquals(usersInText, actual);
    }

    @Test
    public void getText() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //when
        String result = slackParsedCommand.getText();

        //then
        assertEquals("text", result);
    }

    @Test
    public void getFromUser() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //when
        UserDTO actual = slackParsedCommand.getFromUser();

        //then
        assertEquals(userFrom, actual);
    }

    @Test
    public void getUserCount() {
        //given
        usersInText.add(user1);
        String text = String.format("text %s text", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //then
        assertEquals(1, slackParsedCommand.getUserCountInText());
    }

    @Test
    public void getTextWithoutSlackUsersWhenSlackUserInFullPattern() {
        //given
        String text = String.format("%s text %s text %s",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //then
        assertEquals("text text", slackParsedCommand.getTextWithoutSlackUsers());
    }

    @Test
    public void getTextWithoutSlackUsersWhenSlackUserInPartialPattern() {
        //given
        String text = String.format("%s text %s text %s",
                SlackParsedCommand.wrapSlackUserInPartialPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInPartialPattern(user1.getSlackUser()),
                SlackParsedCommand.wrapSlackUserInPartialPattern(user1.getSlackUser()));

        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(userFrom, text, usersInText);

        //then
        assertEquals("text text", slackParsedCommand.getTextWithoutSlackUsers());
    }
}