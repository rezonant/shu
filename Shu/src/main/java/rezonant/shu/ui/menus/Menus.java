package rezonant.shu.ui.menus;

import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import rezonant.shu.ui.listview.ListItem;
import rezonant.shu.R;
import rezonant.shu.ui.listview.RealizedListItem;

/**
 * Created by liam on 6/26/14.
 */
public class Menus {
    static {
        SECTIONS = createMenu(new ArrayList() {
            {
                add(new ListItem("systemStatus", R.string.section_system_status));
                add(new ListItem("networkStatus", R.string.section_network_status));
                add(new ListItem("networkTools", R.string.section_network_tools));
                add(new ListItem("remoteControl", R.string.section_remote_control));
                add(new ListItem("settings", R.string.section_settings));
                add(new ListItem("about", R.string.section_about));
            }
        });

        SYSTEMSTATUS = createMenu(new ArrayList() {
            {
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Local"));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Now we are!"));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Talking!"));
            }
        });

        NETWORKSTATUS = createMenu(new ArrayList() {
            {
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "These are!"));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Networky!"));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Things!"));
            }
        });

        NETWORKTOOLS = createMenu(new ArrayList() {
            {
                add(new RealizedListItem("systemStatus", R.string.section_system_status, ""));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Network Discovery"));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, ""));
                add(new RealizedListItem("systemStatus", R.string.section_system_status, "Things!"));
            }
        });


    }

    private static ListItem[] createMenu(ArrayList<ListItem> list) {
        ListItem[] array = new ListItem[list.size()];
        list.toArray(array);
        return array;
    }

    public static final ListItem[] SECTIONS;
    public static final ListItem[] SYSTEMSTATUS;
    public static final ListItem[] NETWORKSTATUS;
    public static final ListItem[] NETWORKTOOLS;



    public static ListItem[] getMenu(String id)
    {
        Log.i("hrm", "Trying!");

        Field field;

        try {
            field = Menus.class.getDeclaredField(id);
        } catch (NoSuchFieldException e) {
            return null;
        }

        try {
            Log.i("hrm", "YAY!");
            System.out.println("Yay!");
            return (ListItem[])field.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static ListItem find(ListItem[] menu, String itemId)
    {
        for (ListItem item : menu) {
            if (item.getId().equalsIgnoreCase(itemId))
                return item;
        }

        return null;
    }

    public static ListItem find(String menuId, String itemId)
    {
        return find(getMenu(menuId), itemId);
    }

	public static RealizedListItem[] realize(List<ListItem> menu, Resources resources)
	{
		ListItem[] array = new ListItem[menu.size()];
		menu.toArray(array);

		return realize(array, resources);
	}

    /**
     * Realize (translate) an array of menu items
     *
     * @param menu
     * @param resources
     * @return
     */
    public static RealizedListItem[] realize(final ListItem[] menu, final Resources resources)
    {
        RealizedListItem[] array = new RealizedListItem[menu.length];

        new ArrayList<RealizedListItem>() {
            {
                for (ListItem item : menu) {
                    if (item instanceof RealizedListItem) {
                        add((RealizedListItem)item);
                        continue;
                    }

					RealizedListItem realizedItem = new RealizedListItem(item.getId(), item.getLabel(),
							resources.getString(item.getLabel()));

					realizedItem.setIsSectionHeader(item.isSectionHeader());
                    add(realizedItem);
                }
            }
        }.toArray(array);

        return array;
    }
}
