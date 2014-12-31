package rezonant.shu.ui.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import rezonant.shu.R;

/**
 * Created by liam on 6/29/14.
 */
public class ItemAdapter extends ArrayAdapter<ListItem> {
	public ItemAdapter(Context context, List<ListItem> items)
	{
		super(context, android.R.layout.simple_list_item_1, android.R.id.text1, items);

		this.context = context;
		this.items = items;
	}

	public ItemAdapter(Context context, ListItem[] items)
	{
		super(context, android.R.layout.simple_list_item_1, android.R.id.text1, items);

		this.context = context;
		this.items = Arrays.asList(items);
	}

	private Context context;
	List<ListItem> items;

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {

		ListItem item = this.items.get(i);

		if (item.isSectionHeader()) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View newView = inflater.inflate(R.layout.list_section_header, viewGroup, false);

			TextView text1 = (TextView)newView.findViewById(android.R.id.text1);
			String label = "Unnamed";

			if (item instanceof RealizedListItem)
				label = ((RealizedListItem)item).getRealizedLabel();
			else if (text1 != null) {
				label = context.getResources().getText(item.getLabel()).toString();
			}

			text1.setText(label.toUpperCase());

			return newView;
		}

		return super.getView(i, view, viewGroup);
	}
}
