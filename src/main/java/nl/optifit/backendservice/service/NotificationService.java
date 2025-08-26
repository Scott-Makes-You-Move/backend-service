package nl.optifit.backendservice.service;

import com.microsoft.graph.models.Attendee;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.FreeBusyStatus;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    @Value("${notification.user-id}")
    private String notificationUserId;
    @Value("${notification.user-email}")
    private String notificationUserEmail;
    @Value("${notification.user-name}")
    private String notificationUserName;
    @Value("${frontend.url}")
    private String frontendUrl;

    private final GraphServiceClient graphServiceClient;

    public Event sendCalendarEventFrom(String email, String fullName, String sessionId, DateTimeTimeZone start, DateTimeTimeZone end) {
        Event event = createEventFrom(email, fullName, sessionId, start, end);

        Event postedEvent = graphServiceClient
                .users(notificationUserId)
                .calendar()
                .events()
                .buildRequest()
                .post(event);

        log.info("Posted event: {}", postedEvent.id);
        return postedEvent;
    }

    @NotNull
    private Event createEventFrom(String email, String fullName, String sessionId, DateTimeTimeZone start, DateTimeTimeZone end) {
        Event event = new Event();

        // body
        ItemBody body = new ItemBody();
        body.content = generateEmailContent(fullName, sessionId);
        body.contentType = BodyType.HTML;

        // attendee
        Attendee attendee = new Attendee();
        EmailAddress attendeeEmailAddress = new EmailAddress();
        attendeeEmailAddress.address = email;
        attendeeEmailAddress.name = fullName;
        attendee.emailAddress = attendeeEmailAddress;

        // organizer
        Recipient organizer = new Recipient();
        EmailAddress organizerEmailAddress = new EmailAddress();
        organizerEmailAddress.address = notificationUserEmail;
        organizerEmailAddress.name = notificationUserName;
        organizer.emailAddress = organizerEmailAddress;

        event.subject = "Make your next move, %s".formatted(fullName);
        event.body = body;
        event.start = start;
        event.end = end;
        event.attendees = List.of(attendee);
        event.organizer = organizer;
        event.isOnlineMeeting = false;
        event.allowNewTimeProposals = false;
        event.showAs = FreeBusyStatus.BUSY;
        return event;
    }

    private String generateEmailContent(String fullName, String sessionId) {
        return "Hi %s\n".formatted(fullName) +
                "\n" +
                "<p> STOP what you're doing 🖐️ </p>\n" +
                "\n" +
                "<p> It's time for you your 1 min movement break! 🏃‍♂️ </p>\n" +
                "\n" +
                "<p> Click  <a href=\"%s/watch/%s\">HERE</a>  to see what  exercise suits you best based upon your mobility check up results. </p>\n".formatted(frontendUrl, sessionId) +
                "\n" +
                "<p> %s Remember 👉 \"Prevention is better than cure\" </p>\n".formatted(fullName) +
                "\n" +
                "<p> If you have any questions don't hesitate to reach out, I am ready to help you on your journey towards better health and wellbeing. </p>\n" +
                "\n" +
                "<p> 📧 scottmakesyoumove@gmail.com </p>\n" +
                "\n" +
                "<p> Have  great day! ✨ </p>\n" +
                "\n" +
                "<p> Scott | SMYM</p>";
    }
}
