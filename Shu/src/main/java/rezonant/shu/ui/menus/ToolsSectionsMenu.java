package rezonant.shu.ui.menus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolsSectionsMenu {

    /**
     * An array of sample (dummy) items.
     */
    public static List<ToolMenuItem> ITEMS = new ArrayList<ToolMenuItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<String, ToolMenuItem> ITEM_MAP = new HashMap<String, ToolMenuItem>();

    static {
        addItem(new ToolMenuItem("systemStatus", "System Status"));
        addItem(new ToolMenuItem("networkStatus", "Network Status"));
        addItem(new ToolMenuItem("networkTools", "Tools"));
        addItem(new ToolMenuItem("remoteControl", "Remote"));
        addItem(new ToolMenuItem("remoteControl", "Settings"));
    }

    private static void addItem(ToolMenuItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class ToolMenuItem {
        public String id;
        public String content;

        public ToolMenuItem(String id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
