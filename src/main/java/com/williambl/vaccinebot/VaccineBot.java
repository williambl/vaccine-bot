package com.williambl.vaccinebot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import pl.allegro.finance.tradukisto.ValueConverters;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

public class VaccineBot {
    private static final String API_URL = "https://coronavirus.data.gov.uk/api/v1/data?filters=areaName=United%20Kingdom;areaType=overview&latestBy=cumPeopleVaccinatedFirstDoseByPublishDate&structure={%22firstDose%22:%22cumPeopleVaccinatedFirstDoseByPublishDate%22,%20%22secondDose%22:%22cumPeopleVaccinatedSecondDoseByPublishDate%22,%20%22firstDosePercent%22:%20%22cumVaccinationFirstDoseUptakeByPublishDatePercentage%22,%20%22secondDosePercent%22:%20%22cumVaccinationSecondDoseUptakeByPublishDatePercentage%22}";
    private static final Timer TIMER = new Timer();

    private static JDA bot;

    public static void main(String[] args) {
        try {
            bot = JDABuilder.createDefault(args[0])
                    .setActivity(Activity.watching("the vaccine rollout"))
                    .addEventListeners((EventListener) event -> {
                        if (event instanceof MessageReceivedEvent) {
                            MessageReceivedEvent msgEvent = (MessageReceivedEvent) event;
                            if (!msgEvent.getAuthor().isBot() && msgEvent.getMessage().getContentRaw().toLowerCase(Locale.ROOT).startsWith(",vaccinebot")) {
                                checkStats(bot, msgEvent.getChannel().getId());
                            }
                        }
                    })
                    .build();

            TIMER.scheduleAtFixedRate(new TimerTask() {
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

    private static void checkStats(@NotNull JDA bot, String channelId) {
        TextChannel channel = bot.getTextChannelById(channelId);
        if (channel == null) {
            System.err.println("Failed to find channel with id "+channelId);
            return;
        }

        int firstDose;
        int secondDose;
        int firstDosePercent;
        int secondDosePercent;
        try {
            JsonObject values = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(new URL(API_URL).openStream()))).getAsJsonObject().getAsJsonArray("data").get(0).getAsJsonObject();
            firstDose = values.get("firstDose").getAsInt();
            secondDose = values.get("secondDose").getAsInt();
            firstDosePercent = values.get("firstDosePercent").getAsInt();
            secondDosePercent = values.get("secondDosePercent").getAsInt();
        } catch (IOException e) {
            e.printStackTrace();
            channel.sendMessage("Failed to get latest stats.").queue();
            return;
        }
        int total = firstDose+secondDose;

        channel.sendMessage(new EmbedBuilder()
                .setTitle("Good News!")
                .setDescription(":tada: **Good news!**\n" +
                        "There have now been "+toPrettyString(total)+" (that's "+toWords(total)+") vaccinations in the UK!\n" +
                        +firstDosePercent+"% of the population now have at least one dose.")
                .addField("Total Doses", toPrettyString(total), true)
                .addField("First Doses", toPrettyString(firstDose), true)
                .addField("Second Doses", toPrettyString(secondDose), true)
                .addField("People with one dose", firstDosePercent +"%", true)
                .addField("People with both doses", secondDosePercent +"%", true)
                .build()
        ).queue();
    }

    private static String toWords(int value) {
        return ValueConverters.ENGLISH_INTEGER.asWords(value);
    }

    private static String toPrettyString(int value) {
        StringBuilder builder = new StringBuilder().append(value).reverse();
        int originalLength = builder.length();
        int offset = 0;
        for (int i = 0; i < originalLength; i++) {
            if (i != 0 && i % 3 == 0) {
                builder.insert(i+offset++, ' ');
            }
        }
        return builder.toString();
    }
}
