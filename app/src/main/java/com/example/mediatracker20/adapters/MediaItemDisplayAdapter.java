package com.example.mediatracker20.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediatracker20.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import model.model.MediaItem;

//Adapter for MediaItems Recyclerview used only to display and click into. No selection or delete...
public class MediaItemDisplayAdapter extends RecyclerView.Adapter<MediaItemDisplayAdapter.MediaItemViewHolder> {

    private static List<MediaItem> allItems; //all items to be displayed
    private boolean pager = false; //if it is for viewpager or not (view pager needs to wrap)
    private int actionId; //nav id

    public class MediaItemViewHolder extends RecyclerView.ViewHolder {

        public TextView itemTitle;
        public TextView itemRating;
        public ImageView itemImage;
        public TextView itemEpisodes;
        public RelativeLayout layout;


        public MediaItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemTitle = itemView.findViewById(R.id.media_card_dis_title);
            itemRating = itemView.findViewById(R.id.media_card_dis_rating);
            itemImage = itemView.findViewById(R.id.media_card_dis_image);
            itemEpisodes = itemView.findViewById(R.id.media_card_dis_episodes);
            layout = itemView.findViewById(R.id.media_card_dis_layout);
        }
    }

    @NonNull
    @Override
    public MediaItemDisplayAdapter.MediaItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item_display_card, parent, false);
        if(pager == true) {
            v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); //for view pager specifications
        }
        MediaItemDisplayAdapter.MediaItemViewHolder evh = new MediaItemDisplayAdapter.MediaItemViewHolder(v);
        return evh; //view holder;
    }

    //set card info
    @Override
    public void onBindViewHolder(@NonNull MediaItemDisplayAdapter.MediaItemViewHolder holder, int position) {
        MediaItem item = allItems.get(position);
        holder.itemRating.setText("Rating: " + item.getItemInfo("Rating"));
        holder.itemTitle.setText(item.getItemInfo("Title"));
        holder.itemEpisodes.setText("Episodes: " + item.getItemInfo("Episodes"));
        Picasso.get().load(item.getItemInfo("ImageLink")).into(holder.itemImage);
        holder.layout.setOnClickListener(new View.OnClickListener() { //to item summary
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("MEDIA_ITEM", item);
                Navigation.findNavController(holder.itemView).navigate(actionId, bundle);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allItems.size();
    }

    public MediaItemDisplayAdapter(List<MediaItem> itemsList, int navActionID) {
        allItems = itemsList;
        actionId = navActionID;
    }

    public void addItemsToList(List<MediaItem> list) {
            allItems.addAll(list);
    }

    public void setIsPager(boolean t) {
        pager = t;
    }

}
