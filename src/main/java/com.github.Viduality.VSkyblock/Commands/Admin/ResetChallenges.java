package com.github.Viduality.VSkyblock.Commands.Admin;

import com.github.Viduality.VSkyblock.Commands.WorldCommands.AdminSubCommand;
import com.github.Viduality.VSkyblock.SQLConnector;
import com.github.Viduality.VSkyblock.Utilitys.ConfigShorts;
import com.github.Viduality.VSkyblock.Utilitys.DatabaseCache;
import com.github.Viduality.VSkyblock.VSkyblock;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ResetChallenges implements AdminSubCommand {

    private VSkyblock plugin = VSkyblock.getInstance();


    @Override
    public void execute(CommandSender sender, String args, String option1, String option2) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (sender.hasPermission("VSkyblock.ResetChallenges")) {
                DatabaseCache databaseCache = new DatabaseCache();
                try (Connection connection = plugin.getdb().getConnection()) {
                    PreparedStatement preparedStatement;
                    preparedStatement = connection.prepareStatement("SELECT * FROM VSkyblock_Player WHERE playername = ?");
                    preparedStatement.setString(1, args);
                    ResultSet r = preparedStatement.executeQuery();
                    while (r.next()) {
                        databaseCache.setUuid(r.getString("uuid"));
                    }
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }


                UUID uuid = databaseCache.getUuid();
                if (uuid != null) {
                    try (Connection connection = plugin.getdb().getConnection()) {
                        for (int i = 0; i < 3; i++) {
                            for (int x = 1; x < 19; x++) {
                                String c = "c" + x;
                                String table;
                                if (i == 0) {
                                    table = "VSkyblock_Challenges_Easy";
                                } else if (i == 1) {
                                    table = "VSkyblock_Challenges_Medium";
                                } else {
                                    table = "VSkyblock_Challenges_Hard";
                                }

                                PreparedStatement resetChallenges;
                                resetChallenges = connection.prepareStatement("UPDATE " + table + " SET " + c + " = 0 WHERE uuid = ?");
                                resetChallenges.setString(1, uuid.toString());
                                resetChallenges.executeUpdate();
                                resetChallenges.close();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    ConfigShorts.custommessagefromString("ResettedChallenges", sender, args);
                } else {
                    ConfigShorts.messagefromString("PlayerDoesNotExist", sender);
                }
            } else {
                ConfigShorts.messagefromString("PermissionLack", sender);
            }
        });
    }
}
