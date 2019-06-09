package com.example.music1;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class MediaCursorAdapter extends CursorAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public MediaCursorAdapter(Context context) {
        super(context , null , 0);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public View newView(Context context , Cursor cursor , ViewGroup viewGroup) {
        View itemView = mLayoutInflater.inflate(R.layout.list_item , viewGroup, false );
        if (itemView != null) {
            ViewHolder vh = new ViewHolder();
            vh.Title = itemView.findViewById(R.id.title );
            vh.Artist = itemView.findViewById(R.id.artist );
            vh.Sequence = itemView.findViewById(R.id.music_sequence);
            vh.Divider = itemView.findViewById(R.id.divider );
            itemView.setTag(vh);
            return itemView;
        }
        return null;
    }
    public class ViewHolder {
        TextView Title ;
        TextView Artist ;
        TextView Sequence;
        View Divider ;
    }

    @Override
    public void bindView(View view,  Context context , Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();
        int titleIndex = cursor.getColumnIndex( MediaStore.Audio.Media.TITLE);
        int artistIndex = cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST);
        String title = cursor. getString(titleIndex );
        String artist = cursor. getString(artistIndex );
        int position = cursor. getPosition ();
        if (vh != null) {
            vh.Title .setText( title );
            vh.Artist .setText(artist );
            vh.Sequence.setText(Integer.toString(position+1));
        }
    }
}
