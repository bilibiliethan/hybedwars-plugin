package com.hybedwars.game;

/** 对局时间轴事件。 */
public class GameEvent {

    public enum Type {
        DIAMOND_UP("钻石升级"),
        EMERALD_UP("绿宝石升级"),
        BED_GONE("床消失"),
        GAME_END("游戏结束");

        private final String display;

        Type(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }
    }

    private final int seconds;
    private final Type type;

    public GameEvent(int seconds, Type type) {
        this.seconds = seconds;
        this.type = type;
    }

    public int seconds() {
        return seconds;
    }

    public Type type() {
        return type;
    }

    /** "6:00" 形式。 */
    public String timeString() {
        int minutes = seconds / 60;
        int rest = seconds % 60;
        return minutes + ":" + (rest < 10 ? "0" : "") + rest;
    }

    @Override
    public String toString() {
        return timeString() + " " + type.display();
    }
}
