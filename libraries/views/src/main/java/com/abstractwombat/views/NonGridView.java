package com.abstractwombat.views;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Mike on 11/21/2014.
 */
public class NonGridView extends ViewGroup {
    private static final String TAG = "NonGridView";

    private boolean autoColumnCounts = false;
    private Integer mColumnCountPortrait = -1;
    private Integer mColumnCountLandscape = -1;
    private Integer mMinColumnWidth = 200;

    private Integer mItemPaddingVertical = 50;
    private Integer mItemPaddingHorizontal = 50;
    private Integer mDragHandle;
    private Integer mLongPressDragHandle;

    private static  final Integer PADDING_HACK_WIDTH = 10;
    private static  final Integer PADDING_HACK_TOP = 8;
    private static  final Integer PADDING_HACK_BOTTOM = 8;
    private static final Integer COLUMN_COUNT_DEFAULT = 2;

    private final LayoutTransition mLayoutTransition = new LayoutTransition();
    private boolean mDragDropOngoing = false;
    private int mDragDropOriginalPosition;
    private int mDragDropNewPosition;
    private int mDragDropStartX, mDragDropStartY;
    private final HashMap<View, OnLayoutChangeListener> mDragDropLayoutChangeListenerMap =
            new HashMap<View, View.OnLayoutChangeListener>();
    private static long mDragDropAnimatorDuration = 300;
    private static ObjectAnimator mDragDropAnimator = ObjectAnimator.ofPropertyValuesHolder((Object) null,
            PropertyValuesHolder.ofInt("left", 0, 1), PropertyValuesHolder.ofInt("top", 0, 1),
            PropertyValuesHolder.ofInt("right", 0, 1), PropertyValuesHolder.ofInt("bottom", 0,1),
            PropertyValuesHolder.ofInt("scrollX", 0, 1), PropertyValuesHolder.ofInt("scrollY", 0, 1));

    private class Space2D {
        private class Space {
            int mStart, mEnd;
            public Space(int start, int end) {
                this.mStart = start;
                this.mEnd = end;
            }
            public boolean inSpace(int p){
                return (p >= mStart && p <= mEnd);
            }
        }
        Space mX, mY;
        public Space2D(int startX, int endX, int startY, int endY){
            this.mX = new Space(startX, endX);
            this.mY = new Space(startY, endY);
        }
        public boolean inSpace(int x, int y){
            return (mX.inSpace(x) && mY.inSpace(y));
        }
    }
    private List<Space2D> mChildPositions;

    private Context mContext;

    public NonGridView(Context context) {
        super(context);
        mContext = context;
    }

