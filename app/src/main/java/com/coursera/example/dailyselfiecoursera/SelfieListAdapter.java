package com.coursera.example.dailyselfiecoursera;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class SelfieListAdapter extends ArrayAdapter{
    private final Activity activity;
    private final List selfies;
    private static String TAG = "TagSelfieListAdapter";
    private final String mSaveFilename = "SelfieList.json";

    public SelfieListAdapter(Activity activity, List objects) {
        super(activity, R.layout.list_item_selfies, objects);
        this.activity = activity;
        this.selfies = objects;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_item_selfies, null, true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.textView_name);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
        ImageView imageView_delete = (ImageView) rowView.findViewById(R.id.imageView_delete);

        final Selfie currentSelfie = (Selfie) selfies.get(position);

        txtTitle.setText(currentSelfie.getName());
        imageView.setImageURI(currentSelfie.getThumbnailUri());

        imageView_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOneRecord(currentSelfie.getFullimageUri(), currentSelfie.getThumbnailUri(),
                        position);
                notifyDataSetChanged();
            }
        });
        return rowView;
    }

    public void deleteOneRecord(Uri imageUri, Uri thumbnailUri, int position){
        File fileImage = new File(String.valueOf(imageUri.getPath()));
        File fileThumbnail = new File(String.valueOf(thumbnailUri.getPath()));
        if (fileImage.exists())
            fileImage.delete();
        if (fileThumbnail.exists())
            fileThumbnail.delete();

        deleteFromJson(position);
        selfies.remove(selfies.get(position));

        Log.v(TAG, "in deleteOneRecord()");
    }

    public void deleteFromJson(int position) {
        File f = new File(getContext().getExternalFilesDir(null), mSaveFilename);
        StringBuilder text = new StringBuilder();
        BufferedReader br = null;
        JSONArray jsonArrayWithoutOne = new JSONArray();

        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            Log.v(TAG, "Exception in deleteFromJson()");
        } finally {
            try { br.close(); } catch (Exception e) { }
        }

        try {
            JSONArray list = new JSONArray(text.toString());
            Log.v(TAG,"list = "+ list);
            jsonArrayWithoutOne  = jsonRemoveItem(list, position);
        } catch (JSONException e) {
            Log.i(TAG, "JSON Exception in deleteFromJson()" + e.getMessage());
        }
        try {
            FileWriter out = new FileWriter(f);
            out.write(jsonArrayWithoutOne.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.i(TAG, "Error writing file " + e.getMessage());
        }
    }
    private JSONArray jsonRemoveItem(JSONArray jsonArray,int position){
        JSONArray output = new JSONArray();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++)
            {
                //Excluding the item at position
                if (i != position)
                {
                    Log.v(TAG,"i = "+position);
                    try {
                        output.put(jsonArray.get(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return output;
    }
    public void add(Selfie listItem)
    {
        selfies.add(listItem);
        notifyDataSetChanged();
    }

    public Selfie getItem(int position)
    {
        return (Selfie)selfies.get(position);
    }

}