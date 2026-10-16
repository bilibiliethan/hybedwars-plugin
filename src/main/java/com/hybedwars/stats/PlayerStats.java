package com.hybedwars.stats;

import com.hybedwars.config.Settings;

import java.util.UUID;

/** 单个玩家的累计战绩。 */
public class PlayerStats {

    private final UUID uuid;
    private String name;

    private int wins;
    private int losses;
    private int kills;
    private int finalKills;
    private int deaths;
    private int bedsBroken;
    private int games;
    private int xp;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int wins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int losses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int kills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int finalKills() {
        return finalKills;
    }

    public void setFinalKills(int finalKills) {
        this.finalKills = finalKills;
    }

    public int deaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int bedsBroken() {
        return bedsBroken;
    }

    public void setBedsBroken(int bedsBroken) {
        this.bedsBroken = bedsBroken;
    }

    public int games() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }

    public int xp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void addXp(int amount) {
        this.xp = Math.max(0, this.xp + amount);
    }

    /** 星级：1 星起步，每 xp-per-level 点经验升一星。 */
    public int level() {
        return 1 + xp / Math.max(1, Settings.xpPerLevel);
    }

    public double kdr() {
        if (deaths == 0) {
            return kills + finalKills;
        }
        return Math.round(((kills + finalKills) * 100.0) / deaths) / 100.0;
    }

    public double winRate() {
        return games == 0 ? 0 : Math.round(wins * 1000.0 / games) / 10.0;
    }
}
