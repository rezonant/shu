package rezonant.shu.ui.listview;

/**
 * Created by liam on 6/26/14.
 */
public class RealizedListItem extends ListItem {
    public RealizedListItem(String id, int labelId, String realizedLabel) {
        super(id, labelId);

        this.realizedLabel = realizedLabel;
    }

    private String realizedLabel;

    public String getRealizedLabel()
    {
        return realizedLabel;
    }

    public String toString() {
        return realizedLabel;
    }
}
