package com.abstractwombat.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.view.DragEvent;
import android.util.Log;
import android.widget.AdapterView;

class ListViewDragListener implements OnDragListener {
	public ReorderableListView.Reorderable reorderable;
	private int insertAboveRes;
	private int insertBelowRes;

	public ListViewDragListener(ReorderableListView.Reorderable reorderable){
		this.reorderable = reorderable;
	}
	public void setInsertMarkers(int insertAbove, int insertBelow){
		insertAboveRes = insertAbove;
		insertBelowRes = insertBelow;
	}

	@Override
	public boolean onDrag(View view, DragEvent event) {
		ListView listView = (ListView)view;
		switch (event.getAction()) {
			case DragEvent.ACTION_DRAG_STARTED:{
				View mover = (View) event.getLocalState();
				mover.setTag(R.id.row_opacity, mover.getAlpha());
				mover.setAlpha((float) 0.2);
				break;
			}
			case DragEvent.ACTION_DRAG_LOCATION:{
				int x = (int)event.getX();
				int y = (int)event.getY();
				Log.d("ListViewDragListener", "X, Y = " + x + ", " + y);
				
				int firstVisiblePos = listView.getFirstVisiblePosition();
				
				int toIndex = listView.pointToPosition(x, y);	// Returns actual pos, not relative to visible
				if (toIndex == AdapterView.INVALID_POSITION) return true;
				Log.d("ListViewDragListener", "toIndex uncorrected: " + toIndex);
				
				
				View mover = (View) event.getLocalState();
				if (mover == null) return false;
				int fromIndex = (Integer)mover.getTag(R.id.row_index);
				
				View listItem = listView.getChildAt(toIndex-firstVisiblePos);	// Must specify pos relative to visible
				int scrollSlowProximity = 60;
				int scrollSlowDistance = 30;
				int scrollSlowTime = 30;
				int scrollFastProximity = 30;
				int scrollFastDistance = 40;
				int scrollFastTime = 10;
				
				if (listItem.getTop() < scrollSlowProximity){
					listView.smoothScrollBy(-1*scrollSlowDistance, scrollSlowTime);
				}else if (listItem.getTop() < scrollFastProximity){
					listView.smoothScrollBy(-1*scrollFastDistance, scrollFastTime);
				}else if (listView.getBottom() - listItem.getBottom() < scrollSlowProximity){
					listView.smoothScrollBy(scrollSlowDistance, scrollSlowTime);
				}else if (listView.getBottom() - listItem.getBottom() < scrollFastProximity){
					listView.smoothScrollBy(scrollFastDistance, scrollFastTime);
				}
			
				View previousMarker = listView.findViewWithTag(R.id.row_marker);
				if (previousMarker != null){
					previousMarker.setVisibility(View.INVISIBLE);
					previousMarker.setTag(0);
				}
				
				if (toIndex == fromIndex){
					return true;
				}

				int markerRes = 0;
				if (toIndex > fromIndex){
					markerRes = this.insertBelowRes;
				}else{
					markerRes = this.insertAboveRes;
				}

				View marker = listItem.findViewById(markerRes);
				marker.setVisibility(View.VISIBLE);
				marker.setTag(R.id.row_marker);
				
				break;
			}
			case DragEvent.ACTION_DRAG_EXITED:{
				break;
			}
			case DragEvent.ACTION_DRAG_ENTERED:{
				break;
			}
			case DragEvent.ACTION_DROP:{
				int x = (int)event.getX();
				int y = (int)event.getY();
				Log.d("ListViewDragListener", "Dropped  X, Y = " + x + ", " + y);
				
				int toIndex = listView.pointToPosition(x, y);
				if (toIndex == AdapterView.INVALID_POSITION) return true;
				Log.d("ListViewDragListener", "Dropped  toIndex uncorrected: " + toIndex);
				
				View mover = (View) event.getLocalState();
				if (mover == null) return false;
				
				mover.setVisibility(View.VISIBLE);
				float opacity = (Float)mover.getTag(R.id.row_opacity);
				mover.setAlpha(opacity);
				View previousMarker = listView.findViewWithTag(R.id.row_marker);
				if (previousMarker != null){
					previousMarker.setVisibility(View.GONE);
					previousMarker.setTag((Object)(false));
				}
				
				int fromIndex = (Integer)mover.getTag(R.id.row_index);
				mover.setVisibility(View.VISIBLE);
				
				if (toIndex != fromIndex){
					Log.d("ListViewDragListener", "Reordering " + fromIndex + " -> " + toIndex);
					reorderable.reorder(fromIndex, toIndex);
				}
				break;
			}
			case DragEvent.ACTION_DRAG_ENDED:{
				View mover = (View) event.getLocalState();
				if (mover == null) return true;

				mover.setVisibility(View.VISIBLE);
				float opacity = (Float)mover.getTag(R.id.row_opacity);
				mover.setAlpha(opacity);
				View previousMarker = listView.findViewWithTag(R.id.row_marker);
				if (previousMarker != null){
					previousMarker.setVisibility(View.GONE);
					previousMarker.setTag((Object)(false));
				}
				break;
			}
			default:
				break;
		}
		return true;
	}
}

public class ReorderableListView extends ListView {

	public interface Reorderable {
		public void reorder(int fromIndex, int toIndex);
	}

	private ListViewDragListener dragListener;
	private int insertAboveRes;
	private int insertBelowRes;
	
	public ReorderableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setAdapter(ListAdapter adapter){
		if (adapter instanceof Reorderable){
			this.setReorderable((Reorderable)adapter);
		}
		super.setAdapter(adapter);
	}
	
	public void setInsertMarkers(int insertAbove, int insertBelow){
		insertAboveRes = insertAbove;
		insertBelowRes = insertBelow;
		if (this.dragListener != null){
			this.dragListener.setInsertMarkers(this.insertAboveRes, this.insertBelowRes);
		}
	}
			
	public void setReorderable(Reorderable reorderable){
		this.dragListener = new ListViewDragListener(reorderable);
		this.dragListener.setInsertMarkers(this.insertAboveRes, this.insertBelowRes);
		this.setOnDragListener(this.dragListener);
	}
	
}
