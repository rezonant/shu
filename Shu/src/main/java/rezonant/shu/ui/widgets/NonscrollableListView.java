package rezonant.shu.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * A ListView that does not react to scrolling. Should be used inside a ScrollView
 * Created by liam on 7/4/14.
 */
public class NonscrollableListView extends ListView {
	public NonscrollableListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public NonscrollableListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public NonscrollableListView(Context context)
	{
		super(context);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
				MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev){
		if(ev.getAction()==MotionEvent.ACTION_MOVE)
			return true;
		return super.dispatchTouchEvent(ev);
	}
}
