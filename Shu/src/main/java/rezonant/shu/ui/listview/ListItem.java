package rezonant.shu.ui.listview;

/**
 * Created by liam on 6/26/14.
 */
public class ListItem {
	public ListItem(String id, int label) {
		this.id = id;
		this.label = label;
	}

	public static ListItem createSectionHeader(int label)
	{
		return createSectionHeader("", label);
	}

	public static ListItem createSectionHeader(String id, int label)
	{
		ListItem item = new ListItem(id, label);
		item.isSectionHeader = true;
		return item;
	}

    private String id;
    private int label;
	protected boolean isSectionHeader;

    public String getId()
    {
        return this.id;
    }

    public int getLabel()
    {
        return this.label;
    }

    public String toString()
    {
        return this.id;
    }

	public boolean isSectionHeader() {
		return isSectionHeader;
	}

	public void setIsSectionHeader(boolean isSectionHeader) {
		this.isSectionHeader = isSectionHeader;
	}
}

