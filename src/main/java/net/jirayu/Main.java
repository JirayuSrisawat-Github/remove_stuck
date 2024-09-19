package net.jirayu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    private static final String[] NODE = {
            "http://localhost",
            "youshallnotpass"
    };
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long SWEEP_INTERVAL = 5 * 60 * 1000; // 5 minutes in milliseconds

    public static void main(String[] args) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    runSweep();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, SWEEP_INTERVAL);

        // Keep the main thread alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runSweep() throws Exception {
        List<String> sessions = fetchSessions();
        List<Player> players = fetchPlayers();

        for (String sessionId : sessions) {
            for (Player player : players) {
                if (player.track == null) {
                    deletePlayer(sessionId, player.guildId);
                }
            }
        }
    }

    private static List<String> fetchSessions() throws Exception {
        return fetchData("/v4/sessions", List.class);
    }

    private static List<Player> fetchPlayers() throws Exception {
        return fetchData("/v4/players", new TypeReference<List<Player>>() {});
    }

    private static <T> T fetchData(String endpoint, Class<T> valueType) throws Exception {
        return fetchData(endpoint, new TypeReference<T>() {});
    }

    private static <T> T fetchData(String endpoint, TypeReference<T> valueTypeRef) throws Exception {
        URL url = new URL(NODE[0] + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", NODE[1]);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return objectMapper.readValue(reader, valueTypeRef);
        }
    }

    private static void deletePlayer(String sessionId, String guildId) {
        try {
            URL url = new URL(NODE[0] + "/v4/sessions/" + sessionId + "/players/" + guildId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", NODE[1]);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            if (connection.getResponseCode() == 204) {
                System.out.println("Removed " + guildId + " on " + sessionId);
            } else {
                System.out.println("Failed to remove " + guildId + " on " + sessionId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Player {
        public String guildId;
        public Object track; // Assuming track can be of any type
        public Integer volume;
        public Boolean paused;
        public Object state;
        public Object voice;
        public Object filters;
    }
}
