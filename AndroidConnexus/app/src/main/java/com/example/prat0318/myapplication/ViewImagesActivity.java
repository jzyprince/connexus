package com.example.prat0318.myapplication;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewImagesActivity extends Activity {

    private ImageAdapter imageAdapter;

    private ArrayList<String> PhotoURLS = new ArrayList<String>();

    public static int offset = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.allimages);

        Bundle extras = getIntent().getExtras();
        final String stream_id = extras.getString("stream_id");

        TextView stream_name = (TextView) findViewById(R.id.stream_name);
        stream_name.setText("Stream name: " + stream_id);

        imageAdapter = new ImageAdapter(this);
        final ImageView imgView = (ImageView) findViewById(R.id.GalleryView);
        GridView g = (GridView) findViewById(R.id.gridview);

        final Context context = this;
        Button backButton = (Button) findViewById(R.id.streams);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        Button uploadButton = (Button) findViewById(R.id.upload);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(getApplicationContext(), UploadActivity.class);
                i.putExtra("stream_id", stream_id);
                startActivity(i);
            }
        });
        Button moreButton = (Button) findViewById(R.id.more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ViewImagesActivity.offset += 6;
                recreate();
            }
        });
//        Gallery g = (Gallery) findViewById(R.id.Gallery);
        g.setAdapter(imageAdapter);
//        g.setOnItemClickListener(new OnItemClickListener() {
//            public void onItemClick(AdapterView<?> parent, View v,
//                                    int position, long id) {
//                imgView.setImageDrawable(LoadImageFromURL(PhotoURLS
//                        .get(position)));
//                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//            }
//        });

        DefaultHttpClient   httpclient = new DefaultHttpClient(new BasicHttpParams());
        String stream_id_enc = "";
        try {
            stream_id_enc = URLEncoder.encode(stream_id, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpGet httpget = new HttpGet("http://conneksus.appspot.com/view?raw=true&offset=" + offset + "&stream_name=" + stream_id_enc);
        httpget.setHeader("Content-type", "application/json");

        InputStream inputStream = null;
        String result = null;
        try {
            HttpResponse response = httpclient.execute(httpget);
            result = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jArray = jsonObject.getJSONArray("image_ids");
            for (int i=0; i < jArray.length(); i++)
            {
                PhotoURLS.add("http://conneksus.appspot.com/image?image_id=" + jArray.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
        }


        new AddImageTask().execute();

    }

    class AddImageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... unused) {
            for (String url : PhotoURLS) {
                imageAdapter.addItem(LoadThumbnailFromURL(url));
                publishProgress();
            }

            return (null);
        }

        @Override
        protected void onProgressUpdate(Void... unused) {
            imageAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void unused) {
        }
    }

    private Drawable LoadThumbnailFromURL(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            String contentType = connection.getHeaderField("Content-Type");
            boolean isImage = contentType.startsWith("image/");
            if(isImage){
                HttpGet httpRequest = new HttpGet(url);
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = (HttpResponse) httpclient
                        .execute(httpRequest);
                HttpEntity entity = response.getEntity();
                BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(entity);

                InputStream is = bufferedHttpEntity.getContent();
                Drawable d = Drawable.createFromStream(is, "src Name");
                return d;
            } else {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.no_image);
                Drawable d = new BitmapDrawable(b);
                return d;
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_LONG)
                    .show();
            Log.e(e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    private Drawable LoadImageFromURL(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            String contentType = connection.getHeaderField("Content-Type");
            boolean isImage = contentType.startsWith("image/");
            if(isImage){
                HttpGet httpRequest = new HttpGet(url);
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = (HttpResponse) httpclient
                        .execute(httpRequest);
                HttpEntity entity = response.getEntity();
                BufferedHttpEntity bufferedHttpEntity = new BufferedHttpEntity(
                        entity);
                InputStream is = bufferedHttpEntity.getContent();

                // Decode image size
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, o);

                // The new size we want to scale to
                final int REQUIRED_SIZE = 150;

                // Find the correct scale value. It should be the power of 2.
                int width_tmp = o.outWidth, height_tmp = o.outHeight;
                int scale = 1;
                while (true) {
                    if (width_tmp / 2 < REQUIRED_SIZE
                            || height_tmp / 2 < REQUIRED_SIZE)
                        break;
                    width_tmp /= 2;
                    height_tmp /= 2;
                    scale *= 2;
                }

                // Decode with inSampleSize
                is = bufferedHttpEntity.getContent();
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = scale;
                Bitmap b = BitmapFactory.decodeStream(is, null, o2);
                Drawable d = new BitmapDrawable(b);
                return d;
            } else {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.no_image);
                Drawable d = new BitmapDrawable(b);
                return d;
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_LONG)
                    .show();
            Log.e(e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;
        private Context mContext;

        ArrayList<Drawable> drawablesFromUrl = new ArrayList<Drawable>();

        public ImageAdapter(Context c) {
            mContext = c;
            TypedArray a = obtainStyledAttributes(R.styleable.GalleryTheme);
            mGalleryItemBackground = a.getResourceId(
                    R.styleable.GalleryTheme_android_galleryItemBackground, 0);
            a.recycle();
        }

        public void addItem(Drawable item) {
            drawablesFromUrl.add(item);
        }

        public int getCount() {
            return drawablesFromUrl.size();
        }

        public Drawable getItem(int position) {
            return drawablesFromUrl.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);

            i.setImageDrawable(drawablesFromUrl.get(position));
            i.setLayoutParams(new GridView.LayoutParams(90, 90));
            i.setScaleType(ImageView.ScaleType.FIT_CENTER);
            i.setBackgroundResource(mGalleryItemBackground);

            return i;
        }
    }

}