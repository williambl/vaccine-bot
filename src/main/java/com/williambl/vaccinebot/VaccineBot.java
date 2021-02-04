package com.williambl.vaccinebot;

import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

public class VaccineBot {

    private static final String apiURL = "https://coronavirus.data.gov.uk/api/v1/data?filters=areaName=United%20Kingdom;areaType=overview&latestBy=cumPeopleVaccinatedFirstDoseByPublishDate&structure={%22date%22:%22date%22,%22value%22:%22cumPeopleVaccinatedFirstDoseByPublishDate%22}";

    private static final Timer timer = new Timer();

    public static void main(String[] args) {
        System.out.println(args[1]);
        try {
            JDA bot = JDABuilder.createDefault(args[0])
                    .setActivity(Activity.watching("the vaccine rollout"))
                    .build();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    checkStats(bot, args[1]);
                }
            }, 2000, 1000 * 60 * 60 * 24);

        } catch (LoginException e) {
            System.err.println("Unable to log in!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void checkStats(JDA bot, String channelId) {
        long amount;
        try {
            amount = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(new URL(apiURL).openStream()))).getAsJsonObject().getAsJsonArray("data").get(0).getAsJsonObject().get("value").getAsLong();
        } catch (IOException e) {
            e.printStackTrace();
            Objects.requireNonNull(bot.getTextChannelById(channelId)).sendMessage("Failed to get latest stats.").queue();
            return;
        }

        Objects.requireNonNull(bot.getTextChannelById(channelId))
                .sendMessage(":tada: **Good news!**\nThere have now been "+amount+" vaccinations in the UK! That's "+(amount/680000L)+"% of the population!").queue();
    }
}
