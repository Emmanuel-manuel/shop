package emm.sys;

public class Note {

    private int    id;
    private String title;
    private String content;
    private String timestamp;

    public Note(int id, String title, String content, String timestamp) {
        this.id        = id;
        this.title     = title;
        this.content   = content;
        this.timestamp = timestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public int    getId()        { return id; }
    public String getTitle()     { return title; }
    public String getContent()   { return content; }
    public String getTimestamp() { return timestamp; }

    // ── Setters ──────────────────────────────────────────────────────────────
    public void setId(int id)               { this.id = id; }
    public void setTitle(String title)      { this.title = title; }
    public void setContent(String content)  { this.content = content; }
    public void setTimestamp(String ts)     { this.timestamp = ts; }

    // Convenience: short snippet for the list card
    public String getSnippet() {
        if (content == null || content.isEmpty()) return "";
        return content.length() > 80 ? content.substring(0, 80) + "…" : content;
    }
}
