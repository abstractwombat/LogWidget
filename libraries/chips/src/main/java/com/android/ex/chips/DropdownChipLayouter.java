package com.android.ex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.Queries.Query;

/**
 * A class that inflates and binds the views in the dropdown list from
 * RecipientEditTextView.
 */
public class DropdownChipLayouter {
    /**
     * The type of adapter that is requesting a chip layout.
     */
    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    public interface ChipDeleteListener {
        void onChipDelete();
    }

    private final LayoutInflater mInflater;
    private final Context mContext;
    private ChipDeleteListener mDeleteListener;
    private Query mQuery;

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        mInflater = inflater;
        mContext = context;
    }

    public void setQuery(Query query) {
        mQuery = query;
    }

    public void setDeleteListener(ChipDeleteListener listener) {
        mDeleteListener = listener;
    }


    /**
     * Layouts and binds recipient information to the view. If convertView is null, inflates a new
     * view with getItemLaytout().
     *
     * @param convertView The view to bind information to.
     * @param parent The parent to bind the view to if we inflate a new view.
     * @param entry The recipient entry to get information from.
     * @param position The position in the list.
     * @param type The adapter type that is requesting the bind.
     * @param constraint The constraint typed in the auto complete view.
     *
     * @return A view ready to be shown in the drop down list.
     */
    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position,
        AdapterType type, String constraint) {
        return bindView(convertView, parent, entry, position, type, constraint, null);
    }

    /**
     * See {@link #bindView(View, ViewGroup, RecipientEntry, int, AdapterType, String)}
     * @param deleteDrawable
     */
    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position,
            AdapterType type, String constraint, StateListDrawable deleteDrawable) {
        // Default to show all the information
        String displayName = entry.getDisplayName();
        String destination = entry.getDestination();
        boolean showImage = true;
        CharSequence destinationType = getDestinationType(entry);

        final View itemView = reuseOrInflateView(convertView, parent, type);

        final ViewHolder viewHolder = new ViewHolder(itemView);

        // Hide some information depending on the entry type and adapter type
        switch (type) {
            case BASE_RECIPIENT:
                if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
                    displayName = destination;

                    // We only show the destination for secondary entries, so clear it only for the
                    // first level.
                    if (entry.isFirstLevel()) {
                        destination = null;
                    }
                }

                if (!entry.isFirstLevel()) {
                    displayName = null;
                    showImage = false;
                }

                // For BASE_RECIPIENT set all top dividers except for the first one to be GONE.
                if (viewHolder.topDivider != null) {
                    viewHolder.topDivider.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                }
                break;
            case RECIPIENT_ALTERNATES:
                if (position != 0) {
                    displayName = null;
                    showImage = false;
                }
                break;
            case SINGLE_RECIPIENT:
                destination = Rfc822Tokenizer.tokenize(entry.getDestination())[0].getAddress();
                destinationType = null;
        }

        // Bind the information to the view
        bindTextToView(displayName, viewHolder.displayNameView);
        bindTextToView(destination, viewHolder.destinationView);
        bindTextToView(destinationType, viewHolder.destinationTypeView);
        bindIconToView(showImage, entry, viewHolder.imageView, type);
        bindDrawableToDeleteView(deleteDrawable, viewHolder.deleteView);

        return itemView;
    }

    /**
     * Returns a new view with {@link #getItemLayoutResId(AdapterType)}.
     */
    public View newView(AdapterType type) {
        return mInflater.inflate(getItemLayoutResId(type), null);
    }

    /**
     * Returns the same view, or inflates a new one if the given view was null.
     */
    protected View reuseOrInflateView(View convertView, ViewGroup parent, AdapterType type) {
        int itemLayout = getItemLayoutResId(type);
        switch (type) {
            case BASE_RECIPIENT:
            case RECIPIENT_ALTERNATES:
                break;
            case SINGLE_RECIPIENT:
                itemLayout = getAlternateItemLayoutResId(type);
                break;
        }
        return convertView != null ? convertView : mInflater.inflate(itemLayout, parent, false);
    }

    /**
     * Binds the text to the given text view. If the text was null, hides the text view.
     */
    protected void bindTextToView(CharSequence text, TextView view) {
        if (view == null) {
            return;
        }

        if (text != null) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Binds the avatar icon to the image view. If we don't want to show the image, hides the
     * image view.
     */
    protected void bindIconToView(boolean showImage, RecipientEntry entry, ImageView view,
        AdapterType type) {
        if (view == null) {
            return;
        }

        if (showImage) {
            switch (type) {
                case BASE_RECIPIENT:
                    byte[] photoBytes = entry.getPhotoBytes();
                    if (photoBytes != null && photoBytes.length > 0) {
                        final Bitmap photo = BitmapFactory.decodeByteArray(photoBytes, 0,
                            photoBytes.length);
                        view.setImageBitmap(photo);
                    } else {
                        view.setImageResource(getDefaultPhotoResId());
                    }
                    break;
                case RECIPIENT_ALTERNATES:
                    Uri thumbnailUri = entry.getPhotoThumbnailUri();
                    if (thumbnailUri != null) {
                        // TODO: see if this needs to be done outside the main thread
                        // as it may be too slow to get immediately.
                        view.setImageURI(thumbnailUri);
                    } else {
                        view.setImageResource(getDefaultPhotoResId());
                    }
                    break;
                case SINGLE_RECIPIENT:
                default:
                    break;
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    protected void bindDrawableToDeleteView(final StateListDrawable drawable, ImageView view) {
        if (view == null) {
            return;
        }
        if (drawable == null) {
            view.setVisibility(View.GONE);
        }

        view.setImageDrawable(drawable);
        if (mDeleteListener != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawable.getCurrent() != null) {
                        mDeleteListener.onChipDelete();
                    }
                }
            });
        }
    }

    protected CharSequence getDestinationType(RecipientEntry entry) {
        return mQuery.getTypeLabel(mContext.getResources(), entry.getDestinationType(),
            entry.getDestinationLabel()).toString().toUpperCase();
    }

    /**
     * Returns a layout id for each item inside auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected int getItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    /**
     * Returns a layout id for each item inside alternate auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected int getAlternateItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    /**
     * Returns a resource ID representing an image which should be shown when ther's no relevant
     * photo is available.
     */
    protected int getDefaultPhotoResId() {
        return R.drawable.ic_contact_picture;
    }

    /**
     * Returns an id for TextView in an item View for showing a display name. By default
     * {@link android.R.id#title} is returned.
     */
    protected int getDisplayNameResId() {
        return android.R.id.title;
    }

    /**
     * Returns an id for TextView in an item View for showing a destination
     * (an email address or a phone number).
     * By default {@link android.R.id#text1} is returned.
     */
    protected int getDestinationResId() {
        return android.R.id.text1;
    }

    /**
     * Returns an id for TextView in an item View for showing the type of the destination.
     * By default {@link android.R.id#text2} is returned.
     */
    protected int getDestinationTypeResId() {
        return android.R.id.text2;
    }

    /**
     * Returns an id for ImageView in an item View for showing photo image for a person. In default
     * {@link android.R.id#icon} is returned.
     */
    protected int getPhotoResId() {
        return android.R.id.icon;
    }

    /**
     * Returns an id for ImageView in an item View for showing the delete button. In default
     * {@link android.R.id#icon1} is returned.
     */
    protected int getDeleteResId() { return android.R.id.icon1; }

    /**
     * A holder class the view. Uses the getters in DropdownChipLayouter to find the id of the
     * corresponding views.
     */
    protected class ViewHolder {
        public final TextView displayNameView;
        public final TextView destinationView;
        public final TextView destinationTypeView;
        public final ImageView imageView;
        public final ImageView deleteView;
        public final View topDivider;

        public ViewHolder(View view) {
            displayNameView = (TextView) view.findViewById(getDisplayNameResId());
            destinationView = (TextView) view.findViewById(getDestinationResId());
            destinationTypeView = (TextView) view.findViewById(getDestinationTypeResId());
            imageView = (ImageView) view.findViewById(getPhotoResId());
            deleteView = (ImageView) view.findViewById(getDeleteResId());
            topDivider = view.findViewById(R.id.chip_autocomplete_top_divider);
        }
    }
}
