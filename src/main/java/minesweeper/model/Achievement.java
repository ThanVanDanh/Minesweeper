package minesweeper.model;

public enum Achievement {

    CO_DIEN(
            "co_dien",
            "Cổ Điển",
            "Thắng liên tiếp 2 ván",
            "🏆"
    ),
    TON_TRONG(
            "ton_trong",
            "Tôn Trọng",
            "Thắng liên tiếp 5 ván",
            "👑"
    ),
    NGU_LOM(
            "ngu_lom",
            "Ngu Lớm",
            "Thua 2 ván liên tiếp",
            "💀"
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final String icon;

    Achievement(String id, String displayName, String description, String icon) {
        this.id          = id;
        this.displayName = displayName;
        this.description = description;
        this.icon        = icon;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getIcon()        { return icon; }
}