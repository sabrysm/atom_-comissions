package com.me.LootSplit.utils;

import com.me.LootSplit.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.me.LootSplit.utils.ImageTextExtractor.getNames;

public class LootSplitPartyUploadHelper {
    private static final int MIN_WIDTH_RESOLUTION = 300;
    private static final int MIN_HEIGHT_RESOLUTION = 400;
    private final String splitID;
    private final String uploadId;
    private final long guildID;
    private List<String> validNames = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<String> usersStatus = new ArrayList<>();
    private List<String> leftNames = new ArrayList<>();
    private List<String> rightNames = new ArrayList<>();
    private List<String> leftStatus = new ArrayList<>();
    private List<String> rightStatus = new ArrayList<>();

    public LootSplitPartyUploadHelper(String splitID, long guildID) {
        this.splitID = splitID;
        this.guildID = guildID;
        this.uploadId = generateUploadId();
    }

    public static boolean isLowResolution(InputStream imageStream) throws IOException {
        BufferedImage image = ImageIO.read(imageStream);
        if (image == null) {
            throw new IOException("The provided InputStream does not contain a valid image.");
        }
        return image.getWidth() < MIN_WIDTH_RESOLUTION || image.getHeight() < MIN_HEIGHT_RESOLUTION;
    }

    public void sendCorrectCroppingFormat(SlashCommandInteractionEvent event) {
        // Send a message to the user that the image is too low resolution
        try {
            // Reply with an example image of the expected format
            String imagePath = "E:\\Programming\\atom_comm_1\\src\\main\\resources\\static_files\\partyUploadExample.png";
            EmbedBuilder instructionEmbed = new EmbedBuilder();
            instructionEmbed.setTitle("Low Resolution Image").setDescription("Make sure your Screenshot is similar to this\n example with a **Decent Resolution**.").setImage("attachment://partyUploadExample.png").setColor(0xFF0000);
            event.getHook().sendFiles(FileUpload.fromData(new File(imagePath))).setEmbeds(instructionEmbed.build()).queue();
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateUploadId() {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder();
        int count = 7;
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }


    public void addPlayersToParty(SlashCommandInteractionEvent event) {
        try {
            DatabaseManager manager = new DatabaseManager();
            manager.addPlayersToLootSplit(uploadId, splitID, validNames, event.getGuild().getIdLong());
            System.out.println("Players added to the party successfully");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getValidNamesFromImage(InputStream imageStream) {
        List<String> detectedNames = new ArrayList<>();
        List<String> detectedUsersStatus = new ArrayList<>();
        try {
            PartyOCR partyOCR = new PartyOCR();
            List<String> detectedNamesFromParty = partyOCR.extractNamesFromPartyImage(imageStream);
            System.out.println("Length of filtered names: " + detectedNamesFromParty.size());
            DatabaseManager databaseManager = new DatabaseManager();
            for (int i = 0; i < detectedNamesFromParty.size(); i++) {
                String detectedName = detectedNamesFromParty.get(i);
                System.out.printf("Processing name: %s\n", detectedName);
                String detectedSpelledName = databaseManager.spellFixMatch(detectedName, guildID);
                if (detectedSpelledName != null) {
                    System.out.printf("Detected Spell Name: %s\n", detectedSpelledName);
                    detectedNames.add(detectedSpelledName);
                    validNames.add(detectedSpelledName);
                    detectedUsersStatus.add("Exists");
                }
                else {
                    System.out.println("Detected Name is null");
                    detectedNames.add("????");
                    detectedUsersStatus.add("Unknown");
                }
            }
            names = new ArrayList<>(detectedNames);
            usersStatus = new ArrayList<>(detectedUsersStatus);
            fillLeftRightNames();
            System.out.println("Length of party names: " + names.size());
            System.out.println("Length of party users' status: " + usersStatus.size());
            return detectedNames;

        } catch (SQLException e) {
            System.out.println("Error while using spellFixMatch: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.out.println("IOException: Error while getting valid names from the image: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            System.out.println("The image provided is null: " + e.getMessage());
            throw new NullPointerException("The image provided is null: " + e.getMessage());
        } catch (ConcurrentModificationException e) {
            System.out.println("Concurrent Modification Exception: " + e.getMessage());
            throw new ConcurrentModificationException("Concurrent Modification Exception: " + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("Error while getting valid names from the image: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        String[] detectedWords = {
                "Cluster", "Access", "Priority", "9", "ie", "This", "setting", "determines", "who", "gets",
                "preferred", "access", "to", "overcrowded", "clusters.", "Party", "Priority", "(1", "-",
                "First", "Access)", "@", "Party's", "Priority", "within", "Alliance/Guild:", "c=", "9.)",
                "Party", "Member", "Priority", "(1", "-", "First", "Access)", "Troyroxursox", "c=", "9+)",
                "F)", "papuarock", "C=", "D+)", "Ohuevo199612", "xX", "9", "fF)", "Kokomiuwu", "c=", "@.+)",
                "QHELLassasin", "eax", "39", "Joziu", "x9", "w", "XFiltz", "c=", "9+)", "iAtom", "=", "R=)",
                "Anahk", "c=", "9+)", "KeKo22", "c=", "@<+)", "ChepeNoob", "C=", "@<+)", "Daconer", "a9",
                "Cvalashe", "=", "+]", "ggignazio", "=", "+)", "Annihilatixn", "a9", "Xzotick95", "e029",
                "Virhex", "c=", "+)", "YuenDark", "or", "9", "evi", "AO9", "TurtleTaboo"
        };
        String[] detectedWords2 = {
                "Cluster", "Access", "Priority", "@", "ie", "This", "setting", "determines", "who", "gets",
                "preferred", "N�", "access", "to", "overcrowded", "clusters.", "Party", "Priority", "(1", "-",
                "First", "Access)", "@", "Party's", "Priority", "within", "Alliance/Guild:", "c=", "Dit)",
                "Party", "Member", "Priority", "(1", "-", "First", "Access)", "NyrusGoth", "=", "@Di+)",
                "FE]", "sbankk", "=", "@Di+)", "DarKzerah69", "=", "�Di+)", "Casssio", "=", "@Di+)",
                "HUN", "=", "@D", "i+)", "gj", "APPLOOP", "=", "Dit)", "Beuuus", "=", "D+)", "Joziu", "=",
                "@D<+)", "QJ", "DannyChino", "=", "D+)", "TurtleTaboo", ">", "@D<+)", "QO", "HELLassasin",
                "=", "@D<+)", "thirtyone31", "=", "@Di+)", "FanOfBeer", "=", "@D<+)", "@", "danielconta",
                "=", "GD", "i+)", "QuadDamogel5", "=", "D+)", "Contlaser", "=", "D+)", "Ff]", "papuarock",
                "=", "D+", "|", "Shajat", "OC", "XY}", "F)", "LegionarRo", "=", "�+", ")", "F]", "Nakrotth"
        };
        // [C3, Cluster, Access, Priority, =), m), This, setting, determines, who, gets, preferred, a, access, to, overcrowded, clusters., Party, Priority, (1, -, First, Access), @, Party's, Priority, within, Alliance/Guild:, C=,  D.+), Party, Member, Priority, (1, -, First, Access), NyrusGoth, c=, @D<+), FE], sbankk, eC, 4X9, DarKzerah69, =, B+), @, Casssio, =, @B, +), TI, =, B+), gy, APPLOOP, =, Ris), Beuuus, =, GDRs), Joziu, =, @Di+), Q, DannyChino, =, @Di+), , TurtleTaboo, =, Dit), QO, HELLassasin, >, Dis), thirtyone31, >, Di), FanOfBeer, >, Bi), @, danielconta,  =, Di, +), QuadDamogel5, =, ED, i+), Contlaser, =, D+", FP], papuarock, =, p+), Shajat,  =, D+), F), LegionarRo, c=, DB.), F), Nakrotth]
        String[] detectedWords3 = {
                "C3", "Cluster", "Access", "Priority", "=)", "m)", "This", "setting", "determines", "who", "gets",
                "preferred", "a", "access", "to", "overcrowded", "clusters.", "Party", "Priority", "(1", "-",
                "First", "Access)", "@", "Party's", "Priority", "within", "Alliance/Guild:", "C=", "D.+)",
                "Party", "Member", "Priority", "(1", "-", "First", "Access)", "NyrusGoth", "c=", "@D<+)",
                "FE]", "sbankk", "eC", "4X9", "DarKzerah69", "=", "B+)", "@", "Casssio", "=", "@B", "+)",
                "TI", "=", "B+)", "gy", "APPLOOP", "=", "Ris)", "Beuuus", "=", "GDRs)", "Joziu", "=", "@Di+)",
                "Q", "DannyChino", "=", "@Di+)", "TurtleTaboo", "=", "Dit)", "QO", "HELLassasin", ">", "Dis)",
                "thirtyone31", ">", "Di)", "FanOfBeer", ">", "Bi)", "@", "danielconta", "=", "Di", "+)",
                "QuadDamogel5", "=", "ED", "i+)", "Contlaser", "=", "D+)", "FP]", "papuarock", "=", "p+)",
                "Shajat", "=", "D+)", "F)", "LegionarRo", "c=", "DB.)", "F)", "Nakrotth"
        };

        List<String> detectedNames = extractNames(detectedWords2);

        System.out.println("Detected Names: " + detectedNames + "\nSize: " + detectedNames.size());
    }

    public static List<String> extractNames(String[] detectedWords) {
        String[] forbidden_words = {"Cluster", "Access", "preferrad", "Priority", "This", "setting", "determines", "who", "gets", "preferred", "access", "overcrowded", "clusters.", "Party", "Priority", "First", "Access)", "Party's", "Priority", "within", "Alliance/Guild:", "Dit)", "Party", "Member", "Priority",
                "First", "Access)"};
        List<String> detectedNames = new ArrayList<>();
        for (String word : detectedWords) {
            // Skip words that are purely symbols or invalid characters
            if (word.matches("[=,@><|:()\\[\\]{}+\\.\\-]") || word.contains("�")) {
                continue;
            }
            // Exclude isolated numeric words or very short alphanumeric words
            if (word.matches("\\d+") || (word.matches("[a-zA-Z]\\d+") && word.length() <= 4)) {
                continue;
            }
            // if the word length is below 4, then spell fix it (i.e. see if it exists in DB and retrieve it then add it)
            // Exclude any word that is in the forbidden words list
            if (Arrays.asList(forbidden_words).contains(word)) {
                continue;
            }
//            if (word.length() <= 4) {
//                try {
//                    DatabaseManager manager = new DatabaseManager();
//                    // spell fix the word
//                    String correctedWord = manager.spellFixMatch(word);
//                    if (correctedWord != null) {
//                        detectedNames.add(correctedWord);
//                    }
//                    else {
//                        continue;
//                    }
//                } catch (SQLException e) {
//                    throw new RuntimeException(e);
//                } catch (NullPointerException e) {
//                    throw new NullPointerException("The image provided is null: " + e.getMessage());
//                }
//            }
            // Exclude single-letter or short words that are not meaningful names
            if (word.matches("[a-zA-Z0-9]+")) {
                detectedNames.add(word);
            }
        }
        return detectedNames;
    }


    public List<String> filterDetectedNames(List<String> detectedNames) {
        List<String> filteredNames = new ArrayList<>();
        String[] forbidden_words = {"Cluster", "Access", "Priority", "This", "setting", "determines", "who", "gets", "preferred", "preferrad", "access", "overcrowded", "clusters.", "Party", "Priority", "First", "Access)", "Party's", "Priority", "within", "Alliance/Guild:", "Dit)", "Party", "Member", "Priority",
                "First", "Access)"};
        System.out.print("Detected output: [");
        for (String name : detectedNames) {
            // Check if the name is longer than 2 characters and not in the forbidden words
            // and has no character like ! or @ or / ..etc
            // not alphanumeric if it is equal to 3 => && !name.matches(".*\\d.*")
            if (name.length() > 2 && !Arrays.asList(forbidden_words).contains(name) && !name.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*") && !(name.matches(".*\\d.*") && name.length() == 3)) {
                System.out.print(name + ", ");
                filteredNames.add(name);
            }
        }
        System.out.println("]");
        return filteredNames;
    }

    public void fillLeftRightNames() {
        // for even number of names insert into left and for the others insert into right
        for (int i = 0; i < names.size(); i++) {
            if (i % 2 == 0) {
                leftNames.add(names.get(i));
                leftStatus.add(usersStatus.get(i));
            } else {
                rightNames.add(names.get(i));
                rightStatus.add(usersStatus.get(i));
            }
        }
    }

    public void sendPartyUploadSuccessMessage(SlashCommandInteractionEvent event) {
        // Get the paginator embeds
        List<MessageEmbed> partyEmbeds = getPaginatorEmbed2();
        Paginator partyPaginator = new Paginator(partyEmbeds, event.getUser().getIdLong());
        event.getJDA().addEventListener(partyPaginator);
        // Send a success message with the number of players added
        event.getHook().sendMessageEmbeds(partyEmbeds.get(0)).setActionRow(partyPaginator.getPreviousButton(), partyPaginator.getNextButton()).queue();
    }


    public void sendUserRemovedFromPartyEmbed(String playerName, SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("User Removed from Party");
        embed.setDescription("The user **" + playerName + "** has been removed from the party.");
        embed.setColor(0xFF0000); // Red
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    public void sendUserAddedToPartyEmbed(String playerName, String status, SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("User Added to Party");
        embed.setDescription("The user **" + playerName + "** has been marked with status **" + status + "**.");
        embed.setColor(0x6064f4);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    public List<MessageEmbed> getPaginatorEmbed() {
        List<MessageEmbed> pages = new ArrayList<>();
        int namesPerPage = 10;
        int totalPages = (int) (Math.ceil((double) names.size() / namesPerPage));
        int currentPage = 0;
        for (int i = 0; i < names.size(); i++) {
            EmbedBuilder newPage = new EmbedBuilder();
            newPage.setTitle("Detected Names");
            StringBuilder pageContent = new StringBuilder();
            // Use StringUtils.center to center the text with 20 size for #, 20 for name and 20 for status
            String header = "```" + StringUtils.center("#", 3) + StringUtils.center("", 20) + StringUtils.center("Name", 16) + "\n\n";
            pageContent.append(header);
            for (int j = i; j < i + namesPerPage && j < names.size(); j++) {
                // Add fields to the embed and make it as a table
                pageContent.append(StringUtils.center(String.valueOf(j + 1), 3)).append(StringUtils.center("", 20)).append(StringUtils.center(names.get(j), 16)).append("\n");
            }
            pageContent.append("```");
            newPage.setDescription(String.format("The following usernames have been detected \nfor the Party:\n*(You can re-upload the image to update the party)*\n\n" + pageContent.toString()));
            newPage.setFooter("Page " + (currentPage + 1) + " of " + (totalPages));
            newPage.setColor(0x6064f4); // Blue
            pages.add(newPage.build());
            currentPage++;
            i += namesPerPage - 1;
        }
        return pages;
    }

    public List<MessageEmbed> getPaginatorEmbed2() {
        List<MessageEmbed> pages = new ArrayList<>();
        int currentPage = 0;
        EmbedBuilder newPage = new EmbedBuilder();
        newPage.setTitle("Detected Names");
        StringBuilder pageContent = new StringBuilder();
        String header = "```" + StringUtils.center("Names", 57) + "\n\n";
        pageContent.append(header);
        // Add fields to the embed and make it as a table
        for (int i = 0; i < 10; i++) {
            String error = StringUtils.center("", 25);
            if (leftStatus.get(i).equals("Unknown") && rightStatus.get(i).equals("Unknown")) {
                error = StringUtils.center("<= Error here =>", 25);
            } else if (leftStatus.get(i).equals("Unknown")) {
                error = StringUtils.center("<= Error here", 25);
            } else if (rightStatus.get(i).equals("Unknown")) {
                error = StringUtils.center("Error here =>", 25);
            }
            pageContent.append(StringUtils.center(leftNames.get(i), 16)).append(error).append(StringUtils.center(rightNames.get(i), 16)).append("\n");
        }
        pageContent.append("```");
        newPage.setDescription(String.format("The following usernames have been detected \nfor the Party:\n*(You can re-upload the image to update the party)*\n\n" + pageContent.toString()));
        newPage.setFooter("Page 1 of 1");
        newPage.setColor(0x6064f4); // Blue
        pages.add(newPage.build());
        return pages;
    }
}
