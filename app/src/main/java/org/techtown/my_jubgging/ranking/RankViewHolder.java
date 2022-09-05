package org.techtown.my_jubgging.ranking;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.techtown.my_jubgging.R;

public class RankViewHolder extends RecyclerView.ViewHolder {
    TextView rankNum;

    ImageView profileImg;
    TextView nickName;
    TextView walkingCnt;

    RankViewHolder(View itemView) {
        super(itemView);

        rankNum = itemView.findViewById(R.id.ranking_adapter_rank_num);
        profileImg = itemView.findViewById(R.id.ranking_adapter_profile_image);
        nickName = itemView.findViewById(R.id.ranking_adapter_nickname_text);
        walkingCnt = itemView.findViewById(R.id.ranking_adapter_walking_num);
    }
}
