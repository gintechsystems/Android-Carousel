package com.appl.carouselwidget;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.appl.library.CoverFlowCarousel;


public class MainActivity extends AppCompatActivity {

    CoverFlowCarousel carousel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CarouselAdapter adapter = new CarouselAdapter();

        carousel = findViewById(R.id.carousel);
        carousel.setAdapter(adapter);
        carousel.setSelection(adapter.getCount() / 2);
        carousel.setSlowDownCoefficient(1);
        carousel.setSpacing(0.5f);
        carousel.setRotationThreshold(0.3f);
        carousel.shouldRepeat(true); //When not using repeat, I suggest replacing getCount() below = mCount along with getItem = mResourceIds[position % mResourceIds.length].

        //Pointless to add a view when we are repeating.
        Button addButton = findViewById(R.id.add_botton);
        if (!carousel.isRepeating()) {
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.addView();
                }
            });
        }
        else {
            addButton.setVisibility(View.GONE);
        }
    }


    private class CarouselAdapter extends BaseAdapter {
        private int[] mResourceIds = {R.drawable.poster1, R.drawable.poster2, R.drawable.poster3, R.drawable.poster4 };

        //private int mCount = mResourceIds.length * 4;

        @Override
        public int getCount() {
            return mResourceIds.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            MyFrame v;
            if (convertView == null) {
                v = new MyFrame(MainActivity.this);
            } else {
                v = (MyFrame)convertView;
            }

            v.setImageResource(mResourceIds[position]);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    carousel.scrollToItemPosition(position);
                    Toast.makeText(MainActivity.this, "clicked position:" + position, Toast.LENGTH_SHORT).show();
                }
            });

            return v;
        }

        public void addView(){
            //mCount++;
            notifyDataSetChanged();
        }
    }

    public static class MyFrame extends FrameLayout {
        private ImageView mImageView;

        public void setImageResource(int resId){
            mImageView.setImageResource(resId);
        }

        public MyFrame(Context context) {
            super(context);

            mImageView = new ImageView(context);
            mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(mImageView);

            setBackgroundColor(Color.WHITE);
            setSelected(false);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);

            if(selected) {
                mImageView.setAlpha(1.0f);
            } else {
                mImageView.setAlpha(0.5f);
            }
        }
    }
}
