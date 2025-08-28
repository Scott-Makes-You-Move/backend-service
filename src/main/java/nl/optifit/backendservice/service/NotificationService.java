package nl.optifit.backendservice.service;

import com.microsoft.graph.models.Attendee;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.FreeBusyStatus;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
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

    public void sendCalendarEventFrom(String email, String fullName, String sessionId, DateTimeTimeZone start, DateTimeTimeZone end) {
        Event event = createEventFrom(email, fullName, sessionId, start, end);

        Event postedEvent = graphServiceClient.users()
                .byUserId(notificationUserId)
                .calendar()
                .events()
                .post(event);

        log.info("Posted event: {}", postedEvent.getId());
    }

    @NotNull
    private Event createEventFrom(String email, String fullName, String sessionId, DateTimeTimeZone start, DateTimeTimeZone end) {
        Event event = new Event();

        // body
        ItemBody body = new ItemBody();
        body.setContent(generateEmailContent(fullName, sessionId));
        body.setContentType(BodyType.Html);

        // attendee
        Attendee attendee = new Attendee();
        EmailAddress attendeeEmailAddress = new EmailAddress();
        attendeeEmailAddress.setAddress(email);
        attendeeEmailAddress.setName(fullName);
        attendee.setEmailAddress(attendeeEmailAddress);

        // organizer
        Recipient organizer = new Recipient();
        EmailAddress organizerEmailAddress = new EmailAddress();
        organizerEmailAddress.setAddress(notificationUserEmail);
        organizerEmailAddress.setName(notificationUserName);
        organizer.setEmailAddress(organizerEmailAddress);

        event.setSubject("Make your next move, %s".formatted(fullName));
        event.setBody(body);
        event.setStart(start);
        event.setEnd(end);
        event.setAttendees(List.of(attendee));
        event.setOrganizer(organizer);
        event.setIsOnlineMeeting(false);
        event.setAllowNewTimeProposals(false);
        event.setShowAs(FreeBusyStatus.Busy);
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
