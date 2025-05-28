package nl.optifit.backendservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cron")
public class CronProperties {

    private Sessions sessions = new Sessions();
    private Leaderboard leaderboard = new Leaderboard();

    public Sessions getSessions() {
        return sessions;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public static class Sessions {
        private SessionTime morning = new SessionTime();
        private SessionTime lunch = new SessionTime();
        private SessionTime afternoon = new SessionTime();

        public SessionTime getMorning() { return morning; }
        public SessionTime getLunch() { return lunch; }
        public SessionTime getAfternoon() { return afternoon; }

        public static class SessionTime {
            private String create;
            private String update;

            public String getCreate() { return create; }
            public void setCreate(String create) { this.create = create; }

            public String getUpdate() { return update; }
            public void setUpdate(String update) { this.update = update; }
        }
    }

    public static class Leaderboard {
        private String reset;

        public String getReset() { return reset; }
        public void setReset(String reset) { this.reset = reset; }
    }
}
