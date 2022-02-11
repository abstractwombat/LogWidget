package com.abstractwombat.library;

import android.content.ClipData;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListView;
import android.graphics.Point;
import android.util.Log;

import com.abstractwombat.library.R;

class ShadowBuilder extends View.DragShadowBuilder {
	public ShadowBuilder(View view){
		super(view);
	}
	public int x = -1;
	public int y = -1;

	@Override
	public void onProvideShadowMetrics (Point shadowSize, Point shadowTouchPoint){
		super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
		if (x != -1) shadowTouchPoint.x = x;
		if (y != -1) shadowTouchPoint.y = y;
	}
}

public class ReorderableListViewHandleTouchListener implements OnTouchListener {
	public ReorderableListViewHandleTouchListener(){
	}
	public boolean onTouch(View view, MotionEvent motionEvent) {
		Log.d("ReorderableListViewHandleTouchListener", "onTouch");
		if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			Log.d("ReorderableListViewHandleTouchListener", "onTouch ACTION_DOWN");
			ClipData data = ClipData.newPlainText("", "");
			View listViewItem = (View)view.getParent();
			View listViewItemParent = (View)listViewItem.getParent();
			while (!(listViewItemParent instanceof ListView)){
				listViewItem = (View)listViewItem.getParent();
				listViewItemParent = (View)listViewItem.getParent();
			}
			ListView listView = (ListView)(listViewItemParent);
			ShadowBuilder shadowBuilder = new ShadowBuilder(listViewItem);
			shadowBuilder.x = (int)motionEvent.getX();
			listViewItem.setTag(R.id.row_index, listView.indexOfChild(listViewItem)+listView.getFirstVisiblePosition());
			view.startDrag(data, shadowBuilder, listViewItem, 0);
			return true;
		} else {
			return false;
		}
	}
}