    public NonGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        loadAttributeValues(attrs);
    }

    public NonGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        loadAttributeValues(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NonGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        loadAttributeValues(attrs);
    }

    private void loadAttributeValues(AttributeSet attrs) {
        Resources.Theme theme = mContext.getTheme();
        if (theme == null) return;
        TypedArray a = theme.obtainStyledAttributes(
                attrs,
                R.styleable.NonGridView,
                0, 0);
        if (a == null) return;

        try {
            // Get the column count
            int columnCount = a.getInt(R.styleable.NonGridView_columnCount, -1);
            if (columnCount != -1){
                mColumnCountPortrait = columnCount;
                mColumnCountLandscape = columnCount;
            }
            // Individual portrait and/or landscape counts will override above
            mColumnCountPortrait = a.getInt(R.styleable.NonGridView_columnCountPortrait,
                    mColumnCountPortrait);
            mColumnCountLandscape = a.getInt(R.styleable.NonGridView_columnCountLandscape,
                    mColumnCountLandscape);

            // Minimum column width is only used if columns counts aren't found
            mMinColumnWidth = a.getDimensionPixelSize(R.styleable.NonGridView_minColumnWidth, -1);
            if (mColumnCountPortrait == -1 || mColumnCountLandscape == -1){
                autoColumnCounts = true;
            }else{
                autoColumnCounts = false;
                if (mColumnCountPortrait == -1) mColumnCountPortrait = COLUMN_COUNT_DEFAULT;
                if (mColumnCountLandscape == -1) mColumnCountLandscape = COLUMN_COUNT_DEFAULT;
            }

            // Get the item padding
            int itemPadding = a.getDimensionPixelSize(R.styleable.NonGridView_gridPadding, -1);
            if (itemPadding != -1){
                mItemPaddingHorizontal = itemPadding;
                mItemPaddingVertical = itemPadding;
            }

            // Vertical and horizontal padding will override above
            int itemPaddingV = a.getDimensionPixelSize(R.styleable.NonGridView_gridPaddingVertical, -1);
            if (itemPaddingV != -1){
                mItemPaddingVertical = itemPaddingV;
            }
            int itemPaddingH = a.getDimensionPixelSize(R.styleable.NonGridView_gridPaddingHorizontal, -1);
            if (itemPaddingH != -1){
                mItemPaddingHorizontal = itemPaddingH;
            }

            // Get the drag handle
            mDragHandle = a.getResourceId(R.styleable.NonGridView_itemDragHandle, 0);

            // Get the long press drag handle
            mLongPressDragHandle = a.getResourceId(R.styleable.NonGridView_itemLongPressDragHandle, 0);
        } finally {
            a.recycle();
        }
    }

    public void setDragHandle(int dragHandle){
        if (mDragHandle != 0) {
            // Remove the old listeners
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                View handle = child.findViewById(mDragHandle);
                if (handle != null) {
                    handle.setOnTouchListener(null);
                }
            }
        }
        mDragHandle = dragHandle;
        invalidate();
    }
    public void setLongPressDragHandle(int longPressDragHandle){
        if (mLongPressDragHandle != 0) {
            // Remove the old listeners
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                View longHandle = child.findViewById(mLongPressDragHandle);
                if (longHandle != null) {
                    longHandle.setOnLongClickListener(null);
                }
            }
        }
        mLongPressDragHandle = longPressDragHandle;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure");

        int count = getChildCount();
        int columnCount = calculateColumnCount();

        // Measurement will ultimately be computing these values.
        int height = 0;
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - PADDING_HACK_WIDTH;
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "Measured max width " + maxWidth + " max height " + maxHeight);

        // Get the child padding
        int childPaddingLeft = 0, childPaddingRight = 0, childPaddingTop = 0, childPaddingBottom = 0;
        if (count > 0) {
            View childForPadding = getChildAt(0);
            childPaddingLeft = childForPadding.getPaddingLeft();
            childPaddingRight = childForPadding.getPaddingRight();
            childPaddingTop = childForPadding.getPaddingTop();
            childPaddingBottom = childForPadding.getPaddingBottom();
            Log.d(TAG, "Child padding: " + childPaddingLeft + "," + childPaddingRight + "," + childPaddingTop + "," + childPaddingBottom);
        }

        int childWidth = (maxWidth - ((columnCount - 1) * mItemPaddingHorizontal) - (columnCount * (childPaddingLeft + childPaddingRight))) / columnCount;
        int childState = 0;


        // Iterate through all children, measuring them and computing our dimensions
        // from their size.
        int[] columnHeight = new int[columnCount];
        for (int c = 0; c < columnCount; c++) columnHeight[c] = 0;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            int columnIndex = i % columnCount;
            int rowIndex = i / columnCount;
            if (child.getVisibility() != GONE) {
                // Measure the child.
                int widthSpec = View.MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(maxHeight,
                        MeasureSpec.UNSPECIFIED);
                measureChildWithMargins(child, widthSpec, 0, heightSpec, 0);

                // Keep track of each columns height
                int thisHeight = child.getMeasuredHeight() + childPaddingTop + childPaddingBottom
                        + mItemPaddingVertical;
                columnHeight[columnIndex] += thisHeight;
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                Log.d(TAG, "onMeasure Child width: " + child.getMeasuredWidth());
            }
        }
        // Remove the last item padding from each column
        String columnHeightLog = "Column heights: ";
        for (int c=0; c<columnCount; c++){
            columnHeight[c] -= mItemPaddingVertical;
            height = Math.max(height, columnHeight[c]);
            columnHeightLog += columnHeight[c] + ",";
        }
        Log.d(TAG, columnHeightLog.substring(0, columnHeightLog.length()-1));

        // Add the padding
        height += getPaddingTop();
        height += getPaddingBottom();

        // Check against our minimum height and width
        height = Math.max(height, getSuggestedMinimumHeight())+ PADDING_HACK_BOTTOM + PADDING_HACK_TOP;
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Report our final dimensions.
        Log.d(TAG, "Setting view width " + maxWidth + " height " + height);
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    class HandleOnTouchListener implements OnTouchListener{
        final View mChild;
        HandleOnTouchListener(View child){
            this.mChild = child;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN){
                startDragOperation(v, mChild);
                return true;
            }
            return false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.d(TAG, "onLayout (changed=" + changed + ")");
        if (!mDragDropOngoing){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (!mLayoutTransition.isTransitionTypeEnabled(LayoutTransition.CHANGING)) {
                    mLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
                }
            }
            setLayoutTransition(mLayoutTransition);
        }

        final int count = getChildCount();
        int columnCount = calculateColumnCount();
        if (mChildPositions == null || mChildPositions.size() != count){
            mChildPositions = new ArrayList<Space2D>(count);
        }

        // These are the edges in which we are performing layout.
        int leftEdge = getPaddingLeft();
        int rightEdge = right - left - getPaddingRight();
        final int topEdge = getPaddingTop();
        final int bottomEdge = bottom - top - getPaddingBottom();

        // Calculate the widget of each child view
        int width = rightEdge - leftEdge - PADDING_HACK_WIDTH;
        int childWidth = (width - ((columnCount-1)* mItemPaddingHorizontal)) / columnCount;
        Log.d(TAG, "onLayout Child Width: " + childWidth);

        // Position all the children
        int leftPos = leftEdge + PADDING_HACK_WIDTH/2;
        int[] columnPos = new int[columnCount];
        for (int c=0; c<columnCount; c++) columnPos[c] = topEdge;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            // Setup the drag handle
            if (mDragHandle != 0){
                View handle = child.findViewById(mDragHandle);
                if (handle != null){
                    handle.setOnTouchListener(new HandleOnTouchListener(child));
                }
            }

            // Setup the long press drag handle
            if (mLongPressDragHandle != 0){
                View longHandle = child.findViewById(mLongPressDragHandle);
                longHandle.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        startDragOperation(v, child);
                        return true;
                    }
                });

            }

            // Layout the child
            int columnIndex = i % columnCount;
            final int childHeight = child.getMeasuredHeight();
            int startX = leftPos;
            int endX = leftPos + childWidth;
            int startY = columnPos[columnIndex];
            int endY = columnPos[columnIndex] + childHeight;
            child.layout(startX, startY, endX, endY);
            mChildPositions.add(new Space2D(startX, endX, startY, endY));

            // Calculate the next child's position
            columnPos[columnIndex] += childHeight + mItemPaddingVertical;
            leftPos += childWidth + mItemPaddingHorizontal;
            if (leftPos >= rightEdge) leftPos = leftEdge + PADDING_HACK_WIDTH/2;
        }
    }


    private int screenPositionToIndex(int x, int y){
        for(Space2D space : mChildPositions){
            if (space.inSpace(x, y)){
                return mChildPositions.indexOf(space);
            }
        }
        return -1;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (mDragHandle != 0 || mLongPressDragHandle != 0){
            mDragDropStartX = (int) e.getX();
            mDragDropStartY = (int) e.getY();
        }
        return super.dispatchTouchEvent(e);
    }
    @Override
    public boolean dispatchDragEvent(DragEvent ev){
        boolean r = super.dispatchDragEvent(ev);
        if (r && (ev.getAction() == DragEvent.ACTION_DRAG_STARTED
                || ev.getAction() == DragEvent.ACTION_DRAG_ENDED)){
            // If we got a start or end and the return value is true, our
            // onDragEvent wasn't called by ViewGroup.dispatchDragEvent
            // So we do it here.
            onDragEvent(ev);
        }
        return r;
    }

    private void startDragOperation(View handle, View childToDrag){
        // Convert X and Y to be relative to the child view
        int x = mDragDropStartX - childToDrag.getLeft();
        int y = mDragDropStartY - childToDrag.getTop();
        Log.d(TAG, "X: " + mDragDropStartX + " Left:" + childToDrag.getLeft() + " Y: " + mDragDropStartY + " Right:" + childToDrag.getRight());
        // Turn off transitions, handled internally
        setLayoutTransition(null);
        // Setup the shadow
        ClipData dragData = ClipData.newPlainText("nothing", "Here it is");
        DragShadowBuilder shadow = new DragShadow(childToDrag, x, y);
        // Store the view's original position
        mDragDropOriginalPosition = indexOfChild(childToDrag);
        mDragDropNewPosition = mDragDropOriginalPosition;
        mDragDropOngoing = true;
        // Make the View invisible
        childToDrag.setVisibility(INVISIBLE);
        // Drag
        startDrag(dragData, shadow, childToDrag, 0);
    }

    @Override
    //public boolean onDrag(View v, DragEvent event){
    public boolean onDragEvent(DragEvent event){
        View dragged = (View) event.getLocalState();
        switch (event.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:
                Log.d(TAG, "Starting drag from position " + mDragDropOriginalPosition);
                return true;
            case DragEvent.ACTION_DRAG_LOCATION:
                if (!mDragDropLayoutChangeListenerMap.isEmpty()){
                    // Animation still going on
                    Log.d(TAG, "DragLocation - animation ongoing");
                    return true;
                }
                // Compute the index of the child that the drag is over
                int x = (int) event.getX();
                int y = (int) event.getY();
                int index = screenPositionToIndex(x, y);
                if (index >= 0 && index < getChildCount()) {
                    if (index == mDragDropNewPosition) {
                        //Log.d(TAG, "DragLocation - already in new pos!!");
                        return true;
                    }
                    Log.d(TAG, "Trying to insert view at " + index);
                    mDragDropNewPosition = index;
                    // Setup the transition animations
                    setupChangeTransition();
                    // Remove the view from its last position
                    this.removeView(dragged);
                    // Add the View at the computed index
                    this.addView(dragged, index);
                }else{
                    //Log.d(TAG, "DragLocation - invalid position");
                }
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                // Return the View to its original index
                Log.d(TAG, "Drag ended");
                if (dragged.getVisibility() == View.INVISIBLE){
                    removeView(dragged);
                    addView(dragged, mDragDropOriginalPosition);
                    dragged.setVisibility(VISIBLE);
                }
                mDragDropOngoing = false;
                return true;
            case DragEvent.ACTION_DROP:
                // Make the View visible
                Log.d(TAG, "Dropped view");
                dragged.setVisibility(VISIBLE);
                return true;
        }
        return true;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new NonGridView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /**
     * Custom per-child layout information.
     */
    public static class LayoutParams extends MarginLayoutParams {
        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int gravity = Gravity.TOP | Gravity.START;

        public static int POSITION_MIDDLE = 0;
        public static int POSITION_LEFT = 1;
        public static int POSITION_RIGHT = 2;

        public int position = POSITION_MIDDLE;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private int calculateColumnCount(){
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        Log.d(TAG, "calculateColumnCount - screenW="+screenWidth+" screenH="+screenHeight);

        if (autoColumnCounts){
            if (mMinColumnWidth > 0 && screenWidth > 0){
                // Calculate the number of columns
                int cCount = screenWidth / mMinColumnWidth;
                Log.d(TAG, "Computed " + cCount + " columns based on min width of " + mMinColumnWidth);
                return Math.max(1, cCount);
            }
        }else{
            if (screenWidth > screenHeight){
                return mColumnCountLandscape;
            }else{
                return mColumnCountPortrait;
            }
        }

        return COLUMN_COUNT_DEFAULT;
    }
    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    private class DragShadow extends DragShadowBuilder{
        private float mTouchX, mTouchY;

        public DragShadow(View view, float touchX, float touchY) {
            super(view);
            mTouchX = touchX;
            mTouchY = touchY;
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            super.onDrawShadow(canvas);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            View v = getView();
            shadowSize.x = v.getWidth();
            shadowSize.y = v.getHeight();

            Log.d("DraggableChildViewTouchListener", "Setting shadow touch point from " +
                    shadowTouchPoint.x + " , " + shadowTouchPoint.y + " to " +
                    mTouchX + " , " + mTouchY);
            shadowTouchPoint.x = (int) mTouchX;
            shadowTouchPoint.y = (int) mTouchY;
        }
    }

    /**
     * This function sets up animations on all of the views that change during layout.
     * For every child in the parent, we create a change animation of the appropriate
     * type (appearing, disappearing, or changing) and ask it to populate its start values from its
     * target view. We add layout listeners to all child views and listen for changes. For
     * those views that change, we populate the end values for those animations and start them.
     * Animations are not run on unchanging views.
     */
    private void setupChangeTransition() {
        final ViewGroup parent = this;
        final ViewTreeObserver observer = parent.getViewTreeObserver(); // used for later cleanup
        if (!observer.isAlive()) {
            // If the observer's not in a good state, skip the transition
            return;
        }
        int numChildren = parent.getChildCount();

        for (int i = 0; i < numChildren; ++i) {
            final View child = parent.getChildAt(i);
            setupChangeAnimation(child);
        }

        // This is the cleanup step. When we get this rendering event, we know that all of
        // the appropriate animations have been set up and run. Now we can clear out the
        // layout listeners.
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                parent.getViewTreeObserver().removeOnPreDrawListener(this);
                int count = mDragDropLayoutChangeListenerMap.size();
                if (count > 0) {
                    Collection<View> views = mDragDropLayoutChangeListenerMap.keySet();
                    for (View view : views) {
                        View.OnLayoutChangeListener listener = mDragDropLayoutChangeListenerMap.get(view);
                        view.removeOnLayoutChangeListener(listener);
                    }
                }
                Log.d(TAG, "onPreDraw - removed layoutChangeListeners");
                mDragDropLayoutChangeListenerMap.clear();
                return true;
            }
        });
    }

    /**
     * Utility function called by runChangingTransition for both the children and the parent
     * hierarchy.
     */
    private void setupChangeAnimation(final View child) {

        // If we already have a listener for this child, then we've already set up the
        // changing animation we need. Multiple calls for a child may occur when several
        // add/remove operations are run at once on a container; each one will trigger
        // changes for the existing children in the container.
        if (mDragDropLayoutChangeListenerMap.get(child) != null) {
            return;
        }

        // Don't animate items up from size(0,0); this is likely because the objects
        // were offscreen/invisible or otherwise measured to be infinitely small. We don't
        // want to see them animate into their real size; just ignore animation requests
        // on these views
        if (child.getWidth() == 0 && child.getHeight() == 0) {
            return;
        }

        // Make a copy of the appropriate animation
        final Animator anim = mDragDropAnimator.clone();
        anim.setDuration(mDragDropAnimatorDuration);
        anim.setStartDelay(0);
        anim.setInterpolator(new DecelerateInterpolator());

        // Set the target object for the animation
        anim.setTarget(child);

        // A ObjectAnimator (or AnimatorSet of them) can extract start values from
        // its target object
        anim.setupStartValues();
        Log.d(TAG, "setupChangeAnimation - Set the start values");

        // Add a listener to track layout changes on this view. If we don't get a callback,
        // then there's nothing to animate.
        final View.OnLayoutChangeListener listener = new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.d(TAG, "onLayoutChange - child view");

                // Tell the animation to extract end values from the changed object
                anim.setupEndValues();
                anim.setStartDelay(0);
                anim.setDuration(mDragDropAnimatorDuration);

                // this only removes listeners whose views changed - must clear the
                // other listeners later
                child.removeOnLayoutChangeListener(this);
                mDragDropLayoutChangeListenerMap.remove(child);
                anim.start();
            }
        };

        child.addOnLayoutChangeListener(listener);
        // cache the listener for later removal
        mDragDropLayoutChangeListenerMap.put(child, listener);
    }

}
